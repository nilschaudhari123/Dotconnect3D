package com.naampath.colorpath3d.achievements

import com.naampath.colorpath3d.data.ProgressRepository
import kotlinx.coroutines.flow.StateFlow

class AchievementManager(private val progress: ProgressRepository) {
    val unlocked: StateFlow<Set<AchievementId>> = progress.achievements

    fun catalog(): List<AchievementId> = AchievementId.entries
}
