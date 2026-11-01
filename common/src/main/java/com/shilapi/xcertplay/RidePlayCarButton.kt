package com.shilapi.xcertplay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.shilapi.xcertplay.host.R
import java.io.ByteArrayOutputStream

/** Shared built-in car-button artwork for the settings preview and CarPlay payload. */
internal object RidePlayCarButton {
    fun bitmap(context: Context): Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also {
        val icon = requireNotNull(context.getDrawable(R.drawable.ic_rideplay_ktm))
        icon.setBounds(0, 0, it.width, it.height)
        icon.draw(Canvas(it))
    }
    fun encoded(context: Context): ByteArray {
        val bitmap = bitmap(context)
        return try {
            ByteArrayOutputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                out.toByteArray()
            }
        } finally { bitmap.recycle() }
    }
}
