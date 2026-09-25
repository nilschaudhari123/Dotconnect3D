package com.naampath.colorpath3d

import android.app.Application
import com.google.firebase.FirebaseApp
import com.naampath.colorpath3d.ads.AdManager
import com.naampath.colorpath3d.billing.BillingRepository
import com.naampath.colorpath3d.data.ProgressRepository
import com.naampath.colorpath3d.firebase.CrashlyticsManager
import com.naampath.colorpath3d.firebase.RemoteConfigManager
import com.naampath.colorpath3d.leaderboard.LeaderboardManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ColorPathApp : Application() {
    @Inject lateinit var crashlytics: CrashlyticsManager
    @Inject lateinit var remoteConfig: RemoteConfigManager
    @Inject lateinit var ads: AdManager
    @Inject lateinit var billing: BillingRepository
    @Inject lateinit var progress: ProgressRepository
    @Inject lateinit var leaderboards: LeaderboardManager

    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseApp.initializeApp(this) }
        crashlytics.install()
        remoteConfig.start()
        leaderboards.prepare()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { progress.load() }
            runCatching { ads.initialize() }
            runCatching { billing.connect() }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            crashlytics.log("low_memory")
        }
    }
}
