package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.shilpo.peerless.model.ActiveRipTask
import org.shilpo.peerless.model.RipStage
import org.shilpo.peerless.network.LocalPeerlessApiClient
import org.shilpo.peerless.tasks.LocalRipCoordinator
import org.shilpo.peerless.theme.ArtworkShape
import org.shilpo.peerless.theme.ExpressiveTypography
import org.shilpo.peerless.theme.PillShape
import org.shilpo.peerless.theme.SpecBadgeTypography

@Composable
fun RipActivityPane(
    modifier: Modifier = Modifier
) {
    val ripCoordinator = LocalRipCoordinator.current
    val apiClient = LocalPeerlessApiClient.current
    val coroutineScope = rememberCoroutineScope()

    val activeTasksMap by (ripCoordinator?.activeTasks
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap()) }).collectAsState()

    val taskList = remember(activeTasksMap) {
        activeTasksMap.values
            .filterNot { it.isFinished }
            .sortedByDescending { it.taskId }
    }

    if (taskList.isEmpty()) {
        RipEmptyState(modifier = modifier)
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Live Rips",
                    style = ExpressiveTypography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = taskList,
                    key = { it.taskId }
                ) { task ->
                    RipTaskRow(
                        task = task,
                        artworkUrl = apiClient.getArtworkUrl(task.track, 120),
                        onCancel = {
                            coroutineScope.launch { ripCoordinator?.cancelRip(task.taskId) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RipTaskRow(
    task: ActiveRipTask,
    artworkUrl: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val targetProgress = (task.percent / 100f).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 250),
        label = "RipProgressAnim"
    )

    val hasApple = task.track.provider.contains("apple", ignoreCase = true)
    val hasQobuz = task.track.provider.contains("qobuz", ignoreCase = true)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork - identical to TrackRow (52.dp squircle)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(ArtworkShape)
                .background(colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            PeerlessIcon(
                icon = PeerlessIcons.MusicNote,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = PeerlessIcons.RipCloudSync,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center: Title, Artist, Stage badge, Progress indicator
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = task.track.title,
                style = ExpressiveTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${task.track.artist} • ${task.track.album}",
                style = ExpressiveTypography.bodySmall.copy(fontSize = 13.sp),
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (hasApple) {
                    PeerlessIcon(
                        icon = PeerlessIcons.AppleLogo,
                        contentDescription = "Apple Music",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp)
                    )
                } else if (hasQobuz) {
                    PeerlessIcon(
                        icon = PeerlessIcons.QobuzLogo,
                        contentDescription = "Qobuz",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.height(11.dp).width(28.dp)
                    )
                } else if (task.track.provider.isNotEmpty()) {
                    Text(
                        text = formatProviderLabel(task.track.provider),
                        style = SpecBadgeTypography.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                RipStageBadge(stage = task.stage)

                if (!task.speed.isNullOrBlank()) {
                    Text(
                        text = "• ${task.speed}",
                        style = SpecBadgeTypography.copy(fontSize = 9.sp),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(PillShape),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Trailing status & actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${task.percent.toInt()}%",
                style = SpecBadgeTypography.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )

            if (task.isOwner) {
                TextButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Cancel", style = ExpressiveTypography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun RipStageBadge(
    stage: RipStage,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val (bgColor, fgColor) = when (stage) {
        RipStage.QUEUED -> colorScheme.surfaceContainerHigh to colorScheme.onSurfaceVariant
        RipStage.DOWNLOADING -> colorScheme.primaryContainer to colorScheme.onPrimaryContainer
        RipStage.DECRYPTING -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
        RipStage.TAGGING -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
        RipStage.UPLOADING -> colorScheme.primaryContainer.copy(alpha = 0.7f) to colorScheme.primary
        RipStage.COMPLETED -> Color(0xFF1B5E20).copy(alpha = 0.2f) to Color(0xFF4CAF50)
        RipStage.CANCELLED -> colorScheme.surfaceContainerHighest to colorScheme.onSurfaceVariant.copy(
            alpha = 0.6f
        )

        RipStage.ERROR -> colorScheme.errorContainer to colorScheme.onErrorContainer
    }

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(fgColor)
            )
            Text(
                text = stage.displayName,
                style = SpecBadgeTypography.copy(fontSize = 9.sp),
                fontWeight = FontWeight.SemiBold,
                color = fgColor
            )
        }
    }
}

@Composable
fun RipEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PeerlessIcon(
                icon = PeerlessIcons.RipCloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "No active rips",
                style = ExpressiveTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
