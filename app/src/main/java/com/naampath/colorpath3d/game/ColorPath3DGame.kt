package com.naampath.colorpath3d.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.naampath.colorpath3d.theme.Presentation

class ColorPath3DGame(val host: GameHost) : Game() {
    @Volatile var ready: Boolean = false
        private set
    private var pending: PlayRequest? = null

    override fun create() {
        ready = true
        val queued = pending
        if (queued != null) {
            playNow(queued)
        } else {
            setScreen(SplashGdxScreen(this))
        }
    }

    fun play(request: PlayRequest) {
        if (!ready) {
            pending = request
            return
        }
        Gdx.app.postRunnable { playNow(request) }
    }

    fun command(command: GameCommand) {
        if (!ready) return
        Gdx.app.postRunnable {
            (screen as? GameScreen)?.onCommand(command)
        }
    }

    fun updatePresentation(presentation: Presentation) {
        if (!ready) return
        Gdx.app.postRunnable {
            (screen as? GameScreen)?.applyPresentation(presentation)
        }
    }

    private fun playNow(request: PlayRequest) {
        pending = null
        setScreen(GameScreen(this, request))
    }
}
