package org.shilpo.peerless.ui.posture

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WindowPostureKind {
    FLAT,
    BOOK,
    TABLETOP
}

data class NormalizedWindowBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class WindowPosture(
    val kind: WindowPostureKind = WindowPostureKind.FLAT,
    val hingeBounds: NormalizedWindowBounds? = null
) {
    companion object {
        val Flat = WindowPosture()
    }
}

interface WindowPostureProvider {
    val posture: StateFlow<WindowPosture>
}

class FlatWindowPostureProvider : WindowPostureProvider {
    override val posture: StateFlow<WindowPosture> =
        MutableStateFlow(WindowPosture.Flat).asStateFlow()
}

val LocalWindowPostureProvider = staticCompositionLocalOf<WindowPostureProvider> {
    error("No WindowPostureProvider provided")
}
