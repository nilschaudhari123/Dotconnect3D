package com.naampath.colorpath3d.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.naampath.colorpath3d.audio.HapticKind

class HapticPlayer(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    @Volatile var enabled: Boolean = true

    fun play(kind: HapticKind) {
        if (!enabled) return
        val effect = vibrator ?: return
        val (duration, amplitude) = when (kind) {
            HapticKind.SELECT -> 12L to 40
            HapticKind.MOVE -> 8L to 30
            HapticKind.INVALID -> 28L to 80
            HapticKind.CONNECT -> 20L to 70
            HapticKind.COMPLETE -> 40L to 120
            HapticKind.ACHIEVEMENT -> 35L to 90
        }
        runCatching {
            effect.vibrate(VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255)))
        }
    }
}
