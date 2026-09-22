package org.shilpo.peerless.ui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.SessionState
import org.shilpo.peerless.model.MeResponse
import org.shilpo.peerless.model.ServerHealthDto
import org.shilpo.peerless.model.SessionDto
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeLargeTypography
import org.shilpo.peerless.theme.SpecBadgeTypography
import org.shilpo.peerless.theme.SquircleShapeSmall
import org.shilpo.peerless.theme.ambientGlow
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons

private fun formatUptime(uptimeSeconds: Long): String {
    if (uptimeSeconds <= 0L) return "0m"
    val days = uptimeSeconds / 86400
    val hours = (uptimeSeconds % 86400) / 3600
    val minutes = (uptimeSeconds % 3600) / 60
    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0 || days > 0) append("${hours}h ")
        append("${minutes}m")
    }.trim()
}

private fun formatBytesToMb(bytes: Long): String {
    if (bytes <= 0L) return "0.0"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 10) {
        val rounded = (mb * 10).toLong() / 10.0
        rounded.toString()
    } else {
        mb.toLong().toString()
    }
}

@Composable
fun ProfileScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionManager = LocalSessionManager.current
    val apiClient = LocalPeerlessApiClient.current
    val sessionState by sessionManager.sessionState.collectAsState()
    val scope = rememberCoroutineScope()

    val currentUser = (sessionState as? SessionState.Authenticated)?.user
    val serverUrl = (sessionState as? SessionState.Authenticated)?.serverUrl ?: apiClient.baseUrl

    var serverHealth by remember { mutableStateOf<ServerHealthDto?>(null) }
    var isHealthLoading by remember { mutableStateOf(true) }

    var meResponse by remember { mutableStateOf<MeResponse?>(null) }
    var isMeLoading by remember { mutableStateOf(true) }

    var showDisconnectConfirmation by remember { mutableStateOf(false) }

    val refreshTelemetry: () -> Unit = {
        scope.launch {
            isHealthLoading = true
            apiClient.getServerHealth()
                .onSuccess { serverHealth = it }
            isHealthLoading = false
        }
        scope.launch {
            isMeLoading = true
            apiClient.getMe()
                .onSuccess { meResponse = it }
            isMeLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshTelemetry()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .ambientGlow(
                primaryGlow = MaterialTheme.colorScheme.primary,
                secondaryGlow = MaterialTheme.colorScheme.secondary,
                tertiaryGlow = MaterialTheme.colorScheme.tertiary,
                glowAlpha = 0.22f
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(SquircleShapeSmall)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "ACCOUNT & TELEMETRY",
                        style = ExpressiveTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    PeerlessIcon(
                        icon = PeerlessIcons.Close,
                        contentDescription = "Close Profile",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val initial = currentUser?.displayName?.firstOrNull()?.uppercase() ?: "P"
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(Color.White.copy(alpha = 0.5f), Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                style = ExpressiveTypography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentUser?.displayName ?: "Telegram Listener",
                                style = ExpressiveTypography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                                            PillShape
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PeerlessIcon(
                                        icon = PeerlessIcons.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "TG ID: ${currentUser?.telegram_id ?: "—"}",
                                        style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                if (!currentUser?.username.isNullOrBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer)
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                PillShape
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "@${currentUser.username}",
                                            style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Connected to $serverUrl",
                                style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PeerlessIcon(
                                    icon = PeerlessIcons.Speed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "SERVER HEALTH TELEMETRY",
                                    style = SpecBadgeLargeTypography.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isHealthLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    val isHealthy =
                                        serverHealth?.status?.equals(
                                            "healthy",
                                            ignoreCase = true
                                        ) == true ||
                                                serverHealth?.status?.equals(
                                                    "ok",
                                                    ignoreCase = true
                                                ) == true
                                    val badgeColor =
                                        if (isHealthy) Color(0xFF69F0AE) else MaterialTheme.colorScheme.tertiary
                                    val badgeText = if (isHealthy) "HEALTHY" else "DEGRADED"

                                    Row(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(badgeColor.copy(alpha = 0.16f))
                                            .border(1.dp, badgeColor.copy(alpha = 0.5f), PillShape)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Text(
                                            text = badgeText,
                                            style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = refreshTelemetry,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    PeerlessIcon(
                                        icon = PeerlessIcons.Compass,
                                        contentDescription = "Refresh telemetry",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TelemetryMetricCard(
                                    title = "AUDIO WORKERS",
                                    value = "${serverHealth?.workers_available ?: 0} / ${serverHealth?.workers_total ?: 0}",
                                    subtitle = "Least-Loaded Pool (ADR 0007)",
                                    icon = PeerlessIcons.Dns,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f)
                                )

                                TelemetryMetricCard(
                                    title = "CHUNK LRU CACHE",
                                    value = "${serverHealth?.cache_entries ?: 0} entries",
                                    subtitle = "${formatBytesToMb(serverHealth?.cache_bytes ?: 0L)} MB memory",
                                    icon = PeerlessIcons.Memory,
                                    accentColor = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            TelemetryMetricCard(
                                title = "SERVER UPTIME",
                                value = formatUptime(serverHealth?.uptime_seconds ?: 0L),
                                subtitle = "Continuous lossless daemon streaming uptime",
                                icon = PeerlessIcons.LosslessWave,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PeerlessIcon(
                                    icon = PeerlessIcons.Devices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ACTIVE DEVICE SESSIONS",
                                    style = SpecBadgeLargeTypography.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.sp
                                )
                            }

                            val sessionCount = meResponse?.sessions?.size ?: 1
                            Row(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        PillShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$sessionCount ACTIVE",
                                    style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val sessions = meResponse?.sessions ?: emptyList()
                        if (sessions.isEmpty()) {
                            SessionRowItem(
                                session = SessionDto(
                                    id = "current",
                                    device_name = "Current Device",
                                    platform = "Native Multiplatform Client",
                                    last_active_at = "Active now"
                                ),
                                isCurrent = true
                            )
                        } else {
                            sessions.forEachIndexed { index, sess ->
                                SessionRowItem(
                                    session = sess,
                                    isCurrent = index == 0
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF221113))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color(0xFFE57373).copy(alpha = 0.5f),
                                    Color(0xFFB71C1C).copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DANGER ZONE",
                                style = SpecBadgeLargeTypography.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350),
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "Zero-Knowledge Session Termination. Purges all stored tokens, credentials, and configured daemon endpoints from this device. Reverts client to fresh onboarding gateway.",
                            style = ExpressiveTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { showDisconnectConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = SquircleShapeSmall,
                            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF5350)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PeerlessIcon(
                                    icon = PeerlessIcons.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Disconnect & Forget Server",
                                    style = ExpressiveTypography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (showDisconnectConfirmation) {
            AlertDialog(
                onDismissRequest = { showDisconnectConfirmation = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF5350)
                        )
                        Text(
                            text = "Disconnect and Forget Server?",
                            style = ExpressiveTypography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Text(
                        text = "This will immediately invalidate your session on the daemon, clear the secure hardware keystore, and erase the server URL. You will return to the onboarding gateway.",
                        style = ExpressiveTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDisconnectConfirmation = false
                            scope.launch {
                                sessionManager.logout()
                                onClose()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = SquircleShapeSmall
                    ) {
                        Text(
                            text = "Confirm Disconnect",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDisconnectConfirmation = false },
                        shape = SquircleShapeSmall
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: DrawableResource,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(SquircleShapeSmall)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                SquircleShapeSmall
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PeerlessIcon(
                    icon = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = SpecBadgeTypography.copy(fontSize = 8.sp),
                    color = accentColor,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = value,
                style = ExpressiveTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = ExpressiveTypography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SessionRowItem(
    session: SessionDto,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SquircleShapeSmall)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                SquircleShapeSmall
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = session.effectiveDevice,
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isCurrent) {
                        Row(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "THIS DEVICE",
                                style = SpecBadgeTypography.copy(fontSize = 7.5.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val details = buildList {
                    add(session.effectivePlatform)
                    if (!session.effectiveIp.isNullOrBlank()) add("IP: ${session.effectiveIp}")
                    if (!session.user_agent.isNullOrBlank()) add(session.user_agent)
                    if (!session.effectiveTimestamp.isNullOrBlank()) add(session.effectiveTimestamp)
                }.joinToString(" • ")

                Text(
                    text = details,
                    style = ExpressiveTypography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF69F0AE))
            )
        }
    }
}
