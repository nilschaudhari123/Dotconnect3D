package com.naampath.colorpath3d.game

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.model.GridPos
import kotlin.math.abs

class BoardCameraRig(val camera: PerspectiveCamera) {
    var distance = 12f
    var yawDegrees = 20f
    var pitchDegrees = 58f
    private val center = Vector3(0f, 0.2f, 0f)
    private val rayHit = Vector3()

    fun fit(size: Int) {
        distance = when {
            size <= 5 -> 8.5f
            size <= 7 -> 10.5f
            size <= 9 -> 13f
            size <= 10 -> 14.5f
            else -> 17.5f
        }
        val maxDistance = size * 2.4f + 6f
        val minDistance = size * 0.72f + 2.5f
        distance = distance.coerceIn(minDistance, maxDistance)
    }

    fun zoomBy(factor: Float) {
        val minDistance = 4.5f
        val maxDistance = 26f
        distance = (distance * factor).coerceIn(minDistance, maxDistance)
    }

    fun rotate(deltaDegrees: Float) {
        yawDegrees = (yawDegrees + deltaDegrees) % 360f
    }

    fun update(shake: Float) {
        val yaw = yawDegrees * MathUtils.degreesToRadians
        val pitch = pitchDegrees * MathUtils.degreesToRadians
        val horizontal = distance * MathUtils.cos(pitch)
        camera.position.set(
            center.x + horizontal * MathUtils.sin(yaw) + shake,
            center.y + distance * MathUtils.sin(pitch),
            center.z + horizontal * MathUtils.cos(yaw)
        )
        camera.up.set(Vector3.Y)
        camera.lookAt(center)
        camera.update()
    }

    fun pick(screenX: Float, screenY: Float, size: Int): GridPos? {
        val ray = camera.getPickRay(screenX, screenY)
        if (abs(ray.direction.y) < 0.0001f) return null
        val t = (Board3D.TILE_HEIGHT - ray.origin.y) / ray.direction.y
        if (t < 0f) return null
        rayHit.set(ray.origin).mulAdd(ray.direction, t)
        val cell = Board3D.CELL_SIZE
        val origin = -size * cell / 2f
        val gx = MathUtils.floor((rayHit.x - origin) / cell)
        val gy = MathUtils.floor((rayHit.z - origin) / cell)
        val pos = GridPos(gx, gy)
        return if (pos.inBoard(size)) pos else null
    }
}
