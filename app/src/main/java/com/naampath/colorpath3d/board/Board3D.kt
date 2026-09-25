package com.naampath.colorpath3d.board

import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.ObstacleType

/**
 * Logical 3D board. Cell centers are derived from board size so the renderer
 * never hardcodes coordinates.
 */
class Board3D(
    val size: Int,
    val cells: List<GridCell>
) {
    fun cellAt(pos: GridPos): GridCell? {
        if (!pos.inBoard(size)) return null
        return cells[pos.y * size + pos.x]
    }

    fun centerX(pos: GridPos, cellSize: Float = CELL_SIZE): Float {
        val origin = -size * cellSize / 2f + cellSize / 2f
        return origin + pos.x * cellSize
    }

    fun centerZ(pos: GridPos, cellSize: Float = CELL_SIZE): Float {
        val origin = -size * cellSize / 2f + cellSize / 2f
        return origin + pos.y * cellSize
    }

    companion object {
        const val CELL_SIZE = 1f
        const val TILE_HEIGHT = 0.18f
        const val PATH_Y = 0.34f
        const val NODE_Y = 0.52f

        fun from(level: Level): Board3D {
            val grid = MutableList(level.size * level.size) { index ->
                val x = index % level.size
                val y = index / level.size
                GridCell(GridPos(x, y))
            }
            level.obstacles.forEach { obstacle ->
                val index = obstacle.position.y * level.size + obstacle.position.x
                if (index !in grid.indices) return@forEach
                val kind = when (obstacle.type) {
                    ObstacleType.BLOCK -> CellKind.BLOCK
                    ObstacleType.METAL -> CellKind.METAL
                    ObstacleType.CRYSTAL -> CellKind.CRYSTAL
                    ObstacleType.ROTATING -> CellKind.ROTATING
                    ObstacleType.BARRIER -> CellKind.BARRIER
                    ObstacleType.LOCKED -> CellKind.LOCKED
                    ObstacleType.ICE -> CellKind.ICE
                    ObstacleType.PORTAL -> CellKind.PORTAL
                    ObstacleType.TELEPORT -> CellKind.TELEPORT
                    ObstacleType.ONE_WAY -> CellKind.ONE_WAY
                }
                grid[index] = grid[index].copy(
                    kind = kind,
                    oneWay = obstacle.oneWay,
                    portalLink = obstacle.portalLink,
                    opensAfter = obstacle.opensAfter
                )
            }
            level.pairs.forEach { pair ->
                listOf(pair.a, pair.b).forEach { pos ->
                    val index = pos.y * level.size + pos.x
                    if (index in grid.indices) {
                        grid[index] = grid[index].copy(endpoint = pair.color, symbol = pair.symbol)
                    }
                }
            }
            return Board3D(level.size, grid)
        }
    }
}
