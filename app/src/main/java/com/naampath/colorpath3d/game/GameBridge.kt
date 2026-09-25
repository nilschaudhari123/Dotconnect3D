package com.naampath.colorpath3d.game

import android.os.Handler
import android.os.Looper
import com.naampath.colorpath3d.audio.HapticKind
import com.naampath.colorpath3d.audio.SoundId
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.gameplay.LevelResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameHostImpl @Inject constructor() : GameHost {
    private val main = Handler(Looper.getMainLooper())
    private val _splash = MutableStateFlow(false)
    val splash: StateFlow<Boolean> = _splash
    private val _hud = MutableStateFlow<GameState?>(null)
    val hud: StateFlow<GameState?> = _hud
    private val _results = MutableSharedFlow<LevelResult>(extraBufferCapacity = 4)
    val results: SharedFlow<LevelResult> = _results
    private val _sounds = MutableSharedFlow<SoundId>(extraBufferCapacity = 16)
    val sounds: SharedFlow<SoundId> = _sounds
    private val _haptics = MutableSharedFlow<HapticKind>(extraBufferCapacity = 16)
    val haptics: SharedFlow<HapticKind> = _haptics

    override fun onSplashFinished() {
        main.post { _splash.value = true }
    }

    override fun onHud(state: GameState) {
        main.post { _hud.value = state }
    }

    override fun onLevelFinished(result: LevelResult) {
        main.post { _results.tryEmit(result) }
    }

    override fun onSound(id: SoundId) {
        main.post { _sounds.tryEmit(id) }
    }

    override fun onHaptic(kind: HapticKind) {
        main.post { _haptics.tryEmit(kind) }
    }
}

@Singleton
class GameRuntime @Inject constructor() {
    var game: ColorPath3DGame? = null
        private set

    fun attach(game: ColorPath3DGame) {
        this.game = game
    }

    fun play(request: PlayRequest) {
        game?.play(request)
    }

    fun command(command: GameCommand) {
        game?.command(command)
    }

    fun presentation(presentation: com.naampath.colorpath3d.theme.Presentation) {
        game?.updatePresentation(presentation)
    }
}
