package com.naampath.colorpath3d.path

import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.PathColor

data class PathConnection(
    val color: PathColor,
    val cells: List<GridPos>
)
