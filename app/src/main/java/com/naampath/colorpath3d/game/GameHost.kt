package com.naampath.colorpath3d.game

import com.naampath.colorpath3d.audio.HapticKind
import com.naampath.colorpath3d.audio.SoundId
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.gameplay.HintTier
import com.naampath.colorpath3d.gameplay.LevelResult
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.Level
import com.naampath.colorpath3d.theme.Presentation

interface GameHost {
    fun onSplashFinished()
    fun onHud(state: GameState)
    fun onLevelFinished(result: LevelResult)
    fun onSound(id: SoundId)
    fun onHaptic(kind: HapticKind)
}

data class PlayRequest(
    val level: Level,
    val mode: GameMode,
    val tutorial: Boolean,
    val presentation: Presentation
)

sealed class GameCommand {
    data object Undo : GameCommand()
    data object Restart : GameCommand()
    data object Pause : GameCommand()
    data object Resume : GameCommand()
    data class Hint(val tier: HintTier) : GameCommand()
    data class ExtendTime(val extraMs: Long) : GameCommand()
    data class Pointer(
        val phase: Int,
        val x: Float,
        val y: Float,
        val viewW: Float,
        val viewH: Float
    ) : GameCommand()
}
