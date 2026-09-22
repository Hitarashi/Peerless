package org.shilpo.peerless.theme

import android.provider.Settings
import org.shilpo.peerless.auth.AndroidContextProvider

internal actual fun isReducedMotionEnabled(): Boolean {
    val context = AndroidContextProvider.context ?: return false
    return try {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    } catch (_: Exception) {
        false
    }
}
