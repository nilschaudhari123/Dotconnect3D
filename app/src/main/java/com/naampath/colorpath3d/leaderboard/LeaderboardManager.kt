package com.naampath.colorpath3d.leaderboard

import android.app.Activity
import android.content.Context
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.naampath.colorpath3d.R

class LeaderboardManager(private val context: Context) {
    private var started = false

    fun prepare() {
        val appId = context.getString(R.string.play_games_app_id)
        if (appId.isBlank() || appId == "000000000000") return
        runCatching {
            PlayGamesSdk.initialize(context)
            started = true
        }
    }

    fun submit(activity: Activity, boardRes: Int, score: Long) {
        if (!started) return
        runCatching {
            PlayGames.getLeaderboardsClient(activity).submitScore(context.getString(boardRes), score)
        }
    }

    fun show(activity: Activity, boardRes: Int, onUnavailable: () -> Unit) {
        if (!started) {
            onUnavailable()
            return
        }
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(context.getString(boardRes))
            .addOnSuccessListener { activity.startActivity(it) }
            .addOnFailureListener { onUnavailable() }
    }
}
