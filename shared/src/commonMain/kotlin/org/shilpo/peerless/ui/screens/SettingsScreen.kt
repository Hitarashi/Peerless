package org.shilpo.peerless.ui.screens


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.SquircleShapeMedium
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons

@Composable
fun SettingsScreen(
    serverConnected: Boolean,
    onOpenSettingsDialog: () -> Unit,
    onOpenProfile: () -> Unit,
    contentBottomPadding: Dp
) {
    val apiClient = LocalPeerlessApiClient.current
    val serverUrl = apiClient.baseUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = contentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Configuration",
            style = ExpressiveTypography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Telegram Account & Telemetry Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), SquircleShapeMedium)
                .clickable { onOpenProfile() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Telegram Account & Telemetry",
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View active device sessions, daemon metrics, and logout",
                            style = ExpressiveTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PeerlessIcon(
                    icon = PeerlessIcons.OpenInNew,
                    contentDescription = "Open profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SquircleShapeMedium)
                .clickable { onOpenSettingsDialog() }
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Streaming Daemon",
                        style = ExpressiveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (serverConnected) "ONLINE" else "DISCONNECTED",
                        style = SpecBadgeTypography,
                        color = if (serverConnected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                    )
                }

                Text(
                    text = if (serverUrl.isNotBlank()) "Endpoint: $serverUrl" else "Endpoint: Unconfigured",
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Verified Session & HMAC Ticket Auth Active",
                    style = ExpressiveTypography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShapeMedium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, SquircleShapeMedium)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Audio Engine Capabilities",
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• Hi-Res PCM 24-bit / 192kHz output\n• FLAC & Apple Lossless (ALAC) gapless playback\n• Poweramp Signal Path Real-Time Inspector",
                    style = ExpressiveTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
