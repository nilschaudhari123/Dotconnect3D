package com.naampath.colorpath3d.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naampath.colorpath3d.achievements.AchievementId
import com.naampath.colorpath3d.achievements.AchievementManager
import com.naampath.colorpath3d.ads.AdManager
import com.naampath.colorpath3d.audio.MusicManager
import com.naampath.colorpath3d.audio.SoundId
import com.naampath.colorpath3d.audio.SoundManager
import com.naampath.colorpath3d.billing.BillingRepository
import com.naampath.colorpath3d.billing.ShopProduct
import com.naampath.colorpath3d.data.ProgressRepository
import com.naampath.colorpath3d.data.SettingsRepository
import com.naampath.colorpath3d.data.SettingsState
import com.naampath.colorpath3d.data.db.LocalScoreEntity
import com.naampath.colorpath3d.device.DevicePerformance
import com.naampath.colorpath3d.firebase.AnalyticsManager
import com.naampath.colorpath3d.game.GameCommand
import com.naampath.colorpath3d.game.GameHostImpl
import com.naampath.colorpath3d.game.GameRuntime
import com.naampath.colorpath3d.game.PlayRequest
import com.naampath.colorpath3d.gameplay.GameState
import com.naampath.colorpath3d.gameplay.HintTier
import com.naampath.colorpath3d.gameplay.LevelResult
import com.naampath.colorpath3d.haptics.HapticPlayer
import com.naampath.colorpath3d.leaderboard.LeaderboardManager
import com.naampath.colorpath3d.level.LevelRepository
import com.naampath.colorpath3d.level.World
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.model.Quality
import com.naampath.colorpath3d.progress.CampaignState
import com.naampath.colorpath3d.progress.LevelRecord
import com.naampath.colorpath3d.progress.ProgressBookkeeper
import com.naampath.colorpath3d.theme.Presentation
import com.naampath.colorpath3d.theme.Themes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ShellViewModel @Inject constructor(
    val host: GameHostImpl,
    private val sounds: SoundManager,
    private val haptics: HapticPlayer,
    private val music: MusicManager,
    settings: SettingsRepository
) : ViewModel() {
    val settings = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    init {
        viewModelScope.launch { host.sounds.collect { sounds.play(it) } }
        viewModelScope.launch { host.haptics.collect { haptics.play(it) } }
        viewModelScope.launch {
            settings.settings.collect {
                sounds.enabled = it.sound
                haptics.enabled = it.haptic
                music.updateEnabled(it.music)
            }
        }
    }

    fun click() = sounds.play(SoundId.CLICK)
}

@HiltViewModel
class MenuViewModel @Inject constructor(
    progress: ProgressRepository,
    private val remote: com.naampath.colorpath3d.firebase.RemoteConfigManager
) : ViewModel() {
    val progress: StateFlow<CampaignState> = progress.state
    fun tuning() = remote.tuning()
}

@HiltViewModel
class LevelSelectViewModel @Inject constructor(
    private val levels: LevelRepository,
    progress: ProgressRepository
) : ViewModel() {
    val progress = progress.state
    fun worlds(): List<World> = levels.worlds()
    fun record(world: World, mode: GameMode): LevelRecord? {
        return progress.value.levels[ProgressBookkeeper.key(world.firstLevel, mode.name)]
    }
    fun levelRecord(id: Int): LevelRecord? = progress.value.levels[ProgressBookkeeper.key(id, GameMode.CLASSIC.name)]
    fun unlocked(id: Int): Boolean {
        if (id <= 1) return true
        return progress.value.levels[ProgressBookkeeper.key(id - 1, GameMode.CLASSIC.name)]?.completed == true
    }
}

