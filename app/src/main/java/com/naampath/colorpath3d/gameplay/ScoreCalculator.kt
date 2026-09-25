package com.naampath.colorpath3d.gameplay

import com.naampath.colorpath3d.tuning.DifficultyTuning

object ScoreCalculator {
    fun score(
        elapsedMs: Long,
        moves: Int,
        mistakes: Int,
        hints: Int,
        parMoves: Int,
        parTimeSec: Int,
        pathCells: Int,
        optimalCells: Int,
        tuning: DifficultyTuning = DifficultyTuning()
    ): Int {
        var value = 1000
        val seconds = (elapsedMs / 1000L).toInt()
        value -= seconds * tuning.timePenaltyPerSec
        value -= (moves - parMoves).coerceAtLeast(0) * tuning.movePenalty
        value -= mistakes * tuning.mistakePenalty
        value -= hints * tuning.hintPenalty
        if (mistakes == 0) value += tuning.noMistakeBonus
        if (hints == 0 && mistakes == 0 && moves <= parMoves) value += tuning.perfectBonus
        if (seconds <= parTimeSec) value += tuning.fastBonus
        if (pathCells <= optimalCells) value += tuning.efficiencyBonus
        return value.coerceAtLeast(0)
    }

    fun stars(
        moves: Int,
        mistakes: Int,
        hints: Int,
        elapsedMs: Long,
        parMoves: Int,
        parTimeSec: Int
    ): Int {
        var earned = 1
        if (mistakes == 0 && hints == 0) earned = 2
        val seconds = (elapsedMs / 1000L).toInt()
        if (earned == 2 && moves <= parMoves && seconds <= parTimeSec) earned = 3
        return earned
    }
}
