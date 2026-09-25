package com.naampath.colorpath3d.level

import com.naampath.colorpath3d.gameplay.GameController
import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.ObstacleType

data class ValidationReport(val ok: Boolean, val errors: List<String>)

class LevelValidator {
    fun validate(level: Level): ValidationReport {
        val errors = structural(level).toMutableList()
        if (errors.isNotEmpty()) return ValidationReport(false, errors)
        val controller = GameController(level, GameMode.CLASSIC)
        level.solution.forEachIndexed { index, path ->
            if (!controller.commitAuthoredPath(path)) {
                errors += "solution ${index + 1} cannot be played"
            }
        }
        if (!controller.snapshot().isComplete) errors += "level is not complete after the authored solution"
        return ValidationReport(errors.isEmpty(), errors)
    }

    fun structural(level: Level): List<String> {
        val errors = mutableListOf<String>()
        if (level.size !in 4..12) errors += "board size ${level.size} is outside 4..12"
        if (level.pairs.size < 2) errors += "needs at least 2 color pairs"
        if (level.pairs.size != level.solution.size) errors += "solution count does not match pairs"
        if (level.pairs.map { it.color }.distinct().size != level.pairs.size) errors += "duplicate colors"
        val endpoints = level.pairs.flatMap { listOf(it.a, it.b) }
        if (endpoints.distinct().size != endpoints.size) errors += "duplicate endpoints"
        if (endpoints.any { !it.inBoard(level.size) }) errors += "endpoint outside the board"
        val blocked = level.obstacles
            .filter {
                it.type == ObstacleType.BLOCK || it.type == ObstacleType.METAL ||
                    it.type == ObstacleType.CRYSTAL || it.type == ObstacleType.ROTATING
            }
            .map { it.position }
            .toSet()
        if (level.obstacles.map { it.position }.distinct().size != level.obstacles.size) {
            errors += "duplicate obstacles"
        }
        val seen = mutableSetOf<com.naampath.colorpath3d.model.GridPos>()
        level.pairs.forEachIndexed { index, pair ->
            val path = level.solution.getOrNull(index)
            if (path == null || path.size < 2) {
                errors += "pair ${pair.color} has no path"
                return@forEachIndexed
            }
            val ends = setOf(path.first(), path.last())
            if (pair.a !in ends || pair.b !in ends) errors += "pair ${pair.color} endpoints do not match the path"
            if (path.size != path.distinct().size) errors += "pair ${pair.color} reuses a cell"
            for (cell in path) {
                if (!cell.inBoard(level.size)) errors += "path leaves the board"
                if (cell in blocked) errors += "path enters a blocked cell"
                if (!seen.add(cell)) errors += "paths overlap at $cell"
            }
            for (step in 1 until path.size) {
                val from = path[step - 1]
                val to = path[step]
                val portal = level.obstacles.any {
                    it.position == from && it.portalLink == to &&
                        (it.type == ObstacleType.PORTAL || it.type == ObstacleType.TELEPORT)
                }
                if (!portal && Direction.unit(from, to) == null) {
                    errors += "path step is not orthogonal"
                }
            }
        }
        return errors
    }
}
