package org.shilpo.peerless.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.SessionState
import org.shilpo.peerless.theme.*
import org.shilpo.peerless.ui.components.PeerlessIcons

@Composable
fun AuthOnboardingScreen(
    modifier: Modifier = Modifier
) {
    val sessionManager = LocalSessionManager.current
    val sessionState by sessionManager.sessionState.collectAsState()
    val isLoading = sessionState is SessionState.Loading

    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var connectionKey by remember { mutableStateOf("") }
    var manualServerUrl by remember { mutableStateOf("") }
    var manualOtpCode by remember { mutableStateOf("") }
    var isManualConfigExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val manualExpandRotation by animateFloatAsState(
        targetValue = if (isManualConfigExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250, easing = ExpressiveMotion.EmphasizedEasing),
        label = "ManualConfigExpandRotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .ambientGlow(
                primaryGlow = PrimaryDark,
                secondaryGlow = SecondaryDark,
                tertiaryGlow = TertiaryDark,
                glowAlpha = 0.30f
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(SquircleShapeLarge)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryDark, TertiaryDark)
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.1f))
                            ),
                            shape = SquircleShapeLarge
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PeerlessIcons.MusicNote,
                        contentDescription = "Peerless Logo",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Text(
                    text = "PEERLESS LOSSLESS STREAM",
                    style = ExpressiveTypography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = OnSurfaceDark,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your private high-fidelity audio sanctuary",
                    style = ExpressiveTypography.bodyMedium,
                    color = OnSurfaceVariantDark,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SurfaceContainerDark.copy(alpha = 0.8f))
                        .border(1.dp, OutlineVariantDark.copy(alpha = 0.6f), PillShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(LosslessGold)
                    )
                    Text(
                        text = "24-BIT / 192KHZ DIRECT • TELEGRAM MTPROTO",
                        style = SpecBadgeTypography.copy(fontSize = 9.sp),
                        color = LosslessGold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Error Banner
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                errorMessage?.let { errorText ->
                    LiquidGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color(0xFF281114).copy(alpha = 0.90f),
                        borderBrush = Brush.verticalGradient(
                            listOf(Color(0xFFEF5350).copy(alpha = 0.7f), Color(0xFFB71C1C).copy(alpha = 0.3f))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    Icon(
                                        imageVector = PeerlessIcons.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "AUTHENTICATION FAILED",
                                        style = SpecBadgeTypography.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF5350)
                                    )
                                }

                                IconButton(
                                    onClick = { errorMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = PeerlessIcons.Close,
                                        contentDescription = "Dismiss error",
                                        tint = OnSurfaceVariantDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = errorText,
                                style = ExpressiveTypography.bodyMedium,
                                color = OnSurfaceDark
                            )

                            HorizontalDivider(color = Color(0xFFEF5350).copy(alpha = 0.25f))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Troubleshooting:",
                                    style = ExpressiveTypography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurfaceVariantDark
                                )
                                Text(
                                    text = "• Ensure peerless-server daemon is running and reachable.",
                                    style = ExpressiveTypography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = OnSurfaceVariantDark
                                )
                                Text(
                                    text = "• Verify Telegram OTP hasn't expired (valid for 5 minutes).",
                                    style = ExpressiveTypography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = OnSurfaceVariantDark
                                )
                                Text(
                                    text = "• Confirm connection key format is valid Base64 or auth deep-link.",
                                    style = ExpressiveTypography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = OnSurfaceVariantDark
                                )
                            }
                        }
                    }
                }
            }

            // Primary Action Card: Connect with Telegram
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                containerColor = LiquidGlassDefaults.ElevatedContainerColor,
                borderBrush = LiquidGlassDefaults.AccentBorderBrush
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecondaryDark.copy(alpha = 0.2f))
                                .border(1.dp, SecondaryDark.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PeerlessIcons.Compass,
                                contentDescription = null,
                                tint = SecondaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Connect with Telegram",
                                style = ExpressiveTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Text(
                                text = "Recommended • 1-tap seamless session",
                                style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                color = SecondaryDark
                            )
                        }
                    }

                    Text(
                        text = "Open Telegram, start a session with /stream, and tap Open Peerless to pair this device instantly.",
                        style = ExpressiveTypography.bodyMedium,
                        color = OnSurfaceVariantDark
                    )

                    Button(
                        onClick = { uriHandler.openUri("https://t.me") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = SquircleShapeMedium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryDark,
                            contentColor = Color(0xFF0E1A2E)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🚀 Open Telegram",
                                style = ExpressiveTypography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = PeerlessIcons.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Secondary Action Card: Connection Key
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                containerColor = LiquidGlassDefaults.ElevatedContainerColor,
                borderBrush = LiquidGlassDefaults.BorderBrush
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryDark.copy(alpha = 0.2f))
                                .border(1.dp, PrimaryDark.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = PeerlessIcons.Key,
                                contentDescription = null,
                                tint = PrimaryDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Connection Key",
                                style = ExpressiveTypography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                            Text(
                                text = "Paste Base64 payload or auth link",
                                style = SpecBadgeTypography.copy(fontSize = 8.5.sp),
                                color = PrimaryDark
                            )
                        }
                    }

                    OutlinedTextField(
                        value = connectionKey,
                        onValueChange = {
                            connectionKey = it
                            errorMessage = null
                        },
                        placeholder = {
                            Text(
                                text = "peerless://auth?data=... or Base64 key",
                                style = ExpressiveTypography.bodyMedium,
                                color = OnSurfaceVariantDark.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = SquircleShapeSmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDark,
                            unfocusedBorderColor = OutlineVariantDark.copy(alpha = 0.7f),
                            focusedContainerColor = SurfaceContainerDark.copy(alpha = 0.6f),
                            unfocusedContainerColor = SurfaceContainerDark.copy(alpha = 0.4f),
                            focusedTextColor = OnSurfaceDark,
                            unfocusedTextColor = OnSurfaceDark
                        ),
                        trailingIcon = {
                            if (connectionKey.isNotBlank()) {
                                IconButton(onClick = { connectionKey = "" }) {
                                    Icon(
                                        imageVector = PeerlessIcons.Close,
                                        contentDescription = "Clear key",
                                        tint = OnSurfaceVariantDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    connectionKey = clipText.trim()
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = SquircleShapeSmall,
                            border = BorderStroke(1.dp, OutlineVariantDark),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = OnSurfaceDark
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = PeerlessIcons.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "📋 Paste",
                                    style = ExpressiveTypography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (connectionKey.isNotBlank()) {
                                    scope.launch {
                                        errorMessage = null
                                        val result = sessionManager.connectWithPayload(connectionKey.trim())
                                        result.onFailure { err ->
                                            errorMessage = err.message ?: "Failed to connect with provided key"
                                        }
                                    }
                                }
                            },
                            enabled = connectionKey.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = SquircleShapeSmall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryDark,
                                contentColor = OnPrimaryDark,
                                disabledContainerColor = PrimaryDark.copy(alpha = 0.3f),
                                disabledContentColor = OnPrimaryDark.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = OnPrimaryDark
                                )
                            } else {
                                Text(
                                    text = "Connect",
                                    style = ExpressiveTypography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Section: Manual Server Configuration
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                containerColor = LiquidGlassDefaults.SubtleContainerColor,
                borderBrush = LiquidGlassDefaults.BorderBrush
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isManualConfigExpanded = !isManualConfigExpanded }
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = PeerlessIcons.Settings,
                                contentDescription = null,
                                tint = OnSurfaceVariantDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Manual Server Configuration",
                                style = ExpressiveTypography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = OnSurfaceDark
                            )
                        }

                        Icon(
                            imageVector = PeerlessIcons.ExpandMore,
                            contentDescription = if (isManualConfigExpanded) "Collapse" else "Expand",
                            tint = OnSurfaceVariantDark,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(manualExpandRotation)
                        )
                    }

                    AnimatedVisibility(
                        visible = isManualConfigExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Enter streaming daemon endpoint URL and single-use OTP generated via Telegram /stream.",
                                style = ExpressiveTypography.bodySmall,
                                color = OnSurfaceVariantDark
                            )

                            OutlinedTextField(
                                value = manualServerUrl,
                                onValueChange = {
                                    manualServerUrl = it
                                    errorMessage = null
                                },
                                label = { Text("Server URL") },
                                placeholder = { Text("e.g. http://192.168.1.100:4444") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = SquircleShapeSmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryDark,
                                    unfocusedBorderColor = OutlineVariantDark,
                                    focusedTextColor = OnSurfaceDark,
                                    unfocusedTextColor = OnSurfaceDark
                                )
                            )

                            OutlinedTextField(
                                value = manualOtpCode,
                                onValueChange = {
                                    manualOtpCode = it
                                    errorMessage = null
                                },
                                label = { Text("OTP Code") },
                                placeholder = { Text("6-character code from /stream") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = SquircleShapeSmall,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryDark,
                                    unfocusedBorderColor = OutlineVariantDark,
                                    focusedTextColor = OnSurfaceDark,
                                    unfocusedTextColor = OnSurfaceDark
                                )
                            )

                            Button(
                                onClick = {
                                    if (manualServerUrl.isNotBlank() && manualOtpCode.isNotBlank()) {
                                        scope.launch {
                                            errorMessage = null
                                            val result = sessionManager.connectManual(
                                                serverUrl = manualServerUrl.trim(),
                                                code = manualOtpCode.trim()
                                            )
                                            result.onFailure { err ->
                                                errorMessage = err.message ?: "Manual connection failed"
                                            }
                                        }
                                    }
                                },
                                enabled = manualServerUrl.isNotBlank() && manualOtpCode.isNotBlank() && !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = SquircleShapeSmall,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryDark.copy(alpha = 0.85f),
                                    contentColor = OnPrimaryDark
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = OnPrimaryDark
                                    )
                                } else {
                                    Text(
                                        text = "Connect with Server & Code",
                                        style = ExpressiveTypography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = PeerlessIcons.LosslessWave,
                    contentDescription = null,
                    tint = OnSurfaceVariantDark.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Zero-Knowledge Token Vault • Bit-Perfect Bitstream",
                    style = SpecBadgeTypography.copy(fontSize = 9.sp),
                    color = OnSurfaceVariantDark.copy(alpha = 0.6f)
                )
            }
        }
    }
}
