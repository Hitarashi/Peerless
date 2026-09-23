package org.shilpo.peerless.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

val LocalToastNotifier = staticCompositionLocalOf<((String) -> Unit)?> { null }
