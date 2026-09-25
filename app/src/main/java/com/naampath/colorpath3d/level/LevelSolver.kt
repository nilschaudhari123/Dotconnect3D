package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.path.CollisionManager
import com.naampath.colorpath3d.path.PathValidator
import com.naampath.colorpath3d.path.StepOutcome

/** Search solver used to confirm compact levels have a route independent of the authored path. */
class LevelSolver {
    fun solve(level: Level, nodeLimit: Int = 80_000): List<List<GridPos>>? {
        val board = Board3D.from(level)
        val validator = PathValidator(board, CollisionManager())
        val found = MutableList(level.pairs.size) { emptyList<GridPos>() }
        var nodes = 0
        var exhausted = false

        fun search(pairIndex: Int, occupied: Set<GridPos>): Boolean {
            if (exhausted) return false
            if (pairIndex == level.pairs.size) return true
            val pair = level.pairs[pairIndex]
            val path = mutableListOf(pair.a)
            val visited = mutableSetOf(pair.a)

            fun walk(): Boolean {
                if (exhausted) return false
                if (path.last() == pair.b && path.size >= 2) {
                    found[pairIndex] = path.toList()
                    return search(pairIndex + 1, occupied + path)
                }
                if (path.size >= level.size * level.size) return false
                for (direction in Direction.entries) {
                    if (++nodes > nodeLimit) {
                        exhausted = true
                        return false
                    }
                    val next = path.last().step(direction)
                    val outcome = validator.resolveStep(
                        level, pair.color, path.last(), next, path, occupied, pairIndex
                    )
                    if (outcome !is StepOutcome.Extend) continue
                    if (outcome.cells.any { it in visited }) continue
                    path.addAll(outcome.cells)
                    outcome.cells.forEach { visited += it }
                    if (walk()) return true
                    outcome.cells.forEach { visited -= it }
                    repeat(outcome.cells.size) { path.removeAt(path.lastIndex) }
                }
                return false
            }
            return walk()
        }
        return if (search(0, emptySet()) && !exhausted) found else null
    }
}
