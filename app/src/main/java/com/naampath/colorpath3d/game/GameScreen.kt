package com.naampath.colorpath3d.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.naampath.colorpath3d.audio.HapticKind
import com.naampath.colorpath3d.audio.SoundId
import com.naampath.colorpath3d.gameplay.GameController
import com.naampath.colorpath3d.gameplay.HintTier
import com.naampath.colorpath3d.gameplay.InputController
import com.naampath.colorpath3d.model.RunStatus
import com.naampath.colorpath3d.path.RejectReason
import com.naampath.colorpath3d.theme.Presentation

class GameScreen(
    private val game: ColorPath3DGame,
    private val request: PlayRequest
) : ScreenAdapter() {
    private val controller = GameController(request.level, request.mode)
    private val input = InputController(controller)
    private var renderer: GameRenderer? = null
    private var presentation: Presentation = request.presentation
    private var reported = false
    private var time = 0f
    private var hudDirty = true
    private var drawSoundGate = 0f

    override fun show() {
        renderer = GameRenderer(request.level, presentation)
        pushHud()
    }

    fun applyPresentation(value: Presentation) {
        presentation = value
        renderer?.apply(value, request.level)
    }

    fun onCommand(command: GameCommand) {
        when (command) {
            GameCommand.Undo -> {
                input.undo()
                hudDirty = true
            }
            GameCommand.Restart -> {
                input.restart()
                reported = false
                hudDirty = true
            }
            GameCommand.Pause -> controller.setPaused(true)
            GameCommand.Resume -> controller.setPaused(false)
            is GameCommand.Hint -> {
                if (controller.hint(command.tier)) {
                    game.host.onSound(SoundId.HINT)
                    game.host.onHaptic(HapticKind.SELECT)
                }
                hudDirty = true
            }
            is GameCommand.ExtendTime -> controller.extendTime(command.extraMs)
            is GameCommand.Pointer -> {
                if (command.phase == 4) renderer?.rig?.zoomBy(command.x.coerceIn(0.85f, 1.15f))
                else handlePointer(command)
            }
        }
        pushHud()
    }

    private fun handlePointer(pointer: GameCommand.Pointer) {
        val view = renderer ?: return
        val sx = pointer.x / pointer.viewW.coerceAtLeast(1f) * Gdx.graphics.width
        val sy = pointer.y / pointer.viewH.coerceAtLeast(1f) * Gdx.graphics.height
        val cell = view.rig.pick(sx, sy, request.level.size)
        val before = controller.snapshot().draft.size
        val reason = when (pointer.phase) {
            0 -> {
                val result = input.down(cell)
                if (result == null && cell != null && controller.snapshot().draftActive) {
                    game.host.onHaptic(HapticKind.SELECT)
                }
                result
            }
            1 -> input.move(cell)
            2 -> input.up(cell)
            else -> {
                input.cancel()
                null
            }
        }
        val after = controller.snapshot()
        if (pointer.phase == 1 && after.draft.size > before) {
            drawSoundGate += 1f
            if (drawSoundGate >= 1f) {
                game.host.onSound(SoundId.DRAW)
                game.host.onHaptic(HapticKind.MOVE)
                drawSoundGate = 0f
            }
        }
        if (reason == RejectReason.WRONG_END || reason == RejectReason.CROSSES_PATH || reason == RejectReason.BLOCKED ||
            reason == RejectReason.INCOMPLETE || reason == RejectReason.BARRIER || reason == RejectReason.ONE_WAY
        ) {
            game.host.onSound(SoundId.INVALID)
            game.host.onHaptic(HapticKind.INVALID)
        }
        if (reason == null && pointer.phase == 2 && after.locked.size > before.coerceAtMost(after.pairsTotal)) {
            game.host.onSound(SoundId.CONNECT)
            game.host.onHaptic(HapticKind.CONNECT)
        }
        hudDirty = true
    }

    override fun render(delta: Float) {
        time += delta
        controller.advance((delta * 1000f).toLong())
        val state = controller.snapshot()
        if (state.status != RunStatus.PLAYING && !reported) {
            reported = true
            val result = state.toResult(state.isComplete)
            if (state.isComplete) {
                game.host.onSound(SoundId.COMPLETE)
                game.host.onHaptic(HapticKind.COMPLETE)
            }
            game.host.onLevelFinished(result)
        }
        renderer?.render(request.level, state, time, delta)
        if (hudDirty || state.status == RunStatus.PLAYING) {
            pushHud()
            hudDirty = false
        }
    }

    override fun resize(width: Int, height: Int) {
        renderer?.resize(width, height)
    }

    override fun dispose() {
        renderer?.dispose()
        renderer = null
    }

    private fun pushHud() {
        game.host.onHud(controller.snapshot())
    }

    fun hintTier(tier: HintTier) = onCommand(GameCommand.Hint(tier))
}
