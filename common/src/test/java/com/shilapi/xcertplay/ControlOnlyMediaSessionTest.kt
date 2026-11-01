package com.shilapi.xcertplay

import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Looper
import com.shilapi.xcertplay.airplay.CarPlayMediaButton
import com.shilapi.xcertplay.media.CarPlayNowPlaying
import com.shilapi.xcertplay.orchestration.CarPlayController
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 33], manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
class ControlOnlyMediaSessionTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val owners = mutableListOf<CarPlayController>()
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun session(): MediaSession? = ReflectionHelpers.getField(CarPlayMediaKeys, "session")
    private fun owner(info: CarPlayNowPlaying = CarPlayNowPlaying()): CarPlayController =
        mock(CarPlayController::class.java).also {
            owners += it
            `when`(it.nowPlayingSnapshot()).thenReturn(info)
        }
    private fun attach(controller: CarPlayController, disabled: Boolean = true) {
        CarPlayMediaKeys.attach(app, controller, audioOutputDisabled = disabled)
        idle()
    }
    private fun assertNoFocus() {
        assertNull(ReflectionHelpers.getField<Any?>(CarPlayMediaKeys, "focusRequest"))
        assertNull(ReflectionHelpers.getField<Any?>(CarPlayMediaKeys, "focusOwner"))
        assertFalse(ReflectionHelpers.getField(CarPlayMediaKeys, "focusHeld"))
    }
    @After fun cleanup() {
        owners.forEach(CarPlayMediaKeys::detach)
        AirPlayPersistence.saveControlOnlyMediaSession(app, false)
        AirPlayPersistence.saveDisableMediaKeyAudioFocus(app, false)
        CarPlayBackgroundSession.clear()
        idle()
    }
    @Test fun defaultIsOffAndAudioEnabledConnectionsKeepUpstreamBehaviour() {
        assertFalse(AirPlayPersistence.loadControlOnlyMediaSession(app))
        attach(owner())
        assertNull(session())
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        attach(owner(), disabled = false)
        assertNull(session())
    }
    @Test fun retainedMetadataAndPausedSessionExistWithoutAnyAudioCallback() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        attach(owner(CarPlayNowPlaying(title = "Song", artist = "Artist", playing = false)))
        val current = session()!!
        assertTrue(current.isActive)
        assertEquals("Song", current.controller.metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
        assertEquals(PlaybackState.STATE_PAUSED, current.controller.playbackState.state)
        assertNoFocus()
        CarPlayMediaKeys.onMediaAudioChanged(true)
        CarPlayMediaKeys.onMediaAudioChanged(false)
        idle()
        assertSame(current, session())
        assertEquals(PlaybackState.STATE_PAUSED, current.controller.playbackState.state)
        assertNoFocus()
    }
    @Test fun metadataAndPlaybackUpdatesDoNotClaimFocus() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner()
        attach(phone)
        `when`(phone.nowPlayingSnapshot()).thenReturn(CarPlayNowPlaying(title = "Next", playing = true, elapsedMillis = 1200))
        CarPlayMediaKeys::class.java.getDeclaredMethod("onNowPlayingChanged", CarPlayController::class.java)
            .apply { isAccessible = true }.invoke(CarPlayMediaKeys, phone)
        CarPlayMediaKeys::class.java.getDeclaredMethod("onIphonePlaying", CarPlayController::class.java, Boolean::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(CarPlayMediaKeys, phone, true)
        idle()
        assertEquals("Next", session()!!.controller.metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
        assertEquals(PlaybackState.STATE_PLAYING, session()!!.controller.playbackState.state)
        assertNoFocus()
    }
    @Test fun controlsUseExistingCarPlayHidAndDetachReleasesSession() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner()
        attach(phone)
        val current = session()!!
        with(current.controller.transportControls) { play(); pause(); skipToNext(); skipToPrevious() }
        idle()
        listOf(CarPlayMediaButton.PLAY, CarPlayMediaButton.PAUSE,
            CarPlayMediaButton.NEXT, CarPlayMediaButton.PREVIOUS).forEach { verify(phone).sendMediaButton(it) }
        CarPlayMediaKeys.detach(phone)
        idle()
        assertFalse(current.isActive)
        assertNull(session())
        assertNoFocus()
    }
    @Test fun queuedOldAudioCallbackCannotStartANewerOrdinarySession() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val old = owner()
        attach(old)
        val oldSession = session()!!
        CarPlayMediaKeys.onMediaAudioChanged(true)
        val next = owner()
        CarPlayMediaKeys.attach(app, next, audioOutputDisabled = false)
        CarPlayMediaKeys.detach(old)
        idle()
        assertFalse(oldSession.isActive)
        assertNull(session())
        assertNoFocus()
    }
    @Test fun settingChangeWaitsForNewConnectionAndCanBeReverted() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner()
        attach(phone)
        val current = session()!!
        AirPlayPersistence.saveControlOnlyMediaSession(app, false)
        attach(phone)
        assertSame(current, session())
        attach(owner())
        assertFalse(current.isActive)
        assertNull(session())
    }
}
