package com.naampath.colorpath3d.path

import com.naampath.colorpath3d.board.Board3D
import com.naampath.colorpath3d.board.CellKind
import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.model.PathColor

enum class RejectReason {
    OUT_OF_BOARD,
    DIAGONAL,
    NOT_ADJACENT,
    REVISIT,
    CROSSES_PATH,
    BLOCKED,
    WRONG_START,
    WRONG_END,
    ONE_WAY,
    BARRIER,
    LOCKED_PATH,
    FOREIGN_ENDPOINT,
    INCOMPLETE,
    NOT_PLAYING
}

data class RuleResult(val ok: Boolean, val reason: RejectReason? = null) {
    companion object {
        val OK = RuleResult(true)
        fun reject(reason: RejectReason) = RuleResult(false, reason)
    }
}

sealed class StepOutcome {
    data class Extend(val cells: List<GridPos>) : StepOutcome()
    data class Trim(val index: Int) : StepOutcome()
    data class Reject(val reason: RejectReason) : StepOutcome()
}

class PathValidator(
    private val board: Board3D,
    private val collision: CollisionManager
) {
    fun isWellFormed(
        level: Level,
        color: PathColor,
        path: List<GridPos>,
        locked: Set<GridPos>,
        completedBefore: Int
    ): RuleResult {
        if (path.size < 2) return RuleResult.reject(RejectReason.INCOMPLETE)
        for (cell in path) {
            if (collision.leavesBoard(cell, board.size)) return RuleResult.reject(RejectReason.OUT_OF_BOARD)
        }
        val pair = level.pair(color) ?: return RuleResult.reject(RejectReason.WRONG_START)
        if (path.first() != pair.a && path.first() != pair.b) {
            return RuleResult.reject(RejectReason.WRONG_START)
        }
        val expectedEnd = if (path.first() == pair.a) pair.b else pair.a
        if (path.last() != expectedEnd) return RuleResult.reject(RejectReason.WRONG_END)
        if (collision.selfOverlap(path)) return RuleResult.reject(RejectReason.REVISIT)
        for (cell in path) {
            if (collision.crosses(cell, locked)) return RuleResult.reject(RejectReason.CROSSES_PATH)
        }
        for (index in 1 until path.size) {
            val from = path[index - 1]
            val to = path[index]
            val portalJump = isPortalJump(from, to)
            if (!portalJump) {
                if (from.x != to.x && from.y != to.y) return RuleResult.reject(RejectReason.DIAGONAL)
                if (Direction.unit(from, to) == null) return RuleResult.reject(RejectReason.NOT_ADJACENT)
            }
            occupy(level, color, to, Direction.unit(from, to), locked, completedBefore, path.take(index).toSet())
                ?.let { return RuleResult.reject(it) }
        }
        return RuleResult.OK
    }

    fun resolveStep(
        level: Level,
        color: PathColor,
        from: GridPos,
        to: GridPos,
        draft: List<GridPos>,
        locked: Set<GridPos>,
        completedBefore: Int
    ): StepOutcome {
        val existing = draft.indexOf(to)
        if (existing >= 0) return StepOutcome.Trim(existing)
        if (!to.inBoard(board.size)) return StepOutcome.Reject(RejectReason.OUT_OF_BOARD)
        if (from.x != to.x && from.y != to.y) return StepOutcome.Reject(RejectReason.DIAGONAL)
        val direction = Direction.unit(from, to) ?: return StepOutcome.Reject(RejectReason.NOT_ADJACENT)
        val appended = mutableListOf<GridPos>()
        var cursor = from
        var next = to
        var guard = 0
        while (guard++ < board.size * 2) {
            val reason = occupy(
                level, color, next, Direction.unit(cursor, next), locked, completedBefore,
                draft.toSet() + appended
            )
            if (reason != null) {
                return if (appended.isEmpty()) StepOutcome.Reject(reason) else StepOutcome.Extend(appended)
            }
            appended += next
            val cell = board.cellAt(next)
            val slide = cell?.kind == CellKind.ICE
            cursor = next
            if (slide) {
                val further = cursor.step(direction)
                if (!further.inBoard(board.size)) break
                next = further
                continue
            }
            if (cell?.kind == CellKind.PORTAL || cell?.kind == CellKind.TELEPORT) {
                val link = cell.portalLink
                if (link != null && link !in draft && link !in appended) {
                    val linkReason = occupy(
                        level, color, link, null, locked, completedBefore, draft.toSet() + appended
                    )
                    if (linkReason == null) appended += link
                }
            }
            break
        }
        return StepOutcome.Extend(appended)
    }

    private fun isPortalJump(from: GridPos, to: GridPos): Boolean {
        val cell = board.cellAt(from) ?: return false
        if (cell.kind != CellKind.PORTAL && cell.kind != CellKind.TELEPORT) return false
        return cell.portalLink == to
    }

    private fun occupy(
        level: Level,
        color: PathColor,
        cellPos: GridPos,
        direction: Direction?,
        locked: Set<GridPos>,
        completedBefore: Int,
        draftCells: Set<GridPos>
    ): RejectReason? {
        if (!cellPos.inBoard(board.size)) return RejectReason.OUT_OF_BOARD
        if (cellPos in draftCells) return RejectReason.REVISIT
        if (collision.crosses(cellPos, locked)) return RejectReason.CROSSES_PATH
        val endpoint = level.endpointAt(cellPos)
        if (endpoint != null && endpoint.color != color) return RejectReason.FOREIGN_ENDPOINT
        val cell = board.cellAt(cellPos) ?: return RejectReason.OUT_OF_BOARD
        if (cell.kind == CellKind.BARRIER || cell.kind == CellKind.LOCKED) {
            if (cell.blocksTravel(completedBefore)) return RejectReason.BARRIER
        } else if (cell.blocksTravel(completedBefore)) {
            return RejectReason.BLOCKED
        }
        if (cell.oneWay != null && direction != null && direction != cell.oneWay) {
            return RejectReason.ONE_WAY
        }
        return null
    }
}
