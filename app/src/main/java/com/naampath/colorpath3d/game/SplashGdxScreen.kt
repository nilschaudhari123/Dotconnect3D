package com.naampath.colorpath3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.naampath.colorpath3d.model.PathColor
import kotlin.math.sin

/** Short 3D assembly: tiles rise, nodes appear, a path links them, then the menu takes over. */
class SplashGdxScreen(private val game: ColorPath3DGame) : ScreenAdapter() {
    private val camera = PerspectiveCamera(48f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val batch = ModelBatch()
    private val environment = Environment()
    private val models = mutableListOf<Model>()
    private val tiles = mutableListOf<ModelInstance>()
    private val nodes = mutableListOf<ModelInstance>()
    private var path: ModelInstance? = null
    private var elapsed = 0f
    private var finished = false

    override fun show() {
        camera.near = 0.1f
        camera.far = 40f
        camera.position.set(0f, 4.2f, 6.5f)
        camera.lookAt(0f, 0.2f, 0f)
        camera.update()
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.25f, 0.28f, 0.4f, 1f))
        environment.add(DirectionalLight().set(0.8f, 0.85f, 1f, -0.3f, -1f, -0.2f))
        val builder = ModelBuilder()
        val attrs = (Usage.Position or Usage.Normal).toLong()
        val tile = builder.createBox(
            0.9f, 0.16f, 0.9f,
            Material(ColorAttribute.createDiffuse(0.12f, 0.16f, 0.28f, 1f), ColorAttribute.createEmissive(0.05f, 0.2f, 0.25f, 1f)),
            attrs
        )
        models += tile
        for (y in -1..1) {
            for (x in -1..1) {
                tiles += ModelInstance(tile).apply {
                    transform.setToTranslation(x * 1.05f, -2f, y * 1.05f)
                }
            }
        }
        PathColor.entries.take(3).forEachIndexed { index, color ->
            val model = builder.createSphere(
                0.42f, 0.42f, 0.42f, 14, 10,
                Material(
                    ColorAttribute.createDiffuse(Color(color.argb)),
                    ColorAttribute.createEmissive(Color(color.argb))
                ),
                attrs
            )
            models += model
            val x = if (index == 0) -1.05f else if (index == 1) 1.05f else 0f
            nodes += ModelInstance(model).apply { transform.setToTranslation(x, -2f, if (index == 2) 1.05f else -1.05f) }
        }
        val bar = builder.createCylinder(
            0.16f, 2.1f, 0.16f, 8,
            Material(ColorAttribute.createDiffuse(Color(PathColor.CYAN.argb)), ColorAttribute.createEmissive(Color(PathColor.CYAN.argb))),
            attrs
        )
        models += bar
        path = ModelInstance(bar).apply { transform.setToTranslation(0f, 0.35f, -1.05f).rotate(0f, 0f, 1f, 90f).scale(1f, 0.01f, 1f) }
    }

    override fun render(delta: Float) {
        elapsed += delta
        Gdx.gl.glClearColor(0.03f, 0.04f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        tiles.forEachIndexed { index, instance ->
            val delay = index * 0.04f
            val t = ((elapsed - delay) / 0.45f).coerceIn(0f, 1f)
            val y = MathUtils.lerp(-1.4f, 0.08f, t)
            val x = (index % 3 - 1) * 1.05f
            val z = (index / 3 - 1) * 1.05f
            instance.transform.setToTranslation(x, y, z)
        }
        nodes.forEachIndexed { index, instance ->
            val t = ((elapsed - 0.55f - index * 0.12f) / 0.35f).coerceIn(0f, 1f)
            val pulse = 1f + 0.06f * sin(elapsed * 3f)
            val x = if (index == 0) -1.05f else if (index == 1) 1.05f else 0f
            val z = if (index == 2) 1.05f else -1.05f
            instance.transform.setToTranslation(x, MathUtils.lerp(-1f, 0.48f, t), z).scale(pulse * t, pulse * t, pulse * t)
        }
        val grow = ((elapsed - 1.05f) / 0.4f).coerceIn(0f, 1f)
        path?.transform?.setToTranslation(0f, 0.4f, -1.05f)?.rotate(0f, 0f, 1f, 90f)?.scale(1f, grow, 1f)
        camera.position.x = sin(elapsed * 0.4f) * 0.4f
        camera.lookAt(0f, 0.2f, 0f)
        camera.update()
        batch.begin(camera)
        tiles.forEach { batch.render(it, environment) }
        if (elapsed > 0.5f) nodes.forEach { batch.render(it, environment) }
        if (elapsed > 1f) path?.let { batch.render(it, environment) }
        batch.end()
        if (elapsed >= 2.1f && !finished) {
            finished = true
            game.host.onSplashFinished()
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat().coerceAtLeast(1f)
        camera.viewportHeight = height.toFloat().coerceAtLeast(1f)
        camera.update()
    }

    override fun dispose() {
        batch.dispose()
        models.forEach { it.dispose() }
    }
}
