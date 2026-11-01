package com.shilapi.xcertplay

import android.graphics.BitmapFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RidePlayCarButtonTest {
    @Test fun builtInNameAndTransmittedIconUseKtm() {
        val app = RuntimeEnvironment.getApplication()
        assertEquals("KTM", AirPlayPersistence.loadOemLabel(app))
        val bytes = RidePlayCarButton.encoded(app)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)!!
        assertEquals(256, decoded.width)
        assertEquals(0xffff6600.toInt(), decoded.getPixel(0, 0))
        decoded.recycle()
        AirPlayPersistence.saveOemLabel(app, "My motorcycle")
        assertEquals("My motorcycle", AirPlayPersistence.loadOemLabel(app))
    }
}
