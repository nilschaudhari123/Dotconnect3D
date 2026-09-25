package com.naampath.colorpath3d.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.naampath.colorpath3d.model.Quality
import com.naampath.colorpath3d.progress.WalletRecord
import com.naampath.colorpath3d.security.ProgressToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("colorpath_settings")

data class SettingsState(
    val music: Boolean = true,
    val sound: Boolean = true,
    val haptic: Boolean = true,
    val quality: Quality = Quality.AUTO,
    val colorBlind: Boolean = false,
    val highContrast: Boolean = false,
    val reducedEffects: Boolean = false,
    val cameraRotation: Boolean = true,
    val largeText: Boolean = false,
    val themeId: String = "neon",
    val tutorialDone: Boolean = false
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            music = prefs[Keys.MUSIC] ?: true,
            sound = prefs[Keys.SOUND] ?: true,
            haptic = prefs[Keys.HAPTIC] ?: true,
            quality = prefs[Keys.QUALITY]?.let { runCatching { Quality.valueOf(it) }.getOrNull() } ?: Quality.AUTO,
            colorBlind = prefs[Keys.COLOR_BLIND] ?: false,
            highContrast = prefs[Keys.CONTRAST] ?: false,
            reducedEffects = prefs[Keys.REDUCED] ?: false,
            cameraRotation = prefs[Keys.ROTATE] ?: true,
            largeText = prefs[Keys.LARGE_TEXT] ?: false,
            themeId = prefs[Keys.THEME] ?: "neon",
            tutorialDone = prefs[Keys.TUTORIAL] ?: false
        )
    }

    suspend fun update(transform: (SettingsState) -> SettingsState) {
        context.dataStore.edit { prefs ->
            val current = SettingsState(
                music = prefs[Keys.MUSIC] ?: true,
                sound = prefs[Keys.SOUND] ?: true,
                haptic = prefs[Keys.HAPTIC] ?: true,
                quality = prefs[Keys.QUALITY]?.let { runCatching { Quality.valueOf(it) }.getOrNull() } ?: Quality.AUTO,
                colorBlind = prefs[Keys.COLOR_BLIND] ?: false,
                highContrast = prefs[Keys.CONTRAST] ?: false,
                reducedEffects = prefs[Keys.REDUCED] ?: false,
                cameraRotation = prefs[Keys.ROTATE] ?: true,
                largeText = prefs[Keys.LARGE_TEXT] ?: false,
                themeId = prefs[Keys.THEME] ?: "neon",
                tutorialDone = prefs[Keys.TUTORIAL] ?: false
            )
            val next = transform(current)
            prefs[Keys.MUSIC] = next.music
            prefs[Keys.SOUND] = next.sound
            prefs[Keys.HAPTIC] = next.haptic
            prefs[Keys.QUALITY] = next.quality.name
            prefs[Keys.COLOR_BLIND] = next.colorBlind
            prefs[Keys.CONTRAST] = next.highContrast
            prefs[Keys.REDUCED] = next.reducedEffects
            prefs[Keys.ROTATE] = next.cameraRotation
            prefs[Keys.LARGE_TEXT] = next.largeText
            prefs[Keys.THEME] = next.themeId
            prefs[Keys.TUTORIAL] = next.tutorialDone
        }
    }

    suspend fun readWallet(secret: ByteArray): WalletRecord? {
        val prefs = context.dataStore.data.first()
        val coins = prefs[Keys.COINS] ?: return null
        val hints = prefs[Keys.HINTS] ?: 0
        val ads = prefs[Keys.ADS] ?: false
        val premium = prefs[Keys.PREMIUM] ?: false
        val signature = prefs[Keys.WALLET_SIG] ?: return null
        val payload = ProgressToken.walletPayload(coins, hints, ads, premium)
        return if (ProgressToken.verify(secret, payload, signature)) {
            WalletRecord(coins, hints, ads, premium, signature)
        } else {
            null
        }
    }

    suspend fun writeWallet(record: WalletRecord) {
        context.dataStore.edit { prefs ->
            prefs[Keys.COINS] = record.coins
            prefs[Keys.HINTS] = record.hints
            prefs[Keys.ADS] = record.adsRemoved
            prefs[Keys.PREMIUM] = record.premium
            prefs[Keys.WALLET_SIG] = record.signature
        }
    }

    suspend fun meta(): Meta {
        val prefs = context.dataStore.data.first()
        return Meta(
            hintlessClears = prefs[Keys.HINTLESS] ?: 0,
            dailyClears = prefs[Keys.DAILY_CLEARS] ?: 0,
            streak = prefs[Keys.STREAK] ?: 0,
            lastDailyDay = prefs[Keys.LAST_DAILY] ?: -1L,
            endlessIndex = prefs[Keys.ENDLESS] ?: 0,
            ownedThemes = (prefs[Keys.THEMES] ?: "neon").split(",").filter { it.isNotBlank() }.toSet(),
            levelsSinceAd = prefs[Keys.AD_LEVELS] ?: 0,
            lastInterstitialAt = prefs[Keys.AD_TIME] ?: 0L,
            lastAppOpenAt = prefs[Keys.APP_OPEN] ?: 0L
        )
    }

    suspend fun writeMeta(meta: Meta) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HINTLESS] = meta.hintlessClears
            prefs[Keys.DAILY_CLEARS] = meta.dailyClears
            prefs[Keys.STREAK] = meta.streak
            prefs[Keys.LAST_DAILY] = meta.lastDailyDay
            prefs[Keys.ENDLESS] = meta.endlessIndex
            prefs[Keys.THEMES] = meta.ownedThemes.joinToString(",")
            prefs[Keys.AD_LEVELS] = meta.levelsSinceAd
            prefs[Keys.AD_TIME] = meta.lastInterstitialAt
            prefs[Keys.APP_OPEN] = meta.lastAppOpenAt
        }
    }

    data class Meta(
        val hintlessClears: Int = 0,
        val dailyClears: Int = 0,
        val streak: Int = 0,
        val lastDailyDay: Long = -1,
        val endlessIndex: Int = 0,
        val ownedThemes: Set<String> = setOf("neon"),
        val levelsSinceAd: Int = 0,
        val lastInterstitialAt: Long = 0,
        val lastAppOpenAt: Long = 0
    )

    private object Keys {
        val MUSIC = booleanPreferencesKey("music")
        val SOUND = booleanPreferencesKey("sound")
        val HAPTIC = booleanPreferencesKey("haptic")
        val QUALITY = stringPreferencesKey("quality")
        val COLOR_BLIND = booleanPreferencesKey("color_blind")
        val CONTRAST = booleanPreferencesKey("contrast")
        val REDUCED = booleanPreferencesKey("reduced")
        val ROTATE = booleanPreferencesKey("rotate")
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val THEME = stringPreferencesKey("theme")
        val TUTORIAL = booleanPreferencesKey("tutorial_done")
        val COINS = intPreferencesKey("coins")
        val HINTS = intPreferencesKey("hints")
        val ADS = booleanPreferencesKey("ads_removed")
        val PREMIUM = booleanPreferencesKey("premium")
        val WALLET_SIG = stringPreferencesKey("wallet_sig")
        val HINTLESS = intPreferencesKey("hintless")
        val DAILY_CLEARS = intPreferencesKey("daily_clears")
        val STREAK = intPreferencesKey("streak")
        val LAST_DAILY = longPreferencesKey("last_daily")
        val ENDLESS = intPreferencesKey("endless")
        val THEMES = stringPreferencesKey("themes")
        val AD_LEVELS = intPreferencesKey("ad_levels")
        val AD_TIME = longPreferencesKey("ad_time")
        val APP_OPEN = longPreferencesKey("app_open")
    }
}
