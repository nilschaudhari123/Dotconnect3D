package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.tuning.DifficultyTuning
import java.time.LocalDate

interface LevelRepository {
    fun level(id: Int, mode: GameMode = GameMode.CLASSIC, tuning: DifficultyTuning = DifficultyTuning()): Level
    fun daily(date: LocalDate, tuning: DifficultyTuning = DifficultyTuning()): Level
    fun endless(index: Int, tuning: DifficultyTuning = DifficultyTuning()): Level
    fun campaignSize(): Int = WorldCatalog.CAMPAIGN_SIZE
    fun worlds(): List<World> = WorldCatalog.worlds
}

class ProceduralLevelRepository(
    private val generator: LevelGenerator = LevelGenerator()
) : LevelRepository {
    private val cache = object : LinkedHashMap<String, Level>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Level>?) = size > 24
    }

    override fun level(id: Int, mode: GameMode, tuning: DifficultyTuning): Level {
        val key = "L:$id:${mode.name}:${tuning.obstacleMultiplier}:${tuning.extraPairs}"
        return cache.getOrPut(key) { generator.generate(id.coerceIn(1, campaignSize()), mode, tuning) }
    }

    override fun daily(date: LocalDate, tuning: DifficultyTuning): Level {
        val key = "D:${date.toEpochDay()}:${tuning.obstacleMultiplier}"
        return cache.getOrPut(key) { DailyChallengeFactory.level(date, tuning, generator) }
    }

    override fun endless(index: Int, tuning: DifficultyTuning): Level {
        val safe = index.coerceAtLeast(0)
        val key = "E:$safe"
        return cache.getOrPut(key) {
            val spec = DifficultyCurve.spec(1 + (safe % WorldCatalog.CAMPAIGN_SIZE), GameMode.CLASSIC, tuning)
            generator.generateSpec(
                spec.copy(levelId = 100_000 + safe, seed = safe * 7919L + 42L, worldId = 0)
            )
        }
    }
}
