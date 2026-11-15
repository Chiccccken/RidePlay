package com.shilapi.xcertplay

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log

/** One bounded local playback event, for ROMs that select media keys by playback UID history. */
internal class MediaRouteBootstrap {
    private var generation = 0L
    private var track: AudioTrack? = null
    private var lastRequest = Long.MIN_VALUE
    @Volatile var result = "idle"
        private set

    @Synchronized fun request(reason: String) {
        val now = SystemClock.elapsedRealtime()
        if (track != null || (lastRequest != Long.MIN_VALUE && now - lastRequest < 1500)) return
        lastRequest = now
        val owner = ++generation
        result = "queued:$reason"
        Thread({
            try {
                synchronized(this) {
                    if (generation != owner) return@Thread
                    val samples = ShortArray(9600) // 200 ms, 48 kHz, mono PCM16, all zero.
                    val audio = AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                        .setAudioFormat(AudioFormat.Builder().setSampleRate(48000)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(samples.size * 2).build()
                    track = audio
                    check(audio.write(samples, 0, samples.size) == samples.size)
                    audio.play()
                    result = "started:$reason"
                }
                Thread.sleep(300)
                synchronized(this) { if (generation == owner) result = "released:$reason" }
            } catch (error: Exception) {
                synchronized(this) {
                    if (generation == owner) result = "failed:${error.javaClass.simpleName}"
                }
            } finally {
                synchronized(this) {
                    if (generation == owner) releaseTrack()
                    Log.i("DiPlay-MediaKeys", "media route bootstrap $result")
                }
            }
        }, "rideplay-media-route").apply { isDaemon = true }.start()
    }

    @Synchronized fun cancel() {
        generation++
        releaseTrack()
        lastRequest = Long.MIN_VALUE
        result = "cancelled"
    }

    private fun releaseTrack() {
        val audio = track ?: return
        track = null
        try { audio.stop() } catch (_: IllegalStateException) { }
        audio.release()
    }
}
