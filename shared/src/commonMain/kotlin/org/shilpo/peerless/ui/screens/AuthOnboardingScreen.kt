package org.shilpo.peerless.ui.screens


import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.auth.SessionState
import org.shilpo.peerless.readPlainText
import org.shilpo.peerless.theme.ExpressiveMotion
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons

@Composable
fun AuthOnboardingScreen(modifier: Modifier = Modifier) {
    val sessionManager = LocalSessionManager.current
    val sessionState by sessionManager.sessionState.collectAsState()
    val isLoading = sessionState is SessionState.Loading
    val uriHandler = LocalUriHandler.current

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var connectionKey by remember { mutableStateOf("") }
    var manualServerUrl by remember { mutableStateOf("") }
    var manualOtpCode by remember { mutableStateOf("") }
    var isManualConfigExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val manualExpandRotation by animateFloatAsState(
        targetValue = if (isManualConfigExpanded) 180f else 0f,
        animationSpec = tween(250, easing = ExpressiveMotion.EmphasizedEasing),
        label = "ManualConfigExpandRotation"
    )

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val isExpanded = maxWidth >= 900.dp

            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-120).dp, y = (-180).dp)
                    .size(if (isExpanded) 500.dp else 300.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            ) {}

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 1040.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isExpanded) 40.dp else 20.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpanded) PeerlessWelcomePanel(modifier = Modifier.weight(1f))

                Column(
                    modifier = if (isExpanded) Modifier.width(460.dp) else Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (!isExpanded) {
                        PeerlessBrandMark(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Connect Peerless",
                            style = if (isExpanded) MaterialTheme.typography.headlineLarge
                            else MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pair this device with your private music server.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        errorMessage?.let { error ->
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PeerlessIcon(
                                        PeerlessIcons.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { errorMessage = null }) {
                                        PeerlessIcon(PeerlessIcons.Close, contentDescription = "Dismiss error")
                                    }
                                }
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            ConnectionSectionHeader(
                                icon = PeerlessIcons.Compass,
                                title = "Connect with Telegram",
                                supportingText = "Recommended"
                            )
                            Text(
                                text = "In Telegram, send /stream to your Peerless bot, then choose Open Peerless.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { uriHandler.openUri("https://t.me") },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = CircleShape
                            ) {
                                Text("Open Telegram")
                                Spacer(Modifier.width(8.dp))
                                PeerlessIcon(
                                    PeerlessIcons.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            ConnectionSectionHeader(
                                icon = PeerlessIcons.Key,
                                title = "Use a connection key",
                                supportingText = "Paste an auth link or Base64 key"
                            )
                            OutlinedTextField(
                                value = connectionKey,
                                onValueChange = {
                                    connectionKey = it
                                    errorMessage = null
                                },
                                label = { Text("Connection key") },
                                placeholder = { Text("peerless://auth?data=…") },
                                leadingIcon = {
                                    PeerlessIcon(
                                        PeerlessIcons.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val text = clipboard.readPlainText()
                                                if (!text.isNullOrBlank()) {
                                                    connectionKey = text.trim()
                                                    errorMessage = null
                                                }
                                            }
                                        }
                                    ) {
                                        PeerlessIcon(
                                            PeerlessIcons.ContentPaste,
                                            contentDescription = "Paste connection key"
                                        )
                                    }
                                },
                                minLines = 1,
                                maxLines = 3,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        errorMessage = null
                                        sessionManager.connectWithPayload(connectionKey.trim()).onFailure { error ->
                                            errorMessage = error.message ?: "Could not connect with this key"
                                        }
                                    }
                                },
                                enabled = connectionKey.isNotBlank() && !isLoading,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = CircleShape
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Text("Connect with key")
                                }
                            }
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = { isManualConfigExpanded = !isManualConfigExpanded },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                PeerlessIcon(
                                    PeerlessIcons.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Manual server setup",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start
                                )
                                PeerlessIcon(
                                    PeerlessIcons.ExpandMore,
                                    contentDescription = if (isManualConfigExpanded) "Collapse" else "Expand",
                                    modifier = Modifier.rotate(manualExpandRotation)
                                )
                            }

                            AnimatedVisibility(
                                visible = isManualConfigExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Enter your server address and the one-time code from Telegram.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    OutlinedTextField(
                                        value = manualServerUrl,
                                        onValueChange = {
                                            manualServerUrl = it
                                            errorMessage = null
                                        },
                                        label = { Text("Server address") },
                                        placeholder = { Text("https://peerless.example.com") },
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = manualOtpCode,
                                        onValueChange = {
                                            manualOtpCode = it
                                            errorMessage = null
                                        },
                                        label = { Text("One-time code") },
                                        placeholder = { Text("Code from /stream") },
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                errorMessage = null
                                                sessionManager.connectManual(
                                                    serverUrl = manualServerUrl.trim(),
                                                    code = manualOtpCode.trim()
                                                ).onFailure { error ->
                                                    errorMessage = error.message ?: "Could not connect to this server"
                                                }
                                            }
                                        },
                                        enabled = manualServerUrl.isNotBlank() && manualOtpCode.isNotBlank() && !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = CircleShape
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text("Connect to server")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeerlessIcon(
                            PeerlessIcons.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Your session token is stored securely on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeerlessBrandMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(72.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            PeerlessIcon(PeerlessIcons.MusicNote, contentDescription = "Peerless", modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun PeerlessWelcomePanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        PeerlessBrandMark()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Your music. Your cloud.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pair Peerless with Telegram to stream your lossless library privately on every device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 400.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WelcomeBenefit(PeerlessIcons.LosslessWave, "Bit-perfect lossless streaming")
            WelcomeBenefit(PeerlessIcons.Devices, "One library across your devices")
            WelcomeBenefit(PeerlessIcons.Lock, "Private, token-based access")
        }
    }
}

@Composable
private fun WelcomeBenefit(icon: DrawableResource, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                PeerlessIcon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ConnectionSectionHeader(icon: DrawableResource, title: String, supportingText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                PeerlessIcon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
