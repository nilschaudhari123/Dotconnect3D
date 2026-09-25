package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.tuning.DifficultyTuning
import kotlin.math.roundToInt

data class LevelSpec(
    val levelId: Int,
    val seed: Long,
    val size: Int,
    val pairs: Int,
    val obstacles: Int,
    val wiggles: Int,
    val specialTier: Int,
    val worldId: Int
)

object DifficultyCurve {
    fun spec(levelId: Int, mode: GameMode, tuning: DifficultyTuning = DifficultyTuning()): LevelSpec {
        val safeId = levelId.coerceAtLeast(1)
        val world = ((safeId - 1) / 100 + 1).coerceIn(1, 10)
        var size = sizeFor(safeId)
        var pairs = pairsFor(safeId, size)
        var obstacles = obstaclesFor(safeId, size, world)
        var wiggles = wigglesFor(safeId, size, world)
        var tier = tierFor(world)
        if (mode == GameMode.HARD) {
            size = nextSize(size)
            pairs = (pairs + 1 + tuning.extraPairs).coerceAtMost(8)
            obstacles += 2
            wiggles += 2
            tier = maxOf(tier, 2)
        }
        obstacles = (obstacles * tuning.obstacleMultiplier).roundToInt().coerceAtLeast(0)
        pairs = pairs.coerceIn(2, minOf(8, size - 1))
        val seed = safeId * 10007L + mode.ordinal * 17L + tuning.extraPairs * 3L
        return LevelSpec(safeId, seed, size, pairs, obstacles, wiggles, tier, world)
    }

    fun sizeFor(levelId: Int): Int = when {
        levelId <= 30 -> 5
        levelId <= 100 -> 6
        levelId <= 200 -> 6
        levelId <= 300 -> 7
        levelId <= 450 -> 8
        levelId <= 600 -> 8
        levelId <= 750 -> 9
        levelId <= 900 -> 10
        levelId <= 960 -> 10
        else -> 12
    }

    private fun nextSize(size: Int): Int = when (size) {
        5 -> 6
        6 -> 7
        7 -> 8
        8 -> 9
        9 -> 10
        else -> 12
    }

    private fun pairsFor(levelId: Int, size: Int): Int = when (size) {
        5 -> if (levelId < 20) 2 else 3
        6 -> if (levelId < 150) 3 else 4
        7 -> 4
        8 -> if (levelId < 400) 4 else 5
        9 -> 5
        10 -> if (levelId < 850) 5 else 6
        else -> if (levelId < 980) 6 else 7
    }

    private fun obstaclesFor(levelId: Int, size: Int, world: Int): Int = when {
        levelId < 10 -> 0
        levelId < 40 -> 1
        else -> (size / 4) + (world / 3)
    }

    private fun wigglesFor(levelId: Int, size: Int, world: Int): Int = when {
        levelId < 15 -> 0
        else -> size / 2 + world / 2
    }

    private fun tierFor(world: Int): Int = when {
        world <= 2 -> 1
        world == 3 -> 2
        world == 4 -> 3
        world <= 6 -> 4
        else -> 5
    }
}
