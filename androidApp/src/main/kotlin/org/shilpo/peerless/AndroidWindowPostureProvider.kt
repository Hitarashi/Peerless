package org.shilpo.peerless

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.shilpo.peerless.ui.posture.NormalizedWindowBounds
import org.shilpo.peerless.ui.posture.WindowPosture
import org.shilpo.peerless.ui.posture.WindowPostureKind
import org.shilpo.peerless.ui.posture.WindowPostureProvider

class AndroidWindowPostureProvider(
    activity: ComponentActivity
) : WindowPostureProvider {
    private val _posture = MutableStateFlow(WindowPosture.Flat)
    override val posture: StateFlow<WindowPosture> = _posture.asStateFlow()

    init {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(activity)
                    .windowLayoutInfo(activity)
                    .collect { layoutInfo ->
                        val feature = layoutInfo.displayFeatures
                            .filterIsInstance<FoldingFeature>()
                            .firstOrNull { it.state == FoldingFeature.State.HALF_OPENED }
                        if (feature == null) {
                            _posture.value = WindowPosture.Flat
                            return@collect
                        }

                        val bounds = WindowMetricsCalculator.getOrCreate()
                            .computeCurrentWindowMetrics(activity).bounds
                        val width = bounds.width().coerceAtLeast(1).toFloat()
                        val height = bounds.height().coerceAtLeast(1).toFloat()
                        val fold = feature.bounds
                        _posture.value = WindowPosture(
                            kind = when (feature.orientation) {
                                FoldingFeature.Orientation.VERTICAL -> WindowPostureKind.BOOK
                                FoldingFeature.Orientation.HORIZONTAL -> WindowPostureKind.TABLETOP
                                else -> WindowPostureKind.FLAT
                            },
                            hingeBounds = NormalizedWindowBounds(
                                left = ((fold.left - bounds.left) / width).coerceIn(0f, 1f),
                                top = ((fold.top - bounds.top) / height).coerceIn(0f, 1f),
                                right = ((fold.right - bounds.left) / width).coerceIn(0f, 1f),
                                bottom = ((fold.bottom - bounds.top) / height).coerceIn(0f, 1f)
                            )
                        )
                    }
            }
        }
    }
}
