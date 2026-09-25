package com.naampath.colorpath3d.board

import com.naampath.colorpath3d.model.Level

/** Builds the playable board model for a level. Geometry stays size-driven. */
object BoardGenerator {
    fun fromLevel(level: Level): Board3D = Board3D.from(level)

    fun span(size: Int, cellSize: Float = Board3D.CELL_SIZE): Float = size * cellSize
}