@HiltViewModel
class GameplayViewModel @Inject constructor(
    private val runtime: GameRuntime,
    private val host: GameHostImpl,
    private val levels: LevelRepository,
    private val progress: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val remote: com.naampath.colorpath3d.firebase.RemoteConfigManager,
    private val ads: AdManager,
    private val analytics: AnalyticsManager,
    @ApplicationContext private val deviceContext: android.content.Context
) : ViewModel() {
    val hud: StateFlow<GameState?> = host.hud
    val campaign = progress.state
    private val _result = MutableStateFlow<LevelResult?>(null)
    val result = _result.asStateFlow()
    private val _toast = MutableStateFlow<String?>(null)
    val toast = _toast.asStateFlow()
    private val _newAchievements = MutableStateFlow<List<AchievementId>>(emptyList())
    val newAchievements = _newAchievements.asStateFlow()
    var activeMode: GameMode = GameMode.CLASSIC
        private set
    var activeLevel: Int = 1
        private set

    init {
        viewModelScope.launch {
            host.results.collect { incoming ->
                _result.value = incoming
                val unlocked = progress.record(incoming, remote.tuning())
                _newAchievements.value = unlocked
                analytics.event(
                    if (incoming.completed) "level_completed" else "level_failed",
                    mapOf("level" to incoming.levelId.toString(), "mode" to incoming.mode.name)
                )
            }
        }
    }

    fun start(mode: GameMode, levelId: Int) {
        activeMode = mode
        activeLevel = levelId
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val tuning = remote.tuning()
            val level = when (mode) {
                GameMode.DAILY -> levels.daily(LocalDate.now(), tuning)
                GameMode.ENDLESS -> levels.endless(levelId, tuning)
                else -> levels.level(levelId, mode, tuning)
            }
            val quality = DevicePerformance.resolve(deviceContext, settings.quality, settings.reducedEffects)
            runtime.play(
                PlayRequest(
                    level = level,
                    mode = mode,
                    tutorial = mode == GameMode.CLASSIC && levelId == 1 && !settings.tutorialDone,
                    presentation = Presentation(
                        theme = Themes.byId(settings.themeId),
                        quality = quality,
                        colorBlind = settings.colorBlind,
                        highContrast = settings.highContrast,
                        reducedEffects = settings.reducedEffects,
                        cameraRotation = settings.cameraRotation
                    )
                )
            )
            analytics.event(
                if (mode == GameMode.DAILY) "daily_challenge_started" else "level_started",
                mapOf("level" to level.id.toString(), "mode" to mode.name)
            )
            _result.value = null
        }
    }

    fun command(command: GameCommand) = runtime.command(command)

    fun hint(tier: HintTier, activity: Activity?) {
        val tuning = remote.tuning()
        val cost = tuning.hintCosts.getOrElse(tier.ordinal) { 20 }
        viewModelScope.launch {
            if (progress.spend(cost)) {
                analytics.event("hint_used", mapOf("tier" to tier.name))
                runtime.command(GameCommand.Hint(tier))
            } else {
                _toast.value = "Not enough coins"
            }
        }
        activity?.let { /* activity kept for rewarded alternative */ }
    }

    fun rewardedHint(tier: HintTier, activity: Activity) {
        ads.showRewarded(activity) {
            viewModelScope.launch {
                analytics.event("hint_used", mapOf("tier" to tier.name, "source" to "ad"))
                runtime.command(GameCommand.Hint(tier))
            }
        }
    }

    fun rewardedTime(activity: Activity) {
        val extra = remote.tuning().timedExtraSeconds * 1000L
        ads.showRewarded(activity) { runtime.command(GameCommand.ExtendTime(extra)) }
    }

    fun rewardedCoins(activity: Activity) {
        val grant = remote.tuning().rewardedCoinGrant
        ads.showRewarded(activity) { viewModelScope.launch { progress.grantCoins(grant) } }
    }

    suspend fun onLeave(activity: Activity, drawing: Boolean) {
        ads.maybeInterstitial(activity, drawing)
    }

    fun clearToast() { _toast.value = null }

    fun finishTutorial() {
        viewModelScope.launch { progress.markTutorialDone() }
    }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())
    fun update(block: (SettingsState) -> SettingsState) {
        viewModelScope.launch { repository.update(block) }
    }
}

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val billing: BillingRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    val wallet = progress.state
    val billingReady = billing.ready
    val billingMessage = billing.message
    fun buy(activity: Activity, product: ShopProduct) = billing.launch(activity, product)
    fun restore() = billing.restore()
    fun buyTheme(themeId: String, price: Int) {
        viewModelScope.launch {
            if (progress.spend(price)) {
                progress.unlockTheme(themeId)
                settings.update { it.copy(themeId = themeId) }
            }
        }
    }
    fun selectTheme(themeId: String) {
        viewModelScope.launch { settings.update { it.copy(themeId = themeId) } }
    }
}

@HiltViewModel
class DailyViewModel @Inject constructor(
    progress: ProgressRepository,
    private val remote: com.naampath.colorpath3d.firebase.RemoteConfigManager
) : ViewModel() {
    val progress = progress.state
    fun reward() = remote.tuning().dailyRewardCoins
    fun today() = LocalDate.now()
}

@HiltViewModel
class StatsViewModel @Inject constructor(progress: ProgressRepository) : ViewModel() {
    val progress = progress.state
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(manager: AchievementManager) : ViewModel() {
    val unlocked = manager.unlocked
    val catalog = manager.catalog()
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val leaderboards: LeaderboardManager
) : ViewModel() {
    private val _rows = MutableStateFlow<List<LocalScoreEntity>>(emptyList())
    val rows = _rows.asStateFlow()
    private val _note = MutableStateFlow<String?>(null)
    val note = _note.asStateFlow()

    fun load(board: String) {
        viewModelScope.launch { _rows.value = progress.topScores(board) }
    }

    fun sync(activity: Activity, boardRes: Int) {
        leaderboards.show(activity, boardRes) {
            _note.value = "Google Play Games is not configured. Local scores stay on this device."
        }
    }
}
