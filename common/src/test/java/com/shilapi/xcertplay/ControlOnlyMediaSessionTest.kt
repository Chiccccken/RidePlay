package com.shilapi.xcertplay

import android.media.AudioManager
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowMediaSession
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 33], manifest = Config.NONE, shadows = [ControlOnlyMediaSessionTest.RecordingSession::class])
@LooperMode(LooperMode.Mode.PAUSED)
class ControlOnlyMediaSessionTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val owners = mutableListOf<CarPlayController>()
    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun session(): MediaSession? = ReflectionHelpers.getField(CarPlayMediaKeys, "session")
    // Robolectric does not route MediaController Binder calls to its MediaSession service.
    // Inspect the session's published state and invoke the callback bound to that session;
    // actual controller/Bluetooth dispatch is reserved for device verification.
    @Implements(MediaSession::class)
    class RecordingSession : ShadowMediaSession() {
        var publishedMetadata: MediaMetadata? = null
        var publishedState: PlaybackState? = null
        @Implementation fun setMetadata(value: MediaMetadata?) { publishedMetadata = value }
        @Implementation fun setPlaybackState(value: PlaybackState?) { publishedState = value }
    }
    private fun metadata(current: MediaSession): MediaMetadata = Shadow.extract<RecordingSession>(current).publishedMetadata!!
    private fun state(current: MediaSession): PlaybackState = Shadow.extract<RecordingSession>(current).publishedState!!
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
        ArtistNameOverrides.save(app, emptyMap())
        AirPlayPersistence.savePinyinAlbum(app, false)
        AirPlayPersistence.savePinyinMetadata(app, false)
        AirPlayPersistence.saveControlOnlyRouteBootstrap(app, false)
        AirPlayPersistence.saveControlOnlyAudioFocus(app, false)
        AirPlayPersistence.saveControlOnlyMediaSession(app, false)
        AirPlayPersistence.saveDisableMediaKeyAudioFocus(app, false)
        CarPlayBackgroundSession.clear()
        idle()
    }
    @Test fun mediaButtonBroadcastForwardsOnlyToCurrentActiveController() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner(CarPlayNowPlaying(title = "Song", playing = true))
        attach(phone)
        val intent = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON)
            .putExtra(android.content.Intent.EXTRA_KEY_EVENT,
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT))
        CarPlayMediaButtonReceiver().onReceive(app, intent)
        idle()
        verify(phone).sendMediaButton(CarPlayMediaButton.NEXT)
        clearInvocations(phone)
        CarPlayMediaKeys.detach(phone)
        CarPlayMediaButtonReceiver().onReceive(app, intent)
        idle()
        verify(phone, never()).sendMediaButton(anyInt())
        assertNull(session())
    }

    @Test fun mediaButtonReceiverIgnoresOtherBroadcastActions() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner(CarPlayNowPlaying(title = "Song", playing = true))
        attach(phone)
        CarPlayMediaButtonReceiver().onReceive(app, android.content.Intent(android.content.Intent.ACTION_VIEW)
            .putExtra(android.content.Intent.EXTRA_KEY_EVENT,
                android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_NEXT)))
        idle()
        verify(phone, never()).sendMediaButton(anyInt())
    }

    @Test fun bootstrapWaitsForIphoneDataDuringConnectionAttempts() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        AirPlayPersistence.saveControlOnlyRouteBootstrap(app, true)
        attach(owner())
        assertNotNull(session())
        CarPlayMediaKeys.onHostResumed()
        assertTrue(ReflectionHelpers.getField(CarPlayMediaKeys, "routeBootstrapPending"))
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("routeBootstrap=cancelled"))
        CarPlayMediaKeys.detach(owners.last())
        assertFalse(ReflectionHelpers.getField(CarPlayMediaKeys, "routeBootstrapPending"))
    }

    @Test fun routeBootstrapDefaultsOffAndPersistsIndependentlyOfFocus() {
        assertFalse(AirPlayPersistence.loadControlOnlyRouteBootstrap(app))
        AirPlayPersistence.saveControlOnlyRouteBootstrap(app, true)
        assertTrue(AirPlayPersistence.loadControlOnlyRouteBootstrap(app))
        assertFalse(AirPlayPersistence.loadControlOnlyAudioFocus(app))
        AirPlayPersistence.saveControlOnlyRouteBootstrap(app, false)
        assertFalse(AirPlayPersistence.loadControlOnlyRouteBootstrap(app))
    }

    @Test fun disabledBootstrapDoesNotRunOnSessionCreationOrHostResume() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        attach(owner(CarPlayNowPlaying(title = "Song", playing = true)))
        CarPlayMediaKeys.onHostResumed()
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("routeBootstrapEnabled=false"))
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("routeBootstrap=cancelled"))
    }

    @Test fun bootstrapOptInDoesNotApplyToLocalAudioSessions() {
        AirPlayPersistence.saveControlOnlyRouteBootstrap(app, true)
        attach(owner(CarPlayNowPlaying(title = "Song", playing = true)), disabled = false)
        CarPlayMediaKeys.onHostResumed()
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("controlOnly=false"))
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("routeBootstrap=cancelled"))
    }

    @Test fun focusExperimentDefaultsOff() {
        assertFalse(AirPlayPersistence.loadControlOnlyAudioFocus(app))
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        attach(owner())
        assertNoFocus()
    }

    @Test fun optedInFocusIsRequestedAndReleasedWithoutLocalAudio() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        AirPlayPersistence.saveControlOnlyAudioFocus(app, true)
        shadowOf(app.getSystemService(AudioManager::class.java))
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        val phone = owner(CarPlayNowPlaying(title = "Song", playing = true))
        attach(phone)
        assertTrue(session()!!.isActive)
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("focusHeld=true"))
        assertNotNull(ReflectionHelpers.getField<Any?>(CarPlayMediaKeys, "focusRequest"))
        CarPlayMediaKeys.detach(phone)
        assertNoFocus()
        assertNull(session())
    }

    @Test fun globalDisableWinsOverFocusExperiment() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        AirPlayPersistence.saveControlOnlyAudioFocus(app, true)
        AirPlayPersistence.saveDisableMediaKeyAudioFocus(app, true)
        attach(owner(CarPlayNowPlaying(playing = true)))
        assertNoFocus()
        assertTrue(session()!!.isActive)
    }

    @Test fun deniedFocusStillPublishesAndForwardsMediaControls() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        AirPlayPersistence.saveControlOnlyAudioFocus(app, true)
        shadowOf(app.getSystemService(AudioManager::class.java))
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        val phone = owner(CarPlayNowPlaying(title = "Song", playing = true))
        attach(phone)
        assertEquals(PlaybackState.STATE_PLAYING, state(session()!!).state)
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("focusHeld=false"))
        val handler = ReflectionHelpers.getField<Any>(session()!!, "mCallback")
        ReflectionHelpers.getField<MediaSession.Callback>(handler, "mCallback").onSkipToNext()
        verify(phone).sendMediaButton(CarPlayMediaButton.NEXT)
        assertTrue(CarPlayMediaKeys.diagnosticSnapshot().contains("forwardedKeys=1"))
    }

    @Test fun experimentChangesWaitForReconnectAndTurningOffRestoresNoFocus() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner()
        attach(phone)
        AirPlayPersistence.saveControlOnlyAudioFocus(app, true)
        attach(phone)
        assertNoFocus()
        shadowOf(app.getSystemService(AudioManager::class.java))
            .setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        attach(owner())
        assertNotNull(ReflectionHelpers.getField<Any?>(CarPlayMediaKeys, "focusRequest"))
        AirPlayPersistence.saveControlOnlyAudioFocus(app, false)
        attach(owner())
        assertNoFocus()
    }

    @Test @Config(sdk = [33]) fun pinyinOnlyChangesPublishedTitlesAndArtists() {
        AirPlayPersistence.savePinyinMetadata(app, true)
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val original = CarPlayNowPlaying(title = "晴天", artist = "周杰伦", album = "叶惠美", durationMillis = 12000)
        attach(owner(original))
        val published = metadata(session()!!)
        assertEquals("Qing Tian", published.getString(MediaMetadata.METADATA_KEY_TITLE))
        assertEquals("Zhou Jie Lun", published.getString(MediaMetadata.METADATA_KEY_ARTIST))
        assertEquals(published.getString(MediaMetadata.METADATA_KEY_TITLE),
            published.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE))
        assertEquals(published.getString(MediaMetadata.METADATA_KEY_ARTIST),
            published.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE))
        assertEquals("叶惠美", published.getString(MediaMetadata.METADATA_KEY_ALBUM))
        assertEquals("晴天", original.title)
        assertEquals(12000L, published.getLong(MediaMetadata.METADATA_KEY_DURATION))
    }
    @Test @Config(sdk = [33]) fun optionalAlbumAndArtistAliasOnlyChangePublishedMetadata() {
        AirPlayPersistence.savePinyinMetadata(app, true)
        AirPlayPersistence.savePinyinAlbum(app, true)
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        ArtistNameOverrides.save(app, mapOf("想想XiangXiang" to "Xiang Xiang"))
        val original = CarPlayNowPlaying(title = "晴天", artist = "想想XiangXiang", album = "音乐")
        attach(owner(original))
        assertEquals("Xiang Xiang", metadata(session()!!).getString(MediaMetadata.METADATA_KEY_ARTIST))
        assertEquals("Yin Yue", metadata(session()!!).getString(MediaMetadata.METADATA_KEY_ALBUM))
        assertEquals("想想XiangXiang", original.artist)
        assertEquals("音乐", original.album)
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
        assertEquals("Song", metadata(current).getString(MediaMetadata.METADATA_KEY_TITLE))
        assertEquals(PlaybackState.STATE_PAUSED, state(current).state)
        assertNoFocus()
        CarPlayMediaKeys.onMediaAudioChanged(true)
        CarPlayMediaKeys.onMediaAudioChanged(false)
        idle()
        assertSame(current, session())
        assertEquals(PlaybackState.STATE_PAUSED, state(current).state)
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
        assertEquals("Next", metadata(session()!!).getString(MediaMetadata.METADATA_KEY_TITLE))
        assertEquals(PlaybackState.STATE_PLAYING, state(session()!!).state)
        assertNoFocus()
    }
    @Test fun controlsUseExistingCarPlayHidAndDetachReleasesSession() {
        AirPlayPersistence.saveControlOnlyMediaSession(app, true)
        val phone = owner()
        attach(phone)
        val current = session()!!
        val handler = ReflectionHelpers.getField<Any>(current, "mCallback")
        val callback = ReflectionHelpers.getField<MediaSession.Callback>(handler, "mCallback")
        with(callback) { onPlay(); onPause(); onSkipToNext(); onSkipToPrevious() }
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
