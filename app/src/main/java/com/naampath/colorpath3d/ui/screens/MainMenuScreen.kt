package com.naampath.colorpath3d.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.naampath.colorpath3d.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naampath.colorpath3d.ui.components.NeonButton
import com.naampath.colorpath3d.ui.theme.Amber
import com.naampath.colorpath3d.ui.theme.Cyan
import com.naampath.colorpath3d.ui.theme.Magenta

@Composable
fun MainMenuScreen(
    coins: Int,
    endlessEnabled: Boolean,
    dailyEnabled: Boolean,
    showBanner: Boolean,
    onPlay: () -> Unit,
    onDaily: () -> Unit,
    onLevels: () -> Unit,
    onEndless: () -> Unit,
    onAchievements: () -> Unit,
    onLeaderboard: () -> Unit,
    onSettings: () -> Unit,
    onShop: () -> Unit,
    onStats: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("COLOR PATH 3D", color = Cyan)
        Text("CONNECT  •  THINK  •  MASTER", color = com.naampath.colorpath3d.ui.theme.Mist)
        Text("Coins $coins", color = Amber)
        Spacer(Modifier.height(8.dp))
        NeonButton("PLAY", accent = Cyan, onClick = onPlay)
        if (dailyEnabled) NeonButton("DAILY CHALLENGE", accent = Magenta, onClick = onDaily)
        NeonButton("LEVELS", onClick = onLevels)
        if (endlessEnabled) NeonButton("ENDLESS", onClick = onEndless)
        NeonButton("ACHIEVEMENTS", onClick = onAchievements)
        NeonButton("LEADERBOARD", onClick = onLeaderboard)
        NeonButton("STATISTICS", onClick = onStats)
        NeonButton("SETTINGS", onClick = onSettings)
        NeonButton("SHOP", accent = Amber, onClick = onShop)
        if (showBanner) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = context.getString(R.string.ad_banner_unit)
                        loadAd(AdRequest.Builder().build())
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
