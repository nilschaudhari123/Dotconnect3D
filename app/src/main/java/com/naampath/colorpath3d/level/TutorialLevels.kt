package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.model.ColorPair
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.Obstacle
import com.naampath.colorpath3d.model.ObstacleType
import com.naampath.colorpath3d.model.PathColor

object TutorialLevels {
    fun level(id: Int): Level = when (id) {
        1 -> straight()
        2 -> aroundBlocks()
        else -> threeColors()
    }

    private fun straight(): Level {
        val red = row(0)
        val blue = row(4)
        return pack(1, listOf(PathColor.RED to red, PathColor.BLUE to blue), emptyList(), 45)
    }

    private fun aroundBlocks(): Level {
        val red = (0..4).map { GridPos(0, it) }
        val blue = listOf(GridPos(2, 0), GridPos(3, 0), GridPos(4, 0), GridPos(4, 1), GridPos(4, 2))
        val blocks = listOf(
            Obstacle(GridPos(1, 1), ObstacleType.BLOCK),
            Obstacle(GridPos(2, 2), ObstacleType.METAL)
        )
        return pack(2, listOf(PathColor.RED to red, PathColor.BLUE to blue), blocks, 50)
    }

    private fun threeColors(): Level {
        val red = (0..4).map { GridPos(it, 0) } + (1..4).map { GridPos(4, it) }
        val blue = listOf(
            GridPos(0, 4), GridPos(0, 3), GridPos(0, 2), GridPos(0, 1),
            GridPos(1, 1), GridPos(2, 1), GridPos(3, 1), GridPos(3, 2)
        )
        val yellow = listOf(GridPos(2, 4), GridPos(1, 4), GridPos(1, 3))
        val blocks = listOf(
            Obstacle(GridPos(2, 2), ObstacleType.CRYSTAL),
            Obstacle(GridPos(2, 3), ObstacleType.BLOCK)
        )
        return pack(
            3,
            listOf(PathColor.RED to red, PathColor.BLUE to blue, PathColor.YELLOW to yellow),
            blocks,
            70
        )
    }

    private fun row(y: Int) = (0..4).map { GridPos(it, y) }

    private fun pack(
        id: Int,
        paths: List<Pair<PathColor, List<GridPos>>>,
        obstacles: List<Obstacle>,
        parTime: Int
    ): Level {
        val pairs = paths.map { (color, path) ->
            ColorPair(color, color.symbol, path.first(), path.last())
        }
        return Level(
            id = id,
            worldId = 1,
            size = 5,
            pairs = pairs,
            obstacles = obstacles,
            solution = paths.map { it.second },
            parMoves = pairs.size,
            parTimeSec = parTime,
            supportsEditing = true
        )
    }
}
