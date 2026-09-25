package com.naampath.colorpath3d.audio

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class MusicManager(context: Context) {
    private var player: MediaPlayer? = null
    @Volatile var enabled: Boolean = true

    init {
        val file = File(context.cacheDir, "colorpath_pad.wav")
        if (!file.exists()) file.writeBytes(loop())
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            isLooping = true
            setVolume(0.22f, 0.22f)
            prepare()
        }
    }

    fun start() {
        if (!enabled) return
        runCatching { if (player?.isPlaying != true) player?.start() }
    }

    fun stop() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun updateEnabled(value: Boolean) {
        enabled = value
        if (value) start() else stop()
    }

    private fun loop(): ByteArray {
        val rate = 22050
        val seconds = 8
        val count = rate * seconds
        val pcm = ByteArray(count * 2)
        for (i in 0 until count) {
            val t = i.toDouble() / rate
            val sample = (sin(2 * PI * 110 * t) * 0.25 + sin(2 * PI * 164.8 * t) * 0.12) * 32767
            val value = sample.toInt().coerceIn(-32767, 32767)
            pcm[i * 2] = (value and 0xff).toByte()
            pcm[i * 2 + 1] = ((value shr 8) and 0xff).toByte()
        }
        val header = ByteArray(44)
        fun put(offset: Int, value: Int) {
            header[offset] = (value and 0xff).toByte()
            header[offset + 1] = ((value shr 8) and 0xff).toByte()
            header[offset + 2] = ((value shr 16) and 0xff).toByte()
            header[offset + 3] = ((value shr 24) and 0xff).toByte()
        }
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        put(4, 36 + pcm.size)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        put(16, 16)
        header[20] = 1; header[22] = 1
        put(24, rate); put(28, rate * 2)
        header[32] = 2; header[34] = 16
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        put(40, pcm.size)
        return header + pcm
    }
}
