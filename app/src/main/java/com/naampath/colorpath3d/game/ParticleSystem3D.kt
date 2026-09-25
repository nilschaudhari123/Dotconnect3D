package com.naampath.colorpath3d.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.naampath.colorpath3d.model.Quality
import kotlin.math.sin
import kotlin.random.Random

/** Pooled camera-facing particles for node glow, path energy, and completion bursts. */
class ParticleSystem3D : Disposable {
    private val texture: Texture
    private val decals: Array<Decal>
    private val velocity = Array(CAPACITY) { Vector3() }
    private val active = BooleanArray(CAPACITY)
    private val life = FloatArray(CAPACITY)
    private val maxLife = FloatArray(CAPACITY)
    private var next = 0
    var quality: Quality = Quality.HIGH

    init {
        val pixmap = Pixmap(32, 32, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                val dx = (x - 16) / 16f
                val dy = (y - 16) / 16f
                val d = (dx * dx + dy * dy).coerceAtMost(1f)
                val alpha = (1f - d) * (1f - d)
                pixmap.setColor(1f, 1f, 1f, alpha)
                pixmap.drawPixel(x, y)
            }
        }
        texture = Texture(pixmap)
        pixmap.dispose()
        val region = TextureRegion(texture)
        decals = Array(CAPACITY) { Decal.newDecal(0.18f, 0.18f, region, true) }
    }

    fun update(delta: Float) {
        for (i in 0 until cap()) {
            if (!active[i]) continue
            life[i] -= delta
            if (life[i] <= 0f) {
                active[i] = false
                continue
            }
            val decal = decals[i]
            decal.position.add(velocity[i].x * delta, velocity[i].y * delta, velocity[i].z * delta)
            val alpha = (life[i] / maxLife[i]).coerceIn(0f, 1f)
            decal.color.a = alpha
        }
    }

    fun ambient(count: Int, radius: Float, time: Float) {
        val budget = minOf(count, cap() / 2)
        for (i in 0 until budget) {
            if (active[i] && life[i] > 0.2f) continue
            val angle = time * 0.15f + i * 1.7f
            spawn(
                i,
                sin(angle) * radius,
                0.4f + (i % 5) * 0.35f,
                kotlin.math.cos(angle) * radius,
                0f, 0.05f, 0f,
                6f,
                Color(0.6f, 0.8f, 1f, 0.35f)
            )
        }
    }

    fun burst(x: Float, y: Float, z: Float, color: Color, amount: Int) {
        if (quality == Quality.LOW) return
        repeat(amount.coerceAtMost(24)) {
            val slot = (cap() / 2 + next) % cap()
            next = (next + 1) % (cap() / 2).coerceAtLeast(1)
            val rng = Random(slot * 17 + amount)
            spawn(
                slot,
                x, y, z,
                rng.nextFloat() - 0.5f,
                rng.nextFloat() * 1.4f,
                rng.nextFloat() - 0.5f,
                0.8f,
                color
            )
        }
    }

    fun render(batch: DecalBatch) {
        for (i in 0 until cap()) {
            if (active[i]) batch.add(decals[i])
        }
        batch.flush()
    }

    private fun spawn(slot: Int, x: Float, y: Float, z: Float, vx: Float, vy: Float, vz: Float, duration: Float, color: Color) {
        if (slot !in decals.indices) return
        active[slot] = true
        life[slot] = duration
        maxLife[slot] = duration
        velocity[slot].set(vx, vy, vz)
        decals[slot].setPosition(x, y, z)
        decals[slot].setColor(color)
        val size = if (quality == Quality.LOW) 0.08f else 0.16f
        decals[slot].setDimensions(size, size)
    }

    private fun cap(): Int = when (quality) {
        Quality.LOW -> 16
        Quality.MEDIUM -> 48
        else -> CAPACITY
    }

    override fun dispose() {
        texture.dispose()
    }

    companion object {
        const val CAPACITY = 96
    }
}
