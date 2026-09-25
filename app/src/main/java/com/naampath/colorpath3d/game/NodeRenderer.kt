package com.naampath.colorpath3d.game

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
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.model.NodeSymbol
import com.naampath.colorpath3d.model.PathColor
import com.naampath.colorpath3d.theme.Presentation
import kotlin.math.sin

class NodeRenderer : Disposable {
    private val core: Model
    private val halo: Model
    private val ring: Model
    private val symbols: Map<NodeSymbol, Model>
    private val cores = mutableListOf<ModelInstance>()
    private val halos = mutableListOf<ModelInstance>()
    private val rings = mutableListOf<ModelInstance>()
    private val markers = mutableListOf<ModelInstance>()
    private val positions = mutableListOf<Vector3>()
    private val colors = mutableListOf<PathColor>()
    private val tint = Color()

    init {
        val builder = ModelBuilder()
        val attrs = (Usage.Position or Usage.Normal).toLong()
        val lit = Material(
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createSpecular(Color.WHITE),
            ColorAttribute.createEmissive(Color.WHITE)
        )
        core = builder.createSphere(0.46f, 0.46f, 0.46f, 16, 12, lit, attrs)
        ring = builder.createCylinder(0.62f, 0.08f, 0.62f, 16, lit, attrs)
        val glowMat = Material(
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createEmissive(Color.WHITE),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE, 0.35f)
        )
        halo = builder.createSphere(0.78f, 0.78f, 0.78f, 12, 8, glowMat, attrs)
        symbols = NodeSymbol.entries.associateWith { symbolModel(builder, attrs, it) }
    }

    fun build(board: Board3D) {
        cores.clear()
        halos.clear()
        rings.clear()
        markers.clear()
        positions.clear()
        colors.clear()
        board.cells.forEach { cell ->
            val color = cell.endpoint ?: return@forEach
            val pos = Vector3(board.centerX(cell.pos), Board3D.NODE_Y, board.centerZ(cell.pos))
            positions += pos
            colors += color
            cores += ModelInstance(core)
            halos += ModelInstance(halo)
            rings += ModelInstance(ring)
            val symbol = cell.symbol ?: color.symbol
            markers += ModelInstance(symbols.getValue(symbol))
        }
    }

    fun render(batch: ModelBatch, environment: Environment, state: GameState, presentation: Presentation, time: Float) {
        for (index in cores.indices) {
            val color = colors[index]
            val selected = state.draftColor == color
            val pulse = 1f + 0.07f * sin(time * 2.4f + index) + if (selected) 0.12f else 0f
            val pos = positions[index]
            tint(cores[index], color, 0.35f)
            tint(rings[index], color, 0.15f)
            tint(halos[index], color, 0.9f)
            tint(markers[index], color, 0.8f)
            cores[index].transform.setToTranslation(pos).scale(pulse, pulse, pulse)
            rings[index].transform.setToTranslation(pos.x, pos.y - 0.12f, pos.z)
            halos[index].transform.setToTranslation(pos).scale(pulse, pulse, pulse)
            markers[index].transform.setToTranslation(pos.x, pos.y + 0.32f, pos.z)
            batch.render(rings[index], environment)
            batch.render(cores[index], environment)
            if (presentation.colorBlind || presentation.highContrast) {
                batch.render(markers[index], environment)
            }
        }
    }

    fun renderHalo(batch: ModelBatch, environment: Environment) {
        halos.forEach { batch.render(it, environment) }
    }

    private fun tint(instance: ModelInstance, color: PathColor, emissive: Float) {
        Color.argb8888ToColor(tint, color.argb)
        val material = instance.materials.first()
        (material.get(ColorAttribute.Diffuse) as? ColorAttribute)?.color?.set(tint)
        (material.get(ColorAttribute.Emissive) as? ColorAttribute)?.color?.set(
            tint.r * emissive, tint.g * emissive, tint.b * emissive, 1f
        )
    }

    private fun symbolModel(builder: ModelBuilder, attrs: Long, symbol: NodeSymbol): Model {
        builder.begin()
        val material = Material(
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createEmissive(Color.WHITE)
        )
        val part = builder.part("mark", GL20.GL_TRIANGLES, attrs, material)
        fun v(x: Float, z: Float) = part.vertex(x, 0f, z, 0f, 1f, 0f)
        when (symbol) {
            NodeSymbol.CIRCLE, NodeSymbol.RING, NodeSymbol.HEX -> {
                val a = v(0f, 0.16f); val b = v(-0.14f, -0.08f); val c = v(0.14f, -0.08f)
                part.triangle(a, b, c)
            }
            NodeSymbol.TRIANGLE -> {
                val a = v(0f, 0.18f); val b = v(-0.16f, -0.12f); val c = v(0.16f, -0.12f)
                part.triangle(a, b, c)
            }
            NodeSymbol.SQUARE, NodeSymbol.DIAMOND -> {
                val a = v(0f, 0.16f); val b = v(-0.16f, 0f); val c = v(0f, -0.16f); val d = v(0.16f, 0f)
                part.triangle(a, b, c)
                part.triangle(a, c, d)
            }
            NodeSymbol.STAR, NodeSymbol.CROSS -> {
                val a = v(0f, 0.18f); val b = v(-0.05f, 0f); val c = v(0.05f, 0f)
                val d = v(-0.16f, 0f); val e = v(0.16f, 0f); val f = v(0f, -0.16f)
                part.triangle(a, b, c)
                part.triangle(d, f, e)
            }
        }
        return builder.end()
    }

    override fun dispose() {
        core.dispose()
        halo.dispose()
        ring.dispose()
        symbols.values.forEach { it.dispose() }
    }
}
