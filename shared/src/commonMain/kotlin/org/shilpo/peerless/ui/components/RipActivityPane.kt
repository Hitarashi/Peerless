@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
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
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
            ) {
                itemsIndexed(
                    items = taskList,
                    key = { _, task -> task.taskId }
                ) { index, task ->
                    RipTaskRow(
                        task = task,
                        artworkUrl = apiClient.getArtworkUrl(task.track, 120),
                        index = index,
                        count = taskList.size,
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
    index: Int = 0,
    count: Int = 1,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val targetProgress = (task.percent / 100f).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 250),
        label = "RipProgressAnim"
    )

    val itemShapes = ListItemDefaults.segmentedShapes(
        index = index.coerceIn(0, maxOf(0, count - 1)),
        count = maxOf(count, 1)
    )
    val itemColors = ListItemDefaults.segmentedColors(
        containerColor = colorScheme.surfaceContainer,
        selectedContainerColor = colorScheme.secondaryContainer
    )

    SegmentedListItem(
        selected = false,
        onClick = {},
        shapes = itemShapes,
        colors = itemColors,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(ArtworkShape)
                    .background(colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                PeerlessIcon(
                    icon = if (task.isAlbum) PeerlessIcons.Library else PeerlessIcons.MusicNote,
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
            }
        },
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (task.track.provider.contains("apple", ignoreCase = true)) {
                    PeerlessIcon(
                        icon = PeerlessIcons.AppleLogo,
                        contentDescription = "Apple Music",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "•",
                        style = ExpressiveTypography.titleMedium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else if (task.track.provider.contains("qobuz", ignoreCase = true)) {
                    PeerlessIcon(
                        icon = PeerlessIcons.QobuzLogo,
                        contentDescription = "Qobuz",
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.height(11.dp).width(26.dp)
                    )
                    Text(
                        text = "•",
                        style = ExpressiveTypography.titleMedium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else if (task.track.provider.isNotBlank()) {
                    Text(
                        text = formatProviderLabel(task.track.provider),
                        style = ExpressiveTypography.labelMedium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "•",
                        style = ExpressiveTypography.titleMedium,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = task.track.title,
                    style = ExpressiveTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        },
        supportingContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val subtitleText = if (task.isAlbum) {
                    buildString {
                        if (task.track.artist.isNotBlank()) {
                            append(task.track.artist)
                            append(" • ")
                        }
                        if (task.totalTracks != null) {
                            if (task.completedTracks != null && task.completedTracks > 0) {
                                append("${task.completedTracks}/${task.totalTracks} tracks completed")
                            } else {
                                append("${task.totalTracks} tracks")
                            }
                        } else {
                            append("Album")
                        }
                    }
                } else {
                    buildString {
                        append(task.track.artist)
                        if (task.track.album.isNotBlank()) {
                            append(" • ")
                            append(task.track.album)
                        }
                        if (!task.speed.isNullOrBlank()) {
                            append(" • ")
                            append(task.speed)
                        } else if (task.stage != RipStage.QUEUED && task.stage != RipStage.COMPLETED) {
                            append(" • ")
                            append(task.stage.displayName)
                        }
                    }
                }
                Text(
                    text = subtitleText,
                    style = ExpressiveTypography.bodySmall.copy(fontSize = 13.sp),
                    color = if (task.stage == RipStage.ERROR) colorScheme.error else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val isZip =
                    task.stage == RipStage.UPLOADING_ZIP || task.stage == RipStage.PACKAGING_ZIP
                if (task.isAlbum && (!task.currentTrackTitle.isNullOrBlank() || isZip)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PeerlessIcon(
                            icon = if (isZip) PeerlessIcons.CloudDone else PeerlessIcons.MusicNote,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        val trackLabel = buildString {
                            if (task.currentTrackIndex != null && task.totalTracks != null) {
                                append("${task.currentTrackIndex}/${task.totalTracks}: ")
                            } else if (task.currentTrackIndex != null) {
                                append("${task.currentTrackIndex}: ")
                            }
                            append(task.currentTrackTitle ?: if (isZip) "Album ZIP archive" else "")
                            if (!task.currentTrackArtist.isNullOrBlank() && task.currentTrackArtist != task.track.artist) {
                                append(" • ")
                                append(task.currentTrackArtist)
                            }
                        }
                        Text(
                            text = trackLabel,
                            style = ExpressiveTypography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "•",
                            style = ExpressiveTypography.bodySmall.copy(fontSize = 11.sp),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        val stageColor = when (task.stage) {
                            RipStage.DOWNLOADING, RipStage.UPLOADING, RipStage.UPLOADING_ZIP -> colorScheme.primary
                            RipStage.PACKAGING_ZIP, RipStage.DECRYPTING -> colorScheme.tertiary
                            RipStage.TAGGING -> colorScheme.secondary
                            RipStage.ERROR -> colorScheme.error
                            RipStage.COMPLETED -> Color(0xFF4CAF50)
                            else -> colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        }
                        Text(
                            text = task.stage.displayName,
                            style = ExpressiveTypography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = stageColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                val isIndeterminate = task.stage == RipStage.QUEUED ||
                        (task.percent <= 0f && task.stage != RipStage.COMPLETED)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isIndeterminate) {
                        LinearWavyProgressIndicator(
                            modifier = Modifier.weight(1f),
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceContainerHighest
                        )
                    } else {
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.weight(1f),
                            amplitude = { 1f },
                            color = colorScheme.primary,
                            trackColor = colorScheme.surfaceContainerHighest
                        )
                    }

                    if (!task.speed.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            PeerlessIcon(
                                icon = PeerlessIcons.Speed,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = task.speed,
                                style = SpecBadgeTypography.copy(fontSize = 10.sp),
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "${task.percent.toInt()}%",
                        style = SpecBadgeTypography.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
            }
        },
        trailingContent = if (task.isOwner) {
            {
                TextButton(
                    onClick = onCancel,
                    shape = PillShape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorScheme.error
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = ExpressiveTypography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else null
    )
}

private data class StageBadgeStyle(
    val bgColor: Color,
    val fgColor: Color,
    val borderColor: Color,
    val icon: Any
)

@Composable
fun RipStageBadge(
    stage: RipStage,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    val badgeStyle = when (stage) {
        RipStage.QUEUED -> StageBadgeStyle(
            bgColor = colorScheme.surfaceContainerHighest,
            fgColor = colorScheme.onSurfaceVariant,
            borderColor = colorScheme.outlineVariant.copy(alpha = 0.6f),
            icon = PeerlessIcons.Queue
        )

        RipStage.DOWNLOADING -> StageBadgeStyle(
            bgColor = colorScheme.primaryContainer,
            fgColor = colorScheme.onPrimaryContainer,
            borderColor = colorScheme.primary.copy(alpha = 0.25f),
            icon = PeerlessIcons.Download
        )

        RipStage.DECRYPTING -> StageBadgeStyle(
            bgColor = colorScheme.tertiaryContainer,
            fgColor = colorScheme.onTertiaryContainer,
            borderColor = colorScheme.tertiary.copy(alpha = 0.25f),
            icon = PeerlessIcons.Key
        )

        RipStage.TAGGING -> StageBadgeStyle(
            bgColor = colorScheme.secondaryContainer,
            fgColor = colorScheme.onSecondaryContainer,
            borderColor = colorScheme.secondary.copy(alpha = 0.25f),
            icon = PeerlessIcons.MusicNote
        )

        RipStage.UPLOADING -> StageBadgeStyle(
            bgColor = colorScheme.primaryContainer,
            fgColor = colorScheme.onPrimaryContainer,
            borderColor = colorScheme.primary.copy(alpha = 0.25f),
            icon = PeerlessIcons.CloudDone
        )

        RipStage.PACKAGING_ZIP -> StageBadgeStyle(
            bgColor = colorScheme.tertiaryContainer,
            fgColor = colorScheme.onTertiaryContainer,
            borderColor = colorScheme.tertiary.copy(alpha = 0.25f),
            icon = PeerlessIcons.Library
        )

        RipStage.UPLOADING_ZIP -> StageBadgeStyle(
            bgColor = colorScheme.primaryContainer,
            fgColor = colorScheme.onPrimaryContainer,
            borderColor = colorScheme.primary.copy(alpha = 0.25f),
            icon = PeerlessIcons.CloudDone
        )

        RipStage.COMPLETED -> StageBadgeStyle(
            bgColor = Color(0xFF1B5E20).copy(alpha = 0.15f),
            fgColor = Color(0xFF4CAF50),
            borderColor = Color(0xFF4CAF50).copy(alpha = 0.35f),
            icon = PeerlessIcons.CheckCircle
        )

        RipStage.CANCELLED -> StageBadgeStyle(
            bgColor = colorScheme.surfaceContainerHighest,
            fgColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            borderColor = colorScheme.outlineVariant.copy(alpha = 0.5f),
            icon = PeerlessIcons.Close
        )

        RipStage.ERROR -> StageBadgeStyle(
            bgColor = colorScheme.errorContainer,
            fgColor = colorScheme.onErrorContainer,
            borderColor = colorScheme.error.copy(alpha = 0.3f),
            icon = PeerlessIcons.Warning
        )
    }

    Surface(
        shape = PillShape,
        color = badgeStyle.bgColor,
        border = BorderStroke(1.dp, badgeStyle.borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PeerlessIcon(
                icon = badgeStyle.icon,
                contentDescription = null,
                tint = badgeStyle.fgColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = stage.displayName,
                style = SpecBadgeTypography.copy(
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                ),
                color = badgeStyle.fgColor
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
