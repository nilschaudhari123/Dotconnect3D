package com.naampath.colorpath3d.board

import com.naampath.colorpath3d.model.Direction
import com.naampath.colorpath3d.model.GridPos
import com.naampath.colorpath3d.model.NodeSymbol
import com.naampath.colorpath3d.model.PathColor

enum class CellKind {
    OPEN,
    BLOCK,
    METAL,
    CRYSTAL,
    ROTATING,
    BARRIER,
    LOCKED,
    ICE,
    PORTAL,
    TELEPORT,
    ONE_WAY
}

data class GridCell(
    val pos: GridPos,
    val kind: CellKind = CellKind.OPEN,
    val endpoint: PathColor? = null,
    val symbol: NodeSymbol? = null,
    val oneWay: Direction? = null,
    val portalLink: GridPos? = null,
    val opensAfter: Int = 0
) {
    fun blocksTravel(completedConnections: Int): Boolean {
        return when (kind) {
            CellKind.BLOCK, CellKind.METAL, CellKind.CRYSTAL, CellKind.ROTATING -> true
            CellKind.BARRIER, CellKind.LOCKED -> completedConnections < opensAfter
            else -> false
        }
    }
}
