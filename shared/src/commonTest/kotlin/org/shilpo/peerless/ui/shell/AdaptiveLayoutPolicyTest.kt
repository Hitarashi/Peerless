package org.shilpo.peerless.ui.shell

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveLayoutPolicyTest {
    @Test
    fun supportingPaneWaitsUntilBothColumnsFit() {
        assertFalse(supportingPaneLayout(1111.dp, 340.dp).isAvailable)
        assertTrue(supportingPaneLayout(1112.dp, 340.dp).isAvailable)
        assertEquals(280.dp, supportingPaneLayout(1112.dp, 340.dp).width)
    }

    @Test
    fun supportingPaneUsesRemainingWidthAndClampsAtMaximum() {
        assertEquals(300.dp, supportingPaneLayout(1132.dp, 340.dp).width)
        assertEquals(340.dp, supportingPaneLayout(1172.dp, 340.dp).width)
        assertEquals(560.dp, supportingPaneLayout(1920.dp, 600.dp).width)
    }
}
