package com.naampath.colorpath3d.gameplay

import com.naampath.colorpath3d.model.GridPos

/** Translates pointer phases into path commands. Picking stays in the renderer. */
class InputController(private val game: GameController) {
    fun down(cell: GridPos?) = game.pointerDown(cell)
    fun move(cell: GridPos?) = game.pointerMove(cell)
    fun up(cell: GridPos?) = game.pointerUp(cell)
    fun cancel() = game.cancelDraft()
    fun undo() = game.undo()
    fun restart() = game.restart()
}
