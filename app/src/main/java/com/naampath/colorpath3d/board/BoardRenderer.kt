package com.naampath.colorpath3d.board

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.ObstacleType
import com.naampath.colorpath3d.theme.GameTheme

class BoardRenderer : Disposable {
    private val models = mutableListOf<Model>()
    private val tiles = mutableListOf<ModelInstance>()
    private val skirts = mutableListOf<ModelInstance>()
    private val obstacles = mutableListOf<ModelInstance>()
    private val spinners = mutableListOf<Pair<ModelInstance, com.badlogic.gdx.math.Vector3>>()
    private var ground: ModelInstance? = null
    private val tmpColor = Color()

    fun build(level: Level, theme: GameTheme, highContrast: Boolean) {
        disposeModels()
        tiles.clear()
        skirts.clear()
        obstacles.clear()
        spinners.clear()
        val board = Board3D.from(level)
        val builder = ModelBuilder()
        val attrs = (Usage.Position or Usage.Normal).toLong()
        val tileColor = color(if (highContrast) theme.tileEdge else theme.tile, if (highContrast) 1f else 0.9f)
        val edge = color(theme.tileEdge, 0.55f)
        val tileModel = builder.createBox(0.9f, Board3D.TILE_HEIGHT, 0.9f, lit(tileColor, 0.08f), attrs)
        val skirtModel = builder.createBox(0.96f, 0.05f, 0.96f, diffuseOnly(color(0xFF05060A.toInt(), 0.85f)), attrs)
        val blockModel = builder.createBox(0.72f, 0.55f, 0.72f, lit(color(theme.metal, 1f), 0.02f), attrs)
        val crystalModel = builder.createBox(0.55f, 0.7f, 0.55f, lit(color(theme.accent, 1f), 0.35f), attrs)
        val spinModel = builder.createBox(0.5f, 0.5f, 0.5f, lit(color(theme.accent, 1f), 0.2f), attrs)
        models += listOf(tileModel, skirtModel, blockModel, crystalModel, spinModel)
        val groundModel = builder.createCylinder(board.size + 1.6f, 0.08f, board.size + 1.6f, 8, diffuseOnly(color(theme.background, 1f)), attrs)
        models += groundModel
        ground = ModelInstance(groundModel).apply { transform.setToTranslation(0f, -0.12f, 0f) }
        board.cells.forEach { cell ->
            val x = board.centerX(cell.pos)
            val z = board.centerZ(cell.pos)
            tiles += ModelInstance(tileModel).apply { transform.setToTranslation(x, Board3D.TILE_HEIGHT / 2f, z) }
            skirts += ModelInstance(skirtModel).apply { transform.setToTranslation(x, 0.02f, z) }
            val obstacle = level.obstacles.find { it.position == cell.pos }
            if (obstacle != null && obstacle.type != ObstacleType.ICE && obstacle.type != ObstacleType.ONE_WAY &&
                obstacle.type != ObstacleType.PORTAL && obstacle.type != ObstacleType.TELEPORT &&
                obstacle.type != ObstacleType.BARRIER && obstacle.type != ObstacleType.LOCKED
            ) {
                val model = when (obstacle.type) {
                    ObstacleType.CRYSTAL -> crystalModel
                    ObstacleType.ROTATING -> spinModel
                    else -> blockModel
                }
                val instance = ModelInstance(model)
                if (obstacle.type == ObstacleType.ROTATING) {
                    spinners += instance to com.badlogic.gdx.math.Vector3(x, 0.4f, z)
                } else {
                    instance.transform.setToTranslation(x, 0.36f, z)
                    obstacles += instance
                }
            }
            if (obstacle?.type == ObstacleType.BARRIER || obstacle?.type == ObstacleType.LOCKED) {
                obstacles += ModelInstance(crystalModel).apply {
                    transform.setToTranslation(x, 0.42f, z)
                    transform.scale(0.7f, 1.1f, 0.7f)
                }
            }
        }
        edge.a = 1f
    }

    fun render(batch: ModelBatch, environment: Environment, time: Float) {
        ground?.let { batch.render(it, environment) }
        skirts.forEach { batch.render(it, environment) }
        tiles.forEach { batch.render(it, environment) }
        obstacles.forEach { batch.render(it, environment) }
        spinners.forEach { (instance, pos) ->
            instance.transform.setToTranslation(pos).rotate(com.badlogic.gdx.math.Vector3.Y, time * 40f)
            batch.render(instance, environment)
        }
    }

    private fun lit(color: Color, emissive: Float): Material = Material(
        ColorAttribute.createDiffuse(color),
        ColorAttribute.createSpecular(Color.WHITE),
        ColorAttribute.createEmissive(color.r * emissive, color.g * emissive, color.b * emissive, 1f),
        FloatAttribute.createShininess(18f)
    )

    private fun diffuseOnly(color: Color) = Material(ColorAttribute.createDiffuse(color))

    private fun color(argb: Int, scale: Float): Color {
        Color.argb8888ToColor(tmpColor, argb)
        tmpColor.mul(scale)
        tmpColor.a = 1f
        return tmpColor.cpy()
    }

    private fun disposeModels() {
        models.forEach { it.dispose() }
        models.clear()
    }

    override fun dispose() {
        disposeModels()
    }

    companion object {
        val GL_TRIANGLES: Int = GL20.GL_TRIANGLES
    }
}
