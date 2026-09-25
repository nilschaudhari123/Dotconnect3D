package com.naampath.colorpath3d.device

import android.app.ActivityManager
import android.content.Context
import com.naampath.colorpath3d.model.Quality

object DevicePerformance {
    fun resolve(context: Context, requested: Quality, reducedEffects: Boolean): Quality {
        if (reducedEffects) return Quality.LOW
        if (requested != Quality.AUTO) return requested
        val manager = context.getSystemService(ActivityManager::class.java)
        val lowRam = manager?.isLowRamDevice == true
        val memory = manager?.memoryClass ?: 128
        return when {
            lowRam || memory < 160 -> Quality.LOW
            memory < 256 -> Quality.MEDIUM
            else -> Quality.HIGH
        }
    }
}
