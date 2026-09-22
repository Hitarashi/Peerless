package org.shilpo.peerless.ui.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.shilpo.peerless.auth.LocalSessionManager
import org.shilpo.peerless.ui.components.PeerlessIcon
import org.shilpo.peerless.ui.components.PeerlessIcons
import kotlin.math.roundToInt

val LastFmRed = Color(0xFFD51007)

@Composable
fun ScrobbleWaveformGraphic(
    modifier: Modifier = Modifier,
    barColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScrobbleWaveformAnimation")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 24f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 28f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h4"
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h5"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { barHeight ->
            Surface(
                modifier = Modifier
                    .width(4.5.dp)
                    .height(barHeight.dp),
                shape = CircleShape,
                color = barColor
            ) {}
        }
    }
}

@Composable
fun LastFmLoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onLogin: (suspend (username: String, password: String) -> Result<Unit>)? = null
) {
    val sessionManager = LocalSessionManager.current
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }

    val shakeOffset = remember { Animatable(0f) }

    val isFormValid = username.isNotBlank() && password.isNotBlank()

    LaunchedEffect(Unit) {
        usernameFocusRequester.requestFocus()
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    0f at 0
                    -12f at 60
                    12f at 120
                    -8f at 180
                    8f at 240
                    -4f at 300
                    0f at 360
                }
            )
        }
    }

    val submitLogin: () -> Unit = {
        if (isFormValid && !isLoggingIn) {
            keyboardController?.hide()
            focusManager.clearFocus()
            val submittedUsername = username.trim()
            val submittedPassword = password
            isLoggingIn = true
            errorMessage = null
            scope.launch {
                try {
                    val result = if (onLogin != null) {
                        onLogin(submittedUsername, submittedPassword)
                    } else {
                        delay(600)
                        Result.success(Unit)
                    }

                    result.onSuccess {
                        onLoginSuccess?.invoke()
                    }.onFailure { err ->
                        errorMessage = actionableLastFmError(err)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    errorMessage = actionableLastFmError(e)
                } finally {
                    isLoggingIn = false
                }
            }
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            val isExpanded = maxWidth >= 900.dp

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-120).dp)
                    .size(if (isExpanded) 420.dp else 260.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = LastFmRed.copy(alpha = 0.10f)
                ) {}
            }

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 980.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isExpanded) 40.dp else 20.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpanded) {
                    LastFmValuePanel(modifier = Modifier.weight(1f))
                }

                Column(
                    modifier = if (isExpanded) Modifier.width(440.dp) else Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (!isExpanded) {
                        LastFmBrandMark(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Connect Last.fm",
                            style = if (isExpanded) MaterialTheme.typography.headlineLarge
                            else MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Last.fm is required to build Your rotation, Listen again, and music discovery based on your taste. Connect your account to continue.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                    ) {
                        errorMessage?.let { errorText ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { liveRegion = LiveRegionMode.Polite }
                                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        start = 16.dp,
                                        top = 12.dp,
                                        bottom = 12.dp,
                                        end = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PeerlessIcon(
                                        PeerlessIcons.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        errorText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { errorMessage = null }) {
                                        PeerlessIcon(
                                            PeerlessIcons.Close,
                                            contentDescription = "Dismiss error"
                                        )
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
                            OutlinedTextField(
                                value = username,
                                onValueChange = { value ->
                                    username = value
                                    errorMessage = null
                                },
                                enabled = !isLoggingIn,
                                label = { Text("Username or email") },
                                placeholder = { Text("Your Last.fm account") },
                                leadingIcon = {
                                    PeerlessIcon(
                                        icon = PeerlessIcons.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (username.isNotBlank()) {
                                        IconButton(
                                            onClick = { username = "" },
                                            enabled = !isLoggingIn
                                        ) {
                                            PeerlessIcon(
                                                icon = PeerlessIcons.Close,
                                                contentDescription = "Clear username",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                isError = errorMessage != null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { passwordFocusRequester.requestFocus() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(usernameFocusRequester),
                                shape = MaterialTheme.shapes.large
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { value ->
                                    password = value
                                    errorMessage = null
                                },
                                enabled = !isLoggingIn,
                                label = { Text("Password") },
                                placeholder = { Text("Enter your password") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = {
                                    PeerlessIcon(
                                        icon = PeerlessIcons.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        enabled = !isLoggingIn
                                    ) {
                                        PeerlessIcon(
                                            icon = if (passwordVisible) PeerlessIcons.VisibilityOff else PeerlessIcons.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                isError = errorMessage != null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { submitLogin() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(passwordFocusRequester),
                                shape = MaterialTheme.shapes.large
                            )

                            Button(
                                onClick = submitLogin,
                                enabled = isFormValid && !isLoggingIn,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (isLoggingIn) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PeerlessIcon(
                                            icon = PeerlessIcons.Sparkle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Connect Last.fm",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            TextButton(
                                onClick = { uriHandler.openUri("https://www.last.fm/join") },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("New to Last.fm? Create an account")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PeerlessIcon(
                                    icon = PeerlessIcons.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Your credentials are used only to authorize Last.fm.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                if (onLogout != null) onLogout() else sessionManager.logout()
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        shape = CircleShape,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        PeerlessIcon(
                            icon = PeerlessIcons.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Use a different Telegram account",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

internal fun actionableLastFmError(error: Throwable): String {
    val message = error.message.orEmpty().lowercase()
    return when {
        listOf("invalid", "incorrect", "credential", "password", "unauthorized", "authorize")
            .any(message::contains) ->
            "Last.fm didn't accept these credentials. Check the username and password, then try again."

        listOf("timeout", "network", "connect", "socket", "host", "resolve", "unreachable")
            .any(message::contains) ->
            "Couldn't reach Peerless or Last.fm. Check your internet connection and retry."

        listOf("502", "503", "service unavailable", "internal server error")
            .any(message::contains) ->
            "The Last.fm sign-in service is temporarily unavailable. Try again in a moment."

        else ->
            "Last.fm sign-in failed. Check your credentials and connection, then try again."
    }
}

@Composable
private fun LastFmBrandMark(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(72.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = LastFmRed,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScrobbleWaveformGraphic(modifier = Modifier.height(24.dp), barColor = Color.White)
            Spacer(Modifier.height(3.dp))
            Text(
                text = "last.fm",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun LastFmValuePanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(28.dp)) {
        LastFmBrandMark()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Your music, remembered.",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Last.fm is required to build Your rotation, Listen again, and recommendations based on your taste. Once connected, Peerless can also scrobble your playback.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 400.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LastFmBenefit("Automatic scrobbling")
            LastFmBenefit("Listening history across your devices")
        }
    }
}

@Composable
private fun LastFmBenefit(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                PeerlessIcon(
                    icon = PeerlessIcons.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
