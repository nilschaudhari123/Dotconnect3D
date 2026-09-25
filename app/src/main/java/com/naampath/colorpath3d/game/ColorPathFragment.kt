package com.naampath.colorpath3d.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ColorPathFragment : AndroidFragmentApplication() {
    @Inject lateinit var runtime: GameRuntime
    @Inject lateinit var host: GameHostImpl

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val config = AndroidApplicationConfiguration().apply {
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            numSamples = 0
        }
        val game = ColorPath3DGame(host)
        runtime.attach(game)
        return initializeForView(game, config)
    }
}
