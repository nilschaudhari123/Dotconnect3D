package com.naampath.colorpath3d.path

import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.PathColor

/** Mutable path session used by the controller. Locked paths stay immutable snapshots. */
class PathManager {
    private val locked = mutableListOf<PathConnection>()
    private val draftCells = mutableListOf<GridPos>()
    var draftColor: PathColor? = null
        private set
    var suspended: PathConnection? = null
        private set

    fun lockedPaths(): List<PathConnection> = locked.toList()

    fun draft(): List<GridPos> = draftCells.toList()

    fun occupied(): Set<GridPos> = locked.flatMapTo(mutableSetOf()) { it.cells }

    fun start(color: PathColor, origin: GridPos, replaceExisting: Boolean): Boolean {
        val existing = locked.indexOfFirst { it.color == color }
        if (existing >= 0) {
            if (!replaceExisting) return false
            suspended = locked.removeAt(existing)
        } else {
            suspended = null
        }
        draftColor = color
        draftCells.clear()
        draftCells += origin
        return true
    }

    fun trimTo(index: Int) {
        while (draftCells.lastIndex > index) draftCells.removeAt(draftCells.lastIndex)
    }

    fun append(cells: List<GridPos>) {
        draftCells.addAll(cells)
    }

    fun lock(): PathConnection? {
        val color = draftColor ?: return null
        if (draftCells.size < 2) return null
        val connection = PathConnection(color, draftCells.toList())
        locked += connection
        suspended = null
        clearDraft()
        return connection
    }

    fun cancelDraft(restoreSuspended: Boolean) {
        if (restoreSuspended) {
            suspended?.let { locked += it }
        }
        suspended = null
        clearDraft()
    }

    fun removeLast(): PathConnection? {
        if (locked.isEmpty()) return null
        return locked.removeAt(locked.lastIndex)
    }

    fun removeColor(color: PathColor): PathConnection? {
        val index = locked.indexOfFirst { it.color == color }
        if (index < 0) return null
        return locked.removeAt(index)
    }

    fun addLocked(connection: PathConnection) {
        removeColor(connection.color)
        locked += connection
        suspended = null
        clearDraft()
    }

    fun clear() {
        locked.clear()
        suspended = null
        clearDraft()
    }

    private fun clearDraft() {
        draftColor = null
        draftCells.clear()
    }
}
