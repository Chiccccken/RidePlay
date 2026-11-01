package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo

/** App-only orientation preference. DEFAULT restores each screen's upstream policy. */
internal object RidePlayOrientation {
    private const val PREFS = "rideplay_orientation"
    private const val KEY = "mode"
    enum class Mode { DEFAULT, LANDSCAPE, PORTRAIT }

    fun load(context: Context): Mode = Mode.entries.firstOrNull {
        it.name == context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
    } ?: Mode.DEFAULT

    fun save(context: Context, mode: Mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }

    fun requested(mode: Mode, original: Int): Int = when (mode) {
        Mode.DEFAULT -> original
        Mode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        Mode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun apply(activity: Activity, original: Int) {
        val next = requested(load(activity), original)
        if (activity.requestedOrientation != next) activity.requestedOrientation = next
    }
}
