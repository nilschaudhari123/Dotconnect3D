package com.naampath.colorpath3d.gameplay

import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.RunStatus
import com.naampath.colorpath3d.path.CollisionManager
import com.naampath.colorpath3d.path.PathConnection
import com.naampath.colorpath3d.path.PathManager
import com.naampath.colorpath3d.path.PathValidator
import com.naampath.colorpath3d.path.RejectReason
import com.naampath.colorpath3d.path.StepOutcome
import com.naampath.colorpath3d.tuning.DifficultyTuning

class GameController(
    val level: Level,
    val mode: GameMode,
    private val tuning: DifficultyTuning = DifficultyTuning()
) {
    val board: Board3D = Board3D.from(level)
    private val collision = CollisionManager()
    private val validator = PathValidator(board, collision)
    private val hints = HintEngine()
    private val paths = PathManager()
    private val rules: ModeRules = rulesFor(mode, level)

    private var moves = 0
    private var mistakes = 0
    private var hintsUsed = 0
    private var elapsedMs = 0L
    private var bonusTimeMs = 0L
    private var status = RunStatus.PLAYING
    private var lastReject: RejectReason? = null
    private var shake = false
    private var hintCells: List<GridPos> = emptyList()
    private var hintFlash: GridPos? = null
    private var score = 0
    private var stars = 0
    private var paused = false

    fun snapshot(): GameState = GameState(
        levelId = level.id,
        mode = mode,
        locked = paths.lockedPaths(),
        draft = paths.draft(),
        draftColor = paths.draftColor,
        moves = moves,
        moveLimit = rules.moveLimit,
        mistakes = mistakes,
        hintsUsed = hintsUsed,
        elapsedMs = elapsedMs,
        timeLimitMs = rules.timeLimitMs?.plus(bonusTimeMs),
        status = status,
        lastReject = lastReject,
        shake = shake,
        hintCells = hintCells,
        hintFlash = hintFlash,
        score = score,
        stars = stars,
        paused = paused,
        pairsTotal = level.pairs.size
    )

    fun setPaused(value: Boolean) {
        if (status != RunStatus.PLAYING) return
        paused = value
    }

    fun advance(deltaMs: Long) {
        if (paused || status != RunStatus.PLAYING) return
        elapsedMs += deltaMs.coerceAtLeast(0L)
        val limit = rules.timeLimitMs?.plus(bonusTimeMs)
        if (limit != null && elapsedMs >= limit) {
            status = RunStatus.TIME_UP
            paths.cancelDraft(restoreSuspended = true)
        }
    }

    fun extendTime(extraMs: Long) {
        if (status != RunStatus.TIME_UP && status != RunStatus.PLAYING) return
        bonusTimeMs += extraMs
        if (status == RunStatus.TIME_UP) status = RunStatus.PLAYING
    }

    fun pointerDown(cell: GridPos?): RejectReason? {
        if (status != RunStatus.PLAYING || paused) return RejectReason.NOT_PLAYING
        if (cell == null) return null
        val pair = level.endpointAt(cell) ?: return null
        val started = paths.start(pair.color, cell, level.supportsEditing)
        if (!started) {
            lastReject = RejectReason.LOCKED_PATH
            shake = true
            return RejectReason.LOCKED_PATH
        }
        hintFlash = null
        lastReject = null
        shake = false
        return null
    }

    fun pointerMove(cell: GridPos?): RejectReason? {
        if (status != RunStatus.PLAYING || paused) return RejectReason.NOT_PLAYING
        val color = paths.draftColor ?: return null
        if (cell == null) return null
        val draft = paths.draft()
        if (draft.isEmpty()) return null
        if (cell == draft.last()) return null
        if (cell in draft) {
            paths.trimTo(draft.indexOf(cell))
            return null
        }
        val direction = Direction.axis(draft.last(), cell) ?: return null
        var guard = 0
        while (paths.draft().last() != cell && guard++ < level.size * 2) {
            val current = paths.draft()
            val next = current.last().step(direction)
            val outcome = validator.resolveStep(
                level, color, current.last(), next, current, paths.occupied(), paths.lockedPaths().size
            )
            when (outcome) {
                is StepOutcome.Trim -> paths.trimTo(outcome.index)
                is StepOutcome.Extend -> {
                    paths.append(outcome.cells)
                    if (outcome.cells.isEmpty()) break
                }
                is StepOutcome.Reject -> {
                    lastReject = outcome.reason
                    shake = true
                    return outcome.reason
                }
            }
            val updated = paths.draft()
            if (cell in updated) break
            if (updated.last() == current.last()) break
        }
        return null
    }

    fun pointerUp(cell: GridPos?): RejectReason? {
        if (status != RunStatus.PLAYING || paused) return RejectReason.NOT_PLAYING
        val color = paths.draftColor ?: return null
        val draft = paths.draft()
        if (draft.isEmpty()) return null
        val foreign = cell?.let { level.endpointAt(it) }?.takeIf { it.color != color }
        if (foreign != null) {
            return failAttempt(RejectReason.WRONG_END)
        }
        val end = level.otherEnd(color, draft.first())
        if (end != null && draft.last() == end && draft.size >= 2) {
            val check = validator.isWellFormed(level, color, draft, paths.occupied(), paths.lockedPaths().size)
            if (check.ok) {
                paths.lock()
                moves += 1
                lastReject = null
                shake = false
                hintCells = emptyList()
                hintFlash = null
                if (paths.lockedPaths().size == level.pairs.size) {
                    finish()
                } else {
                    checkMoveLimit()
                }
                return null
            }
            return failAttempt(check.reason ?: RejectReason.INCOMPLETE)
        }
        if (draft.size < 2) {
            paths.cancelDraft(restoreSuspended = true)
            return null
        }
        return failAttempt(RejectReason.INCOMPLETE)
    }

    fun cancelDraft() {
        paths.cancelDraft(restoreSuspended = true)
        shake = false
    }

    fun undo(): Boolean {
        if (status != RunStatus.PLAYING) return false
        if (paths.draftColor != null) {
            paths.cancelDraft(restoreSuspended = true)
            return true
        }
        val removed = paths.removeLast() ?: return false
        if (moves > 0) moves -= 1
        hintCells = emptyList()
        hintFlash = null
        return removed.cells.isNotEmpty()
    }

    fun restart() {
        paths.clear()
        moves = 0
        mistakes = 0
        hintsUsed = 0
        elapsedMs = 0L
        bonusTimeMs = 0L
        status = RunStatus.PLAYING
        lastReject = null
        shake = false
        hintCells = emptyList()
        hintFlash = null
        score = 0
        stars = 0
        paused = false
    }

    fun hint(tier: HintTier): Boolean {
        if (status != RunStatus.PLAYING) return false
        val lockedColors = paths.lockedPaths().map { it.color }.toSet()
        if (lockedColors.size >= level.pairs.size) return false
        when (tier) {
            HintTier.NEXT_CELL -> {
                hintFlash = hints.nextCell(level, lockedColors, paths.draft(), paths.draftColor)
                hintCells = listOfNotNull(hintFlash)
            }
            HintTier.PARTIAL_PATH -> {
                paths.cancelDraft(restoreSuspended = true)
                hintCells = hints.partial(level, paths.lockedPaths().map { it.color }.toSet(), null)
                hintFlash = hintCells.lastOrNull()
            }
            HintTier.COMPLETE_PAIR -> {
                paths.cancelDraft(restoreSuspended = true)
                val colors = paths.lockedPaths().map { it.color }.toSet()
                val path = hints.full(level, colors, null)
                if (path.size < 2) return false
                val color = level.endpointAt(path.first())?.color ?: return false
                val check = validator.isWellFormed(level, color, path, paths.occupied(), paths.lockedPaths().size)
                if (!check.ok) return false
                paths.addLocked(PathConnection(color, path))
                hintCells = emptyList()
                hintFlash = null
                if (paths.lockedPaths().size == level.pairs.size) finish()
            }
        }
        hintsUsed += 1
        return true
    }

    /** Replays an authored solution path, skipping cells already filled by ice slides. */
    fun commitAuthoredPath(path: List<GridPos>): Boolean {
        if (path.isEmpty()) return false
        pointerDown(path.first())
        for (cell in path.drop(1)) {
            val draft = paths.draft()
            if (draft.isEmpty()) return false
            if (cell in draft) continue
            val reason = pointerMove(cell)
            if (reason != null || cell !in paths.draft()) return false
        }
        val reason = pointerUp(path.last())
        return reason == null && paths.lockedPaths().any { it.cells.first() == path.first() || it.cells.last() == path.last() }
    }

    private fun failAttempt(reason: RejectReason): RejectReason {
        mistakes += 1
        moves += 1
        lastReject = reason
        shake = true
        paths.cancelDraft(restoreSuspended = true)
        if (rules.failOnMistake) status = RunStatus.FAILED
        else checkMoveLimit()
        return reason
    }

    private fun checkMoveLimit() {
        val limit = rules.moveLimit ?: return
        if (status == RunStatus.PLAYING && moves >= limit && paths.lockedPaths().size < level.pairs.size) {
            status = RunStatus.MOVES_EXHAUSTED
        }
    }

    private fun finish() {
        val usedCells = paths.lockedPaths().sumOf { it.cells.size }
        val optimal = level.solution.sumOf { it.size }
        score = ScoreCalculator.score(
            elapsedMs, moves, mistakes, hintsUsed, level.parMoves, level.parTimeSec, usedCells, optimal, tuning
        )
        stars = ScoreCalculator.stars(moves, mistakes, hintsUsed, elapsedMs, level.parMoves, level.parTimeSec)
        status = RunStatus.COMPLETE
        paused = false
    }

    companion object {
        fun rulesFor(mode: GameMode, level: Level): ModeRules = when (mode) {
            GameMode.TIMED -> ModeRules(level.parTimeSec * 1000L, null, false)
            GameMode.MOVES -> ModeRules(null, level.pairs.size + 3, false)
            GameMode.PERFECT -> ModeRules(null, null, true)
            else -> ModeRules(null, null, false)
        }
    }
}
