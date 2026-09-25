package com.naampath.colorpath3d.progress

import com.naampath.colorpath3d.gameplay.LevelResult
import com.naampath.colorpath3d.model.GameMode
import com.naampath.colorpath3d.security.ProgressToken

data class LevelRecord(
    val levelId: Int,
    val mode: String,
    val stars: Int,
    val bestScore: Int,
    val bestTimeMs: Long,
    val bestMoves: Int,
    val completed: Boolean,
    val signature: String
)

data class WalletRecord(
    val coins: Int,
    val hints: Int,
    val adsRemoved: Boolean,
    val premium: Boolean,
    val signature: String
)

data class CampaignState(
    val levels: Map<String, LevelRecord> = emptyMap(),
    val wallet: WalletRecord,
    val hintlessClears: Int = 0,
    val dailyClears: Int = 0,
    val streak: Int = 0,
    val lastDailyDay: Long = -1,
    val endlessIndex: Int = 0,
    val tutorialDone: Boolean = false,
    val ownedThemes: Set<String> = setOf("neon")
)

/** Applies results and drops records whose signatures do not match. */
class ProgressBookkeeper(private val secret: ByteArray) {
    fun fresh(): CampaignState = CampaignState(wallet = signWallet(0, 1, false, false))

    fun trusted(state: CampaignState): CampaignState {
        val levels = state.levels.filterValues { record ->
            ProgressToken.verify(secret, payload(record), record.signature)
        }
        val wallet = if (ProgressToken.verify(secret, walletPayload(state.wallet), state.wallet.signature)) {
            state.wallet
        } else {
            signWallet(0, 0, false, false)
        }
        return state.copy(levels = levels, wallet = wallet)
    }

    fun apply(state: CampaignState, result: LevelResult, coinReward: Int): CampaignState {
        val safe = trusted(state)
        if (!result.completed) return safe
        val key = key(result.levelId, result.mode.name)
        val previous = safe.levels[key]
        val stars = maxOf(previous?.stars ?: 0, result.stars)
        val score = maxOf(previous?.bestScore ?: 0, result.score)
        val time = minOf(previous?.bestTimeMs ?: Long.MAX_VALUE, result.elapsedMs)
        val moves = minOf(previous?.bestMoves ?: Int.MAX_VALUE, result.moves)
        val record = signLevel(result.levelId, result.mode.name, stars, score, time, moves)
        val firstClear = previous?.completed != true
        val coins = safe.wallet.coins + if (firstClear) coinReward else 0
        var hintless = safe.hintlessClears
        if (result.hintsUsed == 0 && result.mode != GameMode.DAILY) hintless += 1
        return safe.copy(levels = safe.levels + (key to record), wallet = signWallet(coins, safe.wallet.hints, safe.wallet.adsRemoved, safe.wallet.premium), hintlessClears = hintless)
    }

    fun spendCoins(state: CampaignState, amount: Int): CampaignState? {
        val safe = trusted(state)
        if (amount < 0 || safe.wallet.coins < amount) return null
        return safe.copy(wallet = signWallet(safe.wallet.coins - amount, safe.wallet.hints, safe.wallet.adsRemoved, safe.wallet.premium))
    }

    fun grantCoins(state: CampaignState, amount: Int): CampaignState {
        val safe = trusted(state)
        return safe.copy(wallet = signWallet(safe.wallet.coins + amount.coerceAtLeast(0), safe.wallet.hints, safe.wallet.adsRemoved, safe.wallet.premium))
    }

    fun setEntitlement(state: CampaignState, adsRemoved: Boolean, premium: Boolean, bonusCoins: Int, bonusHints: Int): CampaignState {
        val safe = trusted(state)
        return safe.copy(
            wallet = signWallet(
                safe.wallet.coins + bonusCoins.coerceAtLeast(0),
                safe.wallet.hints + bonusHints.coerceAtLeast(0),
                safe.wallet.adsRemoved || adsRemoved,
                safe.wallet.premium || premium
            )
        )
    }

    private fun signLevel(levelId: Int, mode: String, stars: Int, score: Int, timeMs: Long, moves: Int): LevelRecord {
        val payload = ProgressToken.levelPayload(levelId, mode, stars, score, timeMs, moves)
        return LevelRecord(levelId, mode, stars, score, timeMs, moves, true, ProgressToken.sign(secret, payload))
    }

    private fun signWallet(coins: Int, hints: Int, adsRemoved: Boolean, premium: Boolean): WalletRecord {
        val record = WalletRecord(coins, hints, adsRemoved, premium, "")
        return record.copy(signature = ProgressToken.sign(secret, walletPayload(record)))
    }

    private fun payload(record: LevelRecord) =
        ProgressToken.levelPayload(record.levelId, record.mode, record.stars, record.bestScore, record.bestTimeMs, record.bestMoves)

    private fun walletPayload(record: WalletRecord) =
        ProgressToken.walletPayload(record.coins, record.hints, record.adsRemoved, record.premium)

    companion object {
        fun key(levelId: Int, mode: String) = "$mode:$levelId"
    }
}
