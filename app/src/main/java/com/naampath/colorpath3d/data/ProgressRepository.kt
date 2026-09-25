package com.naampath.colorpath3d.data

import com.naampath.colorpath3d.achievements.AchievementEvaluator
import com.naampath.colorpath3d.achievements.AchievementId
import com.naampath.colorpath3d.achievements.AchievementSnapshot
import com.naampath.colorpath3d.data.db.AchievementEntity
import com.naampath.colorpath3d.data.db.AppDatabase
import com.naampath.colorpath3d.data.db.DailyResultEntity
import com.naampath.colorpath3d.data.db.LevelProgressEntity
import com.naampath.colorpath3d.data.db.LocalScoreEntity
import com.naampath.colorpath3d.gameplay.LevelResult
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.progress.CampaignState
import com.naampath.colorpath3d.progress.LevelRecord
import com.naampath.colorpath3d.progress.ProgressBookkeeper
import com.naampath.colorpath3d.security.SecurityKeyProvider
import com.naampath.colorpath3d.tuning.DifficultyTuning
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ProgressRepository(
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    keys: SecurityKeyProvider
) {
    private val secret = keys.secret()
    private val bookkeeper = ProgressBookkeeper(secret)
    private val _state = MutableStateFlow(bookkeeper.fresh())
    val state: StateFlow<CampaignState> = _state
    private val _achievements = MutableStateFlow<Set<AchievementId>>(emptySet())
    val achievements: StateFlow<Set<AchievementId>> = _achievements

    suspend fun load() {
        val levels = database.levels().all().map {
            ProgressBookkeeper.key(it.levelId, it.mode) to LevelRecord(
                it.levelId, it.mode, it.stars, it.bestScore, it.bestTimeMs, it.bestMoves, it.completed, it.signature
            )
        }.toMap()
        val meta = settings.meta()
        val wallet = settings.readWallet(secret) ?: bookkeeper.fresh().wallet
        val loaded = bookkeeper.trusted(
            CampaignState(
                levels = levels,
                wallet = wallet,
                hintlessClears = meta.hintlessClears,
                dailyClears = meta.dailyClears,
                streak = meta.streak,
                lastDailyDay = meta.lastDailyDay,
                endlessIndex = meta.endlessIndex,
                tutorialDone = settings.settings.first().tutorialDone,
                ownedThemes = meta.ownedThemes
            )
        )
        _state.value = loaded
        if (settings.readWallet(secret) == null) settings.writeWallet(loaded.wallet)
        _achievements.value = database.achievements().all().mapNotNull { runCatching { AchievementId.valueOf(it.id) }.getOrNull() }.toSet()
    }

    suspend fun record(result: LevelResult, tuning: DifficultyTuning): List<AchievementId> {
        val reward = if (result.completed) tuning.levelCoinBase + result.stars * tuning.starCoinBonus else 0
        var next = bookkeeper.apply(_state.value, result, reward)
        if (result.completed && result.mode == GameMode.DAILY) {
            val today = LocalDate.now().toEpochDay()
            val continued = next.lastDailyDay == today - 1
            val streak = if (next.lastDailyDay == today) next.streak else if (continued) next.streak + 1 else 1
            next = next.copy(streak = streak, lastDailyDay = today, dailyClears = next.dailyClears + 1)
            database.daily().upsert(DailyResultEntity(today, result.stars, result.score, true))
            next = bookkeeper.grantCoins(next, tuning.dailyRewardCoins)
        }
        if (result.completed && result.mode == GameMode.ENDLESS) {
            val index = (result.levelId - 100_000).coerceAtLeast(0)
            next = next.copy(endlessIndex = maxOf(next.endlessIndex, index + 1))
        }
        _state.value = next
        persist(next)
        if (result.completed) {
            database.scores().insert(
                LocalScoreEntity(
                    board = result.mode.name,
                    score = result.score,
                    timeMs = result.elapsedMs,
                    epochDay = LocalDate.now().toEpochDay()
                )
            )
        }
        return refreshAchievements(result)
    }

    suspend fun spend(amount: Int): Boolean {
        val next = bookkeeper.spendCoins(_state.value, amount) ?: return false
        _state.value = next
        settings.writeWallet(next.wallet)
        return true
    }

    suspend fun grantCoins(amount: Int) {
        val next = bookkeeper.grantCoins(_state.value, amount)
        _state.value = next
        settings.writeWallet(next.wallet)
    }

    suspend fun unlockTheme(themeId: String) {
        val next = _state.value.copy(ownedThemes = _state.value.ownedThemes + themeId)
        _state.value = next
        persistMeta(next)
    }

    suspend fun applyEntitlement(adsRemoved: Boolean, premium: Boolean, coins: Int, hints: Int) {
        val next = bookkeeper.setEntitlement(_state.value, adsRemoved, premium, coins, hints)
        val themes = if (premium) next.ownedThemes + com.naampath.colorpath3d.theme.Themes.all.map { it.id } else next.ownedThemes
        _state.value = next.copy(ownedThemes = themes)
        persist(next.copy(ownedThemes = themes))
    }

    suspend fun markTutorialDone() {
        settings.update { it.copy(tutorialDone = true) }
        _state.value = _state.value.copy(tutorialDone = true)
    }

    suspend fun topScores(board: String) = database.scores().top(board)

    suspend fun adMeta() = settings.meta()

    suspend fun writeAdMeta(meta: SettingsRepository.Meta) = settings.writeMeta(meta)

    private suspend fun persist(state: CampaignState) {
        state.levels.values.forEach { record ->
            database.levels().upsert(
                LevelProgressEntity(
                    record.levelId, record.mode, record.stars, record.bestScore,
                    record.bestTimeMs, record.bestMoves, record.completed, record.signature
                )
            )
        }
        settings.writeWallet(state.wallet)
        persistMeta(state)
    }

    private suspend fun persistMeta(state: CampaignState) {
        val current = settings.meta()
        settings.writeMeta(
            current.copy(
                hintlessClears = state.hintlessClears,
                dailyClears = state.dailyClears,
                streak = state.streak,
                lastDailyDay = state.lastDailyDay,
                endlessIndex = state.endlessIndex,
                ownedThemes = state.ownedThemes
            )
        )
    }

    private suspend fun refreshAchievements(result: LevelResult): List<AchievementId> {
        val clears = _state.value.levels.values.count { it.completed && it.mode == GameMode.CLASSIC.name }
        val snapshot = AchievementSnapshot(
            campaignClears = clears,
            threeStarClears = _state.value.levels.values.count { it.stars >= 3 },
            cleanClears = if (result.completed && result.mistakes == 0) 1 else 0,
            fastClears = if (result.completed) 1 else 0,
            hintlessClears = _state.value.hintlessClears,
            dailyClears = _state.value.dailyClears,
            streak = _state.value.streak,
            connections = if (result.completed) 1 else 0
        )
        val earned = AchievementEvaluator.unlocked(snapshot)
        val fresh = earned - _achievements.value
        fresh.forEach { database.achievements().insert(AchievementEntity(it.name, System.currentTimeMillis())) }
        _achievements.value = _achievements.value + earned
        return fresh.toList()
    }

}
