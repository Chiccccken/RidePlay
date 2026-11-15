package com.shilapi.xcertplay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Historical media-button fallback; never starts a CarPlay connection on its own. */
class CarPlayMediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MEDIA_BUTTON) CarPlayMediaKeys.onMediaButtonBroadcast(intent)
    }
}
