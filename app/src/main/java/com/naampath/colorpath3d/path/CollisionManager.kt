package com.naampath.colorpath3d.path

import com.naampath.colorpath3d.model.GridPos

/** Occupancy and intersection tests shared by validation and the solver. */
class CollisionManager {
    fun lockedCells(paths: List<PathConnection>): Set<GridPos> =
        paths.flatMapTo(mutableSetOf()) { it.cells }

    fun crosses(cell: GridPos, occupied: Set<GridPos>): Boolean = cell in occupied

    fun selfOverlap(path: List<GridPos>): Boolean = path.size != path.toHashSet().size

    fun leavesBoard(cell: GridPos, size: Int): Boolean = !cell.inBoard(size)

    fun pathsIntersect(first: List<GridPos>, second: List<GridPos>): Boolean {
        val set = first.toHashSet()
        return second.any { it in set }
    }
}
