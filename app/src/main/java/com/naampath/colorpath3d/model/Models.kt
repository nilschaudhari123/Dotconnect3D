package com.naampath.colorpath3d.model

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun left(): Direction = when (this) {
        UP -> LEFT
        LEFT -> DOWN
        DOWN -> RIGHT
        RIGHT -> UP
    }

    fun right(): Direction = when (this) {
        UP -> RIGHT
        RIGHT -> DOWN
        DOWN -> LEFT
        LEFT -> UP
    }

    companion object {
        fun axis(from: GridPos, to: GridPos): Direction? {
            val dx = to.x - from.x
            val dy = to.y - from.y
            if (dx != 0 && dy != 0) return null
            if (dx == 0 && dy == 0) return null
            val sx = dx.sign
            val sy = dy.sign
            return entries.first { it.dx == sx && it.dy == sy }
        }

        fun unit(from: GridPos, to: GridPos): Direction? {
            val dx = to.x - from.x
            val dy = to.y - from.y
            if (kotlin.math.abs(dx) + kotlin.math.abs(dy) != 1) return null
            return entries.first { it.dx == dx && it.dy == dy }
        }
    }
}

private val Int.sign: Int get() = when {
    this > 0 -> 1
    this < 0 -> -1
    else -> 0
}

data class GridPos(val x: Int, val y: Int) {
    fun step(direction: Direction, distance: Int = 1): GridPos =
        GridPos(x + direction.dx * distance, y + direction.dy * distance)

    fun inBoard(size: Int): Boolean = x in 0 until size && y in 0 until size

    fun orthogonalNeighbors(): List<GridPos> = Direction.entries.map { step(it) }
}

enum class PathColor(val argb: Int, val symbol: NodeSymbol) {
    RED(0xFFFF3B5C.toInt(), NodeSymbol.CIRCLE),
    BLUE(0xFF3D7CFF.toInt(), NodeSymbol.TRIANGLE),
    YELLOW(0xFFFFC857.toInt(), NodeSymbol.SQUARE),
    PURPLE(0xFFB45CFF.toInt(), NodeSymbol.DIAMOND),
    PINK(0xFFFF6AD5.toInt(), NodeSymbol.STAR),
    GREEN(0xFF3DDC97.toInt(), NodeSymbol.HEX),
    ORANGE(0xFFFF8A3D.toInt(), NodeSymbol.CROSS),
    CYAN(0xFF3DFFF2.toInt(), NodeSymbol.RING)
}

enum class NodeSymbol {
    CIRCLE, TRIANGLE, SQUARE, DIAMOND, STAR, HEX, CROSS, RING
}

enum class GameMode {
    CLASSIC,
    TIMED,
    MOVES,
    PERFECT,
    DAILY,
    ENDLESS,
    ZEN,
    HARD
}

enum class RunStatus {
    PLAYING,
    COMPLETE,
    FAILED,
    TIME_UP,
    MOVES_EXHAUSTED
}

enum class Quality { LOW, MEDIUM, HIGH, AUTO }

data class ColorPair(
    val color: PathColor,
    val symbol: NodeSymbol,
    val a: GridPos,
    val b: GridPos
)

enum class ObstacleType {
    BLOCK,
    METAL,
    CRYSTAL,
    ROTATING,
    BARRIER,
    LOCKED,
    ICE,
    PORTAL,
    TELEPORT,
    ONE_WAY
}

data class Obstacle(
    val position: GridPos,
    val type: ObstacleType,
    val oneWay: Direction? = null,
    val portalLink: GridPos? = null,
    val opensAfter: Int = 0
)

data class Level(
    val id: Int,
    val worldId: Int,
    val size: Int,
    val pairs: List<ColorPair>,
    val obstacles: List<Obstacle>,
    val solution: List<List<GridPos>>,
    val parMoves: Int,
    val parTimeSec: Int,
    val supportsEditing: Boolean = true
) {
    fun pair(color: PathColor): ColorPair? = pairs.find { it.color == color }

    fun otherEnd(color: PathColor, start: GridPos): GridPos? {
        val pair = pair(color) ?: return null
        return when (start) {
            pair.a -> pair.b
            pair.b -> pair.a
            else -> null
        }
    }

    fun endpointAt(pos: GridPos): ColorPair? = pairs.find { it.a == pos || it.b == pos }
}
