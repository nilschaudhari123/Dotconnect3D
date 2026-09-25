package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.tuning.DifficultyTuning
import java.time.LocalDate

object DailyChallengeFactory {
    fun seed(epochDay: Long): Long = epochDay * 9973L + 0x5F3759DF

    fun level(date: LocalDate, tuning: DifficultyTuning = DifficultyTuning(), generator: LevelGenerator = LevelGenerator()): Level {
        val day = date.toEpochDay()
        val size = intArrayOf(6, 7, 8, 9, 10)[(kotlin.math.abs(day % 5)).toInt()]
        val pairs = (3 + (kotlin.math.abs(day % 3)).toInt()).coerceAtMost(size - 1)
        val spec = LevelSpec(
            levelId = 10_000 + (kotlin.math.abs(day % 100_000)).toInt(),
            seed = seed(day),
            size = size,
            pairs = pairs,
            obstacles = (kotlin.math.abs(day % 4)).toInt(),
            wiggles = 2 + (kotlin.math.abs(day % 3)).toInt(),
            specialTier = (kotlin.math.abs(day % 5)).toInt(),
            worldId = 0
        )
        return generator.generateSpec(spec)
    }
}
