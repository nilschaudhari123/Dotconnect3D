package com.naampath.colorpath3d.path

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.PathColor
import kotlin.math.sin

/** Pooled 3D path segments. Transforms are updated; meshes are not recreated per frame. */
class PathRenderer : Disposable {
    private val cylinder: Model
    private val joint: Model
    private val glow: Model
    private val orb: Model
    private val segments: Array<ModelInstance>
    private val joints: Array<ModelInstance>
    private val glows: Array<ModelInstance>
    private val cursor: ModelInstance
    private var segmentCount = 0
    private var jointCount = 0
    private var glowCount = 0
    private var showCursor = false
    private val up = Vector3(0f, 1f, 0f)
    private val dir = Vector3()
    private val mid = Vector3()
    private val aPos = Vector3()
    private val bPos = Vector3()
    private val quat = Quaternion()
    private val tint = Color()

    init {
        val builder = ModelBuilder()
        val attrs = (Usage.Position or Usage.Normal).toLong()
        val core = Material(
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createEmissive(Color.WHITE),
            ColorAttribute.createSpecular(Color.WHITE)
        )
        cylinder = builder.createCylinder(0.22f, 1f, 0.22f, 10, core, attrs)
        joint = builder.createSphere(0.28f, 0.28f, 0.28f, 10, 8, core, attrs)
        val additive = Material(
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createEmissive(Color.WHITE),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 0.45f)
        )
        glow = builder.createSphere(0.46f, 0.46f, 0.46f, 8, 6, additive, attrs)
        orb = builder.createSphere(0.34f, 0.34f, 0.34f, 10, 8, additive, attrs)
        segments = Array(POOL) { ModelInstance(cylinder) }
        joints = Array(POOL) { ModelInstance(joint) }
        glows = Array(POOL) { ModelInstance(glow) }
        cursor = ModelInstance(orb)
    }

    fun sync(board: Board3D, state: GameState, time: Float) {
        segmentCount = 0
        jointCount = 0
        glowCount = 0
        state.locked.forEach { place(board, it.cells, it.color, time, pulse = true) }
        if (state.draft.size >= 1) {
            state.draftColor?.let { place(board, state.draft, it, time, pulse = false) }
        }
        state.hintCells.let { cells ->
            if (cells.size >= 2) place(board, cells, PathColor.CYAN, time, pulse = false)
        }
        val moving = state.draft.takeIf { it.size >= 2 } ?: state.locked.lastOrNull()?.cells
        if (moving != null && moving.size >= 2) {
            val cursorCell = sample(moving, time)
            cellPos(board, cursorCell, aPos)
            cursor.transform.setToTranslation(aPos.x, Board3D.PATH_Y + 0.08f, aPos.z)
            showCursor = true
        } else {
            showCursor = false
        }
    }

    fun renderOpaque(batch: ModelBatch, environment: Environment) {
        for (i in 0 until segmentCount) batch.render(segments[i], environment)
        for (i in 0 until jointCount) batch.render(joints[i], environment)
    }

    fun renderGlow(batch: ModelBatch, environment: Environment) {
        for (i in 0 until glowCount) batch.render(glows[i], environment)
        if (showCursor) batch.render(cursor, environment)
    }

    private fun place(board: Board3D, cells: List<GridPos>, color: PathColor, time: Float, pulse: Boolean) {
        paint(color, pulse, time)
        for (index in 0 until cells.size - 1) {
            if (segmentCount >= POOL) break
            cellPos(board, cells[index], aPos)
            cellPos(board, cells[index + 1], bPos)
            dir.set(bPos.x - aPos.x, 0f, bPos.z - aPos.z)
            val length = dir.len().coerceAtLeast(0.05f)
            dir.scl(1f / length)
            mid.set(aPos).lerp(bPos, 0.5f)
            quat.setFromCross(up, dir)
            val instance = segments[segmentCount++]
            tint(instance, color, 0.15f)
            instance.transform.idt().translate(mid).rotate(quat).scale(1f, length, 1f)
        }
        cells.forEach { cell ->
            if (jointCount >= POOL) return@forEach
            cellPos(board, cell, aPos)
            val instance = joints[jointCount++]
            tint(instance, color, 0.25f)
            val scale = if (pulse) 1f + 0.06f * sin(time * 3f + cell.x) else 1f
            instance.transform.setToTranslation(aPos).scale(scale, scale, scale)
            if (glowCount < POOL) {
                val halo = glows[glowCount++]
                tint(halo, color, 0.85f)
                halo.transform.setToTranslation(aPos.x, Board3D.PATH_Y, aPos.z).scale(1.15f, 0.45f, 1.15f)
            }
        }
    }

    private fun paint(color: PathColor, pulse: Boolean, time: Float) {
        val wave = if (pulse) 0.75f + 0.25f * sin(time * 4f) else 1f
        Color.argb8888ToColor(tint, color.argb)
        tint.mul(wave)
    }

    private fun tint(instance: ModelInstance, color: PathColor, emissive: Float) {
        Color.argb8888ToColor(tint, color.argb)
        val material = instance.materials.first()
        (material.get(ColorAttribute.Diffuse) as? ColorAttribute)?.color?.set(tint)
        (material.get(ColorAttribute.Emissive) as? ColorAttribute)?.color?.set(
            tint.r * emissive, tint.g * emissive, tint.b * emissive, 1f
        )
    }

    private fun cellPos(board: Board3D, cell: GridPos, out: Vector3) {
        out.set(board.centerX(cell), Board3D.PATH_Y, board.centerZ(cell))
    }

    private fun sample(cells: List<GridPos>, time: Float): GridPos {
        val span = (cells.size - 1).coerceAtLeast(1)
        val cursor = (time * 0.65f) % span
        val index = cursor.toInt().coerceIn(0, cells.lastIndex)
        return cells[index]
    }

    override fun dispose() {
        cylinder.dispose()
        joint.dispose()
        glow.dispose()
        orb.dispose()
    }

    companion object {
        private const val POOL = 180
    }
}
