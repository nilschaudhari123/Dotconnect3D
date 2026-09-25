package com.naampath.colorpath3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.utils.Disposable
import com.naampath.colorpath3d.board.BoardRenderer
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.Quality
import com.naampath.colorpath3d.path.PathRenderer
import com.naampath.colorpath3d.theme.Presentation
import kotlin.math.sin

class GameRenderer(level: Level, presentation: Presentation) : Disposable {
    private val camera = PerspectiveCamera(46f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    val rig = BoardCameraRig(camera)
    private val batch = ModelBatch()
    private val environment = Environment()
    private val directional = DirectionalLight()
    private val point = PointLight()
    private val boardRenderer = BoardRenderer()
    private val pathRenderer = PathRenderer()
    private val nodeRenderer = NodeRenderer()
    private val particles = ParticleSystem3D()
    private val decals: DecalBatch
    private var presentation = presentation
    private var burstSent = false
    private val clear = Color()
    private val board = com.naampath.colorpath3d.board.Board3D.from(level)

    init {
        camera.near = 0.1f
        camera.far = 80f
        rig.fit(level.size)
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.28f, 0.32f, 0.42f, 1f))
        directional.set(0.85f, 0.9f, 1f, -0.4f, -1f, -0.25f)
        environment.add(directional)
        point.set(0.3f, 0.8f, 0.9f, 0f, 3f, 0f, 8f)
        if (presentation.quality != Quality.LOW) environment.add(point)
        boardRenderer.build(level, presentation.theme, presentation.highContrast)
        nodeRenderer.build(boardRendererBoard(level))
        decals = DecalBatch(ParticleSystem3D.CAPACITY, CameraGroupStrategy(camera))
        particles.quality = presentation.quality
        Color.argb8888ToColor(clear, presentation.theme.background)
    }

    fun apply(presentation: Presentation, level: Level) {
        this.presentation = presentation
        particles.quality = presentation.quality
        boardRenderer.build(level, presentation.theme, presentation.highContrast)
        Color.argb8888ToColor(clear, presentation.theme.background)
    }

    fun render(level: Level, state: GameState, time: Float, delta: Float) {
        val shake = if (state.shake) sin(time * 45f) * 0.08f else 0f
        if (state.isComplete) rig.distance += delta * 0.35f
        rig.update(shake)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        pathRenderer.sync(board, state, time)
        batch.begin(camera)
        boardRenderer.render(batch, environment, time)
        pathRenderer.renderOpaque(batch, environment)
        nodeRenderer.render(batch, environment, state, presentation, time)
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glDepthMask(false)
        batch.begin(camera)
        if (presentation.quality != Quality.LOW && !presentation.reducedEffects) {
            pathRenderer.renderGlow(batch, environment)
            nodeRenderer.renderHalo(batch, environment)
        }
        batch.end()
        Gdx.gl.glDepthMask(true)
        if (!presentation.reducedEffects) {
            val ambient = when (presentation.quality) {
                Quality.LOW -> 8
                Quality.MEDIUM -> 18
                else -> 32
            }
            particles.ambient(ambient, level.size * 0.7f, time)
            if (state.isComplete && !burstSent) {
                particles.burst(0f, 1.2f, 0f, Color(1f, 0.85f, 0.4f, 1f), 20)
                burstSent = true
            }
            particles.update(delta)
            particles.render(decals)
        }
    }

    fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat().coerceAtLeast(1f)
        camera.viewportHeight = height.toFloat().coerceAtLeast(1f)
        camera.update()
    }

    private fun boardRendererBoard(level: Level) = com.naampath.colorpath3d.board.Board3D.from(level)

    override fun dispose() {
        batch.dispose()
        boardRenderer.dispose()
        pathRenderer.dispose()
        nodeRenderer.dispose()
        particles.dispose()
        decals.dispose()
    }
}
