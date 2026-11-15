package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewTreeObserver

/** App-only orientation preference. DEFAULT restores each screen's upstream policy. */
internal object RidePlayOrientation {
    private const val PREFS = "rideplay_orientation"
    private const val KEY = "mode"
    private const val LAUNCH_ROTATION_TIMEOUT_MS = 1000L
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

    /** No starting preview; withhold app content until the requested configuration arrives.
     * Multi-window and OEM policies may ignore rotation, so never wait indefinitely.
     */
    fun prepareLaunch(activity: Activity, original: Int) {
        apply(activity, original)
        val mode = load(activity)
        if (mode == Mode.DEFAULT || activity.isInMultiWindowMode) return
        val decor = activity.window.decorView
        var expired = false
        val timeout = Runnable { expired = true; decor.invalidate() }
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = expired || activity.isInMultiWindowMode || matches(mode, decor.resources.configuration.orientation)
                if (ready) {
                    if (decor.viewTreeObserver.isAlive) decor.viewTreeObserver.removeOnPreDrawListener(this)
                    decor.removeCallbacks(timeout)
                }
                return ready
            }
        }
        decor.viewTreeObserver.addOnPreDrawListener(listener)
        decor.postDelayed(timeout, LAUNCH_ROTATION_TIMEOUT_MS)
    }

    private fun matches(mode: Mode, orientation: Int): Boolean = when (mode) {
        Mode.DEFAULT -> true
        Mode.LANDSCAPE -> orientation == Configuration.ORIENTATION_LANDSCAPE
        Mode.PORTRAIT -> orientation == Configuration.ORIENTATION_PORTRAIT
    }
}
