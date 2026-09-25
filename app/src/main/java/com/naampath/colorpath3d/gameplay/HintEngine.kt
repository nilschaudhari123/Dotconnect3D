package com.naampath.colorpath3d.gameplay

import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.PathColor

class HintEngine {
    fun remainingSolution(level: Level, lockedColors: Set<PathColor>): List<GridPos>? {
        val index = level.pairs.indexOfFirst { it.color !in lockedColors }
        if (index < 0) return null
        return level.solution.getOrNull(index)
    }

    fun nextCell(level: Level, lockedColors: Set<PathColor>, draft: List<GridPos>, draftColor: PathColor?): GridPos? {
        val path = pathFor(level, lockedColors, draftColor) ?: return null
        if (draft.isNotEmpty() && draftColor != null && path.first() == draft.first()) {
            val prefix = draft.size.coerceAtMost(path.size)
            if (path.take(prefix) == draft.take(prefix) && prefix < path.size) return path[prefix]
        }
        return path.getOrNull(1)
    }

    fun partial(level: Level, lockedColors: Set<PathColor>, draftColor: PathColor?): List<GridPos> {
        val path = pathFor(level, lockedColors, draftColor) ?: return emptyList()
        val keep = (path.size / 2).coerceAtLeast(2).coerceAtMost(path.size)
        return path.take(keep)
    }

    fun full(level: Level, lockedColors: Set<PathColor>, draftColor: PathColor?): List<GridPos> {
        return pathFor(level, lockedColors, draftColor).orEmpty()
    }

    private fun pathFor(level: Level, lockedColors: Set<PathColor>, draftColor: PathColor?): List<GridPos>? {
        if (draftColor != null && draftColor !in lockedColors) {
            val index = level.pairs.indexOfFirst { it.color == draftColor }
            if (index >= 0) return level.solution.getOrNull(index)
        }
        return remainingSolution(level, lockedColors)
    }
}
