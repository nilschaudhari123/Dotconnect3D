package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.model.ColorPair
import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.Obstacle
import com.naampath.colorpath3d.model.ObstacleType
import com.naampath.colorpath3d.model.PathColor
import com.naampath.colorpath3d.tuning.DifficultyTuning
import kotlin.random.Random

class LevelGenerator(
    private val validator: LevelValidator = LevelValidator()
) {
    fun generate(
        levelId: Int,
        mode: GameMode = GameMode.CLASSIC,
        tuning: DifficultyTuning = DifficultyTuning()
    ): Level {
        if (levelId in 1..3 && mode != GameMode.HARD && mode != GameMode.ENDLESS) {
            return TutorialLevels.level(levelId)
        }
        return generateSpec(DifficultyCurve.spec(levelId, mode, tuning))
    }

    fun generateSpec(spec: LevelSpec): Level {
        val seeded = build(spec, Random(spec.seed), enableSpecials = true, wiggleBudget = spec.wiggles)
        if (validator.validate(seeded).ok) return seeded
        val plain = build(spec, Random(spec.seed), enableSpecials = false, wiggleBudget = 0)
        if (validator.validate(plain).ok) return plain
        return build(spec.copy(obstacles = 0, wiggles = 0, specialTier = 0), Random(spec.seed), false, 0)
    }

    private fun build(spec: LevelSpec, rng: Random, enableSpecials: Boolean, wiggleBudget: Int): Level {
        val reserve = when {
            spec.obstacles == 0 && wiggleBudget == 0 -> 0
            else -> maxOf(1, minOf(spec.size - spec.pairs, 1 + spec.obstacles / spec.size))
        }
        val playRows = (spec.size - reserve).coerceAtLeast(spec.pairs)
        val horizontal = rng.nextBoolean()
        val bands = allocate(playRows, spec.pairs, rng)
        val paths = mutableListOf<MutableList<GridPos>>()
        var cursor = 0
        bands.forEach { height ->
            paths += snake(cursor, cursor + height, spec.size, horizontal, rng.nextBoolean()).toMutableList()
            cursor += height
        }
        repeat(wiggleBudget) { detour(paths, spec.size, rng) }
        val occupied = paths.flatten().toSet()
        val obstacles = mutableListOf<Obstacle>()
        if (enableSpecials) {
            applyIce(paths, obstacles, spec.specialTier)
            applyOneWay(paths, obstacles, spec.specialTier, rng)
            applyBarrier(paths, obstacles, spec.specialTier)
            applyPortals(occupied, obstacles, spec, rng)
        }
        val reserved = obstacles.map { it.position }.toSet()
        val empties = allCells(spec.size).filter { it !in occupied && it !in reserved }.shuffled(rng)
        val visuals = listOf(ObstacleType.BLOCK, ObstacleType.METAL, ObstacleType.CRYSTAL, ObstacleType.ROTATING)
        empties.take(spec.obstacles).forEachIndexed { index, pos ->
            val type = if (spec.specialTier <= 1) ObstacleType.BLOCK else visuals[index % visuals.size]
            obstacles += Obstacle(pos, type)
        }
        val colors = PathColor.entries.shuffled(rng).take(paths.size)
        val pairs = paths.mapIndexed { index, path ->
            val color = colors[index]
            ColorPair(color, color.symbol, path.first(), path.last())
        }
        return Level(
            id = spec.levelId,
            worldId = spec.worldId,
            size = spec.size,
            pairs = pairs,
            obstacles = obstacles,
            solution = paths.map { it.toList() },
            parMoves = pairs.size,
            parTimeSec = (spec.size * pairs.size * 6).coerceIn(30, 280),
            supportsEditing = true
        )
    }

    private fun allocate(playRows: Int, pairs: Int, rng: Random): IntArray {
        val rows = IntArray(pairs) { 1 }
        var extra = playRows - pairs
        val order = rows.indices.shuffled(rng)
        var cursor = 0
        while (extra > 0) {
            rows[order[cursor % pairs]]++
            extra--
            cursor++
        }
        return rows
    }

    private fun snake(start: Int, end: Int, size: Int, horizontal: Boolean, forwardFirst: Boolean): List<GridPos> {
        val cells = mutableListOf<GridPos>()
        var forward = forwardFirst
        for (major in start until end) {
            val minors = if (forward) 0 until size else (size - 1) downTo 0
            for (minor in minors) {
                cells += if (horizontal) GridPos(minor, major) else GridPos(major, minor)
            }
            forward = !forward
        }
        return cells
    }

    private fun detour(paths: MutableList<MutableList<GridPos>>, size: Int, rng: Random): Boolean {
        val path = paths[rng.nextInt(paths.size)]
        if (path.size < 2) return false
        val index = rng.nextInt(path.size - 1)
        val a = path[index]
        val b = path[index + 1]
        val direction = Direction.unit(a, b) ?: return false
        val side = if (rng.nextBoolean()) direction.left() else direction.right()
        val first = a.step(side)
        val second = b.step(side)
        if (!first.inBoard(size) || !second.inBoard(size) || first == second) return false
        val occupied = paths.flatten().toSet()
        if (first in occupied || second in occupied) return false
        path.add(index + 1, second)
        path.add(index + 1, first)
        return true
    }

    private fun applyIce(paths: List<List<GridPos>>, obstacles: MutableList<Obstacle>, tier: Int) {
        if (tier < 3) return
        paths.forEach { path ->
            var index = 0
            while (index < path.size - 1) {
                val direction = Direction.unit(path[index], path[index + 1]) ?: break
                var end = index
                while (end + 1 < path.size && Direction.unit(path[end], path[end + 1]) == direction) end++
                if (end - index >= 2) {
                    for (mark in (index + 1) until end) {
                        val pos = path[mark]
                        if (pos != path.first() && pos != path.last()) {
                            obstacles += Obstacle(pos, ObstacleType.ICE)
                        }
                    }
                }
                index = if (end == index) index + 1 else end
            }
        }
    }

    private fun applyOneWay(
        paths: List<List<GridPos>>,
        obstacles: MutableList<Obstacle>,
        tier: Int,
        rng: Random
    ) {
        if (tier < 2) return
        val taken = obstacles.map { it.position }.toSet()
        paths.forEach { path ->
            val candidates = (1 until path.lastIndex).filter { path[it] !in taken }
            if (candidates.isEmpty()) return@forEach
            val at = candidates[rng.nextInt(candidates.size)]
            val direction = Direction.unit(path[at - 1], path[at]) ?: return@forEach
            obstacles += Obstacle(path[at], ObstacleType.ONE_WAY, oneWay = direction)
        }
    }

    private fun applyBarrier(paths: List<List<GridPos>>, obstacles: MutableList<Obstacle>, tier: Int) {
        if (tier < 4 || paths.size < 2) return
        val taken = obstacles.map { it.position }.toSet()
        val path = paths[1]
        val spot = path.drop(1).dropLast(1).firstOrNull { it !in taken } ?: return
        obstacles += Obstacle(spot, ObstacleType.BARRIER, opensAfter = 1)
    }

    private fun applyPortals(
        occupied: Set<GridPos>,
        obstacles: MutableList<Obstacle>,
        spec: LevelSpec,
        rng: Random
    ) {
        if (spec.specialTier < 5) return
        val taken = occupied + obstacles.map { it.position }
        val free = allCells(spec.size).filter { it !in taken }.shuffled(rng)
        if (free.size < 2) return
        val a = free[0]
        val b = free[1]
        obstacles += Obstacle(a, ObstacleType.PORTAL, portalLink = b)
        obstacles += Obstacle(b, ObstacleType.TELEPORT, portalLink = a)
    }

    private fun allCells(size: Int): List<GridPos> =
        (0 until size).flatMap { y -> (0 until size).map { x -> GridPos(x, y) } }
}
