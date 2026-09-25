package com.naampath.colorpath3d.achievements

enum class AchievementId(val title: String, val description: String) {
    FIRST_CONNECTION("First Connection", "Connect a matching pair."),
    LEVELS_10("10 Levels Complete", "Finish 10 campaign levels."),
    LEVELS_50("50 Levels Complete", "Finish 50 campaign levels."),
    LEVELS_100("100 Levels Complete", "Finish 100 campaign levels."),
    LEVELS_500("500 Levels Complete", "Finish 500 campaign levels."),
    LEVELS_1000("1000 Levels Complete", "Finish the full campaign."),
    PERFECT_SOLVER("Perfect Solver", "Earn 3 stars on a level."),
    SPEED_MASTER("Speed Master", "Finish a level inside the par time."),
    NO_MISTAKES("No Mistakes", "Finish a level without a wrong path."),
    HINTLESS_MASTER("Hintless Master", "Finish 10 levels without hints."),
    DAILY_CHAMPION("Daily Champion", "Complete a daily challenge."),
    STREAK_7("7 Day Streak", "Complete the daily challenge 7 days in a row."),
    STREAK_30("30 Day Streak", "Complete the daily challenge 30 days in a row."),
    STREAK_100("100 Day Streak", "Complete the daily challenge 100 days in a row.")
}

data class AchievementSnapshot(
    val campaignClears: Int,
    val threeStarClears: Int,
    val cleanClears: Int,
    val fastClears: Int,
    val hintlessClears: Int,
    val dailyClears: Int,
    val streak: Int,
    val connections: Int
)

object AchievementEvaluator {
    fun unlocked(snapshot: AchievementSnapshot): Set<AchievementId> {
        val earned = mutableSetOf<AchievementId>()
        if (snapshot.connections >= 1) earned += AchievementId.FIRST_CONNECTION
        if (snapshot.campaignClears >= 10) earned += AchievementId.LEVELS_10
        if (snapshot.campaignClears >= 50) earned += AchievementId.LEVELS_50
        if (snapshot.campaignClears >= 100) earned += AchievementId.LEVELS_100
        if (snapshot.campaignClears >= 500) earned += AchievementId.LEVELS_500
        if (snapshot.campaignClears >= 1000) earned += AchievementId.LEVELS_1000
        if (snapshot.threeStarClears >= 1) earned += AchievementId.PERFECT_SOLVER
        if (snapshot.fastClears >= 1) earned += AchievementId.SPEED_MASTER
        if (snapshot.cleanClears >= 1) earned += AchievementId.NO_MISTAKES
        if (snapshot.hintlessClears >= 10) earned += AchievementId.HINTLESS_MASTER
        if (snapshot.dailyClears >= 1) earned += AchievementId.DAILY_CHAMPION
        if (snapshot.streak >= 7) earned += AchievementId.STREAK_7
        if (snapshot.streak >= 30) earned += AchievementId.STREAK_30
        if (snapshot.streak >= 100) earned += AchievementId.STREAK_100
        return earned
    }
}
