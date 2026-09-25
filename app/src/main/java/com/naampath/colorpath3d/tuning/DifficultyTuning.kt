package com.naampath.colorpath3d.tuning

/**
 * Local defaults for every remotely configurable knob. Remote Config overrides
 * these at runtime; gameplay code reads the resolved [DifficultyTuning] only.
 */
data class DifficultyTuning(
    val obstacleMultiplier: Float = 1f,
    val extraPairs: Int = 0,
    val dailyRewardCoins: Int = 40,
    val hintCosts: List<Int> = listOf(20, 45, 90),
    val rewardedHintGrant: Int = 1,
    val rewardedCoinGrant: Int = 50,
    val interstitialEvery: Int = 3,
    val minSecondsBetweenInterstitials: Int = 90,
    val appOpenMinIntervalSec: Int = 240,
    val perfectBonus: Int = 150,
    val fastBonus: Int = 100,
    val noMistakeBonus: Int = 150,
    val hintPenalty: Int = 80,
    val mistakePenalty: Int = 50,
    val timePenaltyPerSec: Int = 2,
    val movePenalty: Int = 10,
    val efficiencyBonus: Int = 80,
    val levelCoinBase: Int = 8,
    val starCoinBonus: Int = 6,
    val endlessEnabled: Boolean = true,
    val dailyEnabled: Boolean = true,
    val timedExtraSeconds: Int = 30
) {
    companion object {
        fun from(reader: (String, String) -> String): DifficultyTuning {
            fun int(key: String, default: Int) = reader(key, default.toString()).toIntOrNull() ?: default
            fun float(key: String, default: Float) = reader(key, default.toString()).toFloatOrNull() ?: default
            fun bool(key: String, default: Boolean) = reader(key, default.toString()).toBooleanStrictOrNull() ?: default
            return DifficultyTuning(
                obstacleMultiplier = float("obstacle_multiplier", 1f),
                extraPairs = int("hard_extra_pairs", 0),
                dailyRewardCoins = int("daily_reward_coins", 40),
                hintCosts = listOf(
                    int("hint_cost_1", 20),
                    int("hint_cost_2", 45),
                    int("hint_cost_3", 90)
                ),
                rewardedHintGrant = int("rewarded_hint_grant", 1),
                rewardedCoinGrant = int("rewarded_coin_grant", 50),
                interstitialEvery = int("ad_interstitial_every", 3).coerceAtLeast(2),
                minSecondsBetweenInterstitials = int("ad_min_interval_sec", 90),
                appOpenMinIntervalSec = int("ad_app_open_interval_sec", 240),
                perfectBonus = int("score_perfect_bonus", 150),
                fastBonus = int("score_fast_bonus", 100),
                noMistakeBonus = int("score_clean_bonus", 150),
                hintPenalty = int("score_hint_penalty", 80),
                mistakePenalty = int("score_mistake_penalty", 50),
                timePenaltyPerSec = int("score_time_penalty", 2),
                movePenalty = int("score_move_penalty", 10),
                efficiencyBonus = int("score_efficiency_bonus", 80),
                levelCoinBase = int("level_coin_base", 8),
                starCoinBonus = int("star_coin_bonus", 6),
                endlessEnabled = bool("feature_endless", true),
                dailyEnabled = bool("feature_daily", true),
                timedExtraSeconds = int("timed_extra_seconds", 30)
            )
        }
    }
}
