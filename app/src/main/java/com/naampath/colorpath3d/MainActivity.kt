package com.naampath.colorpath3d

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.naampath.colorpath3d.ads.AdManager
import com.naampath.colorpath3d.audio.MusicManager
import com.naampath.colorpath3d.game.ColorPathFragment
import com.naampath.colorpath3d.ui.SessionFlags
import com.naampath.colorpath3d.ui.nav.AppNav
import com.naampath.colorpath3d.ui.theme.ColorPathTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var music: MusicManager
    @Inject lateinit var ads: AdManager
    private var leftForBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        if (supportFragmentManager.findFragmentByTag(GDX) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.gdx_host, ColorPathFragment(), GDX)
                .commitNow()
        }
        val host = findViewById<View>(R.id.gdx_host)
        findViewById<ComposeView>(R.id.compose_root).setContent {
            ColorPathTheme {
                AppNav { visible -> host.visibility = if (visible) View.VISIBLE else View.GONE }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        music.start()
    }

    override fun onStop() {
        leftForBackground = true
        music.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (leftForBackground && SessionFlags.allowAppOpen && !SessionFlags.gameplayVisible) {
            ads.maybeAppOpen(this, true)
        }
        leftForBackground = false
    }

    companion object {
        private const val GDX = "gdx"
    }
}
