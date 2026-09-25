package com.naampath.colorpath3d.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class SoundManager(context: Context) {
    private val sounds = mutableMapOf<SoundId, Int>()
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    @Volatile var enabled: Boolean = true

    init {
        val dir = File(context.cacheDir, "tones").apply { mkdirs() }
        SoundId.entries.forEach { id ->
            val file = File(dir, "${id.name.lowercase()}.wav")
            if (!file.exists()) file.writeBytes(wav(id))
            sounds[id] = pool.load(file.absolutePath, 1)
        }
    }

    fun play(id: SoundId) {
        if (!enabled) return
        val sound = sounds[id] ?: return
        if (sound == 0) return
        pool.play(sound, 0.45f, 0.45f, 1, 0, 1f)
    }

    private fun wav(id: SoundId): ByteArray {
        val spec = when (id) {
            SoundId.CLICK -> Tone(880.0, 40)
            SoundId.DRAW -> Tone(520.0, 50)
            SoundId.CONNECT -> Tone(660.0, 140)
            SoundId.INVALID -> Tone(180.0, 120)
            SoundId.COMPLETE -> Tone(523.0, 280)
            SoundId.STAR -> Tone(988.0, 160)
            SoundId.HINT -> Tone(740.0, 120)
            SoundId.ACHIEVEMENT -> Tone(1046.0, 240)
        }
        return encode(spec)
    }

    private fun encode(tone: Tone): ByteArray {
        val rate = 22050
        val count = rate * tone.ms / 1000
        val pcm = ByteArray(count * 2)
        for (i in 0 until count) {
            val t = i.toDouble() / rate
            val env = kotlin.math.min(1.0, i / 80.0) * kotlin.math.min(1.0, (count - i) / 200.0)
            val sample = (sin(2 * PI * tone.freq * t) * env * 0.4 * 32767).toInt().coerceIn(-32767, 32767)
            pcm[i * 2] = (sample and 0xff).toByte()
            pcm[i * 2 + 1] = ((sample shr 8) and 0xff).toByte()
        }
        val header = ByteArray(44)
        val dataSize = pcm.size
        val chunk = 36 + dataSize
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeInt(header, 4, chunk)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeInt(header, 16, 16)
        header[20] = 1; header[22] = 1
        writeInt(header, 24, rate)
        writeInt(header, 28, rate * 2)
        header[32] = 2; header[34] = 16
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeInt(header, 40, dataSize)
        return header + pcm
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value and 0xff).toByte()
        target[offset + 1] = ((value shr 8) and 0xff).toByte()
        target[offset + 2] = ((value shr 16) and 0xff).toByte()
        target[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private data class Tone(val freq: Double, val ms: Int)
}
