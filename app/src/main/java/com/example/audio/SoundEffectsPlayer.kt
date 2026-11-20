package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight, zero-dependency PCM synthesizer for tactile puzzle sound effects.
 * All audio synthesis runs on Dispatchers.Default and fails safely if audio hardware is unavailable.
 */
class SoundEffectsPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    var isMuted: Boolean = false

    fun playKeyTap() {
        if (isMuted) return
        playNotes(listOf(ToneSpec(freqHz = 520.0, durationMs = 36, gain = 0.22)))
    }

    fun playErase() {
        if (isMuted) return
        playNotes(listOf(ToneSpec(freqHz = 310.0, durationMs = 42, gain = 0.20)))
    }

    fun playEquationValid() {
        if (isMuted) return
        playNotes(
            listOf(
                ToneSpec(freqHz = 523.25, durationMs = 75, gain = 0.24),
                ToneSpec(freqHz = 783.99, durationMs = 135, gain = 0.26)
            )
        )
    }

    fun playEquationError() {
        if (isMuted) return
        playNotes(
            listOf(
                ToneSpec(freqHz = 210.0, durationMs = 95, gain = 0.24),
                ToneSpec(freqHz = 165.0, durationMs = 130, gain = 0.22)
            )
        )
    }

    fun playHintOrSwap() {
        if (isMuted) return
        playNotes(
            listOf(
                ToneSpec(freqHz = 659.25, durationMs = 65, gain = 0.22),
                ToneSpec(freqHz = 783.99, durationMs = 65, gain = 0.22),
                ToneSpec(freqHz = 987.77, durationMs = 120, gain = 0.25)
            )
        )
    }

    fun playVictory() {
        if (isMuted) return
        playNotes(
            listOf(
                ToneSpec(freqHz = 523.25, durationMs = 90, gain = 0.25),
                ToneSpec(freqHz = 659.25, durationMs = 90, gain = 0.25),
                ToneSpec(freqHz = 783.99, durationMs = 90, gain = 0.25),
                ToneSpec(freqHz = 987.77, durationMs = 100, gain = 0.25),
                ToneSpec(freqHz = 1174.66, durationMs = 240, gain = 0.28)
            )
        )
    }

    private data class ToneSpec(
        val freqHz: Double,
        val durationMs: Int,
        val gain: Double
    )

    private fun playNotes(notes: List<ToneSpec>) {
        scope.launch {
            try {
                val sampleRate = 22050
                val totalSamples = notes.sumOf { (sampleRate * it.durationMs) / 1000 }
                if (totalSamples <= 0) return@launch

                val pcm = ShortArray(totalSamples)
                var offset = 0
                for (note in notes) {
                    val count = (sampleRate * note.durationMs) / 1000
                    for (i in 0 until count) {
                        val t = i.toDouble() / sampleRate
                        val progress = i.toDouble() / count
                        // Smooth attack + exponential decay for marimba-like percussive warmth
                        val attack = if (i < 120) i / 120.0 else 1.0
                        val envelope = attack * exp(-4.5 * progress)
                        val wave = sin(2.0 * PI * note.freqHz * t) +
                            0.25 * sin(4.0 * PI * note.freqHz * t)
                        val sample = (wave * envelope * note.gain * Short.MAX_VALUE)
                            .toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        pcm[offset + i] = sample.toShort()
                    }
                    offset += count
                }

                val bufferSizeBytes = pcm.size * 2
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val totalDurationMs = notes.sumOf { it.durationMs }.toLong() + 120L
                kotlinx.coroutines.delay(totalDurationMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore cleanly in headless or Robolectric test environments
            }
        }
    }
}
