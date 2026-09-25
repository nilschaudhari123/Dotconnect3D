package com.naampath.colorpath3d.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.naampath.colorpath3d.R
import com.naampath.colorpath3d.tuning.DifficultyTuning

class CrashlyticsManager {
    fun install() {
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true) }
    }

    fun log(message: String) {
        runCatching { FirebaseCrashlytics.getInstance().log(message) }
    }

    fun record(error: Throwable) {
        runCatching { FirebaseCrashlytics.getInstance().recordException(error) }
    }
}

class AnalyticsManager(context: Context, private val crashlytics: CrashlyticsManager) {
    private val analytics = runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()

    fun event(name: String, params: Map<String, String> = emptyMap()) {
        val bundle = Bundle()
        params.forEach { (key, value) -> bundle.putString(key.take(40), value.take(100)) }
        runCatching { analytics?.logEvent(name.take(40), bundle) }
            .onFailure { crashlytics.record(it) }
    }
}

class RemoteConfigManager(private val crashlytics: CrashlyticsManager) {
    private val remote = runCatching { FirebaseRemoteConfig.getInstance() }.getOrNull()
    @Volatile private var tuning = DifficultyTuning()

    fun start() {
        val config = remote ?: return
        runCatching {
            config.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(3600).build()
            )
            config.setDefaultsAsync(R.xml.remote_config_defaults)
            config.fetchAndActivate().addOnCompleteListener {
                tuning = read()
            }
            tuning = read()
        }.onFailure { crashlytics.record(it) }
    }

    fun tuning(): DifficultyTuning = tuning

    private fun read(): DifficultyTuning {
        val config = remote ?: return DifficultyTuning()
        return DifficultyTuning.from { key, default ->
            runCatching { config.getString(key).ifBlank { default } }.getOrDefault(default)
        }
    }
}
