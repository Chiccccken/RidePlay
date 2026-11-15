package com.shilapi.xcertplay

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 33], manifest = Config.NONE)
class RidePlayOrientationTest {
    private val app get() = RuntimeEnvironment.getApplication()
    @After fun cleanup() { app.getSharedPreferences("rideplay_orientation", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun defaultAndInvalidPreferencesKeepTheOriginalPolicy() {
        assertEquals(RidePlayOrientation.Mode.DEFAULT, RidePlayOrientation.load(app))
        app.getSharedPreferences("rideplay_orientation", Context.MODE_PRIVATE).edit().putString("mode", "unknown").commit()
        assertEquals(RidePlayOrientation.Mode.DEFAULT, RidePlayOrientation.load(app))
        for (original in listOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)) {
            assertEquals(original, RidePlayOrientation.requested(RidePlayOrientation.Mode.DEFAULT, original))
        }
    }
    @Test fun lockedModePersistsAndRestoringDefaultReleasesTheLock() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val activity = controller.get()
            val original = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            for ((mode, expected) in listOf(
                RidePlayOrientation.Mode.LANDSCAPE to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                RidePlayOrientation.Mode.PORTRAIT to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                RidePlayOrientation.Mode.DEFAULT to original,
            )) {
                RidePlayOrientation.save(app, mode)
                assertEquals(mode, RidePlayOrientation.load(app))
                RidePlayOrientation.apply(activity, original)
                assertEquals(expected, activity.requestedOrientation)
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun aNewScreenUsesThePersistedChoice() {
        RidePlayOrientation.save(app, RidePlayOrientation.Mode.LANDSCAPE)
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            RidePlayOrientation.apply(controller.get(), ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, controller.get().requestedOrientation)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun launchWaitsForRotationButTimesOutWhenTheSystemIgnoresIt() {
        RidePlayOrientation.save(app, RidePlayOrientation.Mode.PORTRAIT)
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val initial = Configuration(controller.get().resources.configuration)
        try {
            val activity = controller.get()
            val decor = activity.window.decorView
            val configuration = Configuration(decor.resources.configuration)
            configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
            @Suppress("DEPRECATION")
            decor.resources.updateConfiguration(configuration, decor.resources.displayMetrics)
            RidePlayOrientation.prepareLaunch(activity, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, activity.requestedOrientation)
            assertTrue(decor.viewTreeObserver.dispatchOnPreDraw()) // true means cancelled
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(java.time.Duration.ofSeconds(1))
            assertFalse(decor.viewTreeObserver.dispatchOnPreDraw())

            RidePlayOrientation.prepareLaunch(activity, ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR)
            assertTrue(decor.viewTreeObserver.dispatchOnPreDraw())
            configuration.orientation = Configuration.ORIENTATION_PORTRAIT
            @Suppress("DEPRECATION")
            decor.resources.updateConfiguration(configuration, decor.resources.displayMetrics)
            assertFalse(decor.viewTreeObserver.dispatchOnPreDraw())
        } finally {
            @Suppress("DEPRECATION")
            controller.get().resources.updateConfiguration(initial, controller.get().resources.displayMetrics)
            controller.pause().stop().destroy()
        }
    }
}
