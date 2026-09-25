package com.naampath.colorpath3d.gameplay

import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.PathColor
import com.naampath.colorpath3d.model.RunStatus
import com.naampath.colorpath3d.path.PathConnection
import com.naampath.colorpath3d.path.RejectReason

data class GameState(
    val levelId: Int,
    val mode: GameMode,
    val locked: List<PathConnection>,
    val draft: List<GridPos>,
    val draftColor: PathColor?,
    val moves: Int,
    val moveLimit: Int?,
    val mistakes: Int,
    val hintsUsed: Int,
    val elapsedMs: Long,
    val timeLimitMs: Long?,
    val status: RunStatus,
    val lastReject: RejectReason?,
    val shake: Boolean,
    val hintCells: List<GridPos>,
    val hintFlash: GridPos?,
    val score: Int,
    val stars: Int,
    val paused: Boolean,
    val pairsTotal: Int
) {
    val pairsConnected: Int get() = locked.size
    val draftActive: Boolean get() = draftColor != null
    val isComplete: Boolean get() = status == RunStatus.COMPLETE

    fun toResult(completed: Boolean): LevelResult = LevelResult(
        levelId = levelId,
        mode = mode,
        score = score,
        stars = stars,
        moves = moves,
        elapsedMs = elapsedMs,
        mistakes = mistakes,
        hintsUsed = hintsUsed,
        completed = completed
    )
}

data class LevelResult(
    val levelId: Int,
    val mode: GameMode,
    val score: Int,
    val stars: Int,
    val moves: Int,
    val elapsedMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val completed: Boolean
)

enum class HintTier { NEXT_CELL, PARTIAL_PATH, COMPLETE_PAIR }

data class ModeRules(
    val timeLimitMs: Long?,
    val moveLimit: Int?,
    val failOnMistake: Boolean
)
