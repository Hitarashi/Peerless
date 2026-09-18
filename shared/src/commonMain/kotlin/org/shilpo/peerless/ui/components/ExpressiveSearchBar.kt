package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shilpo.peerless.model.SearchFilter
import org.shilpo.peerless.theme.*

val DefaultSearchFilters = listOf(
    SearchFilter.ALL,
    SearchFilter.TRACKS,
    SearchFilter.ALBUMS,
    SearchFilter.ARTISTS,
    SearchFilter.APPLE_MUSIC,
    SearchFilter.QOBUZ,
    SearchFilter.CACHED
)

@Composable
fun ExpressiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    selectedFilter: SearchFilter,
    onFilterSelect: (SearchFilter) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isSearching: Boolean = false,
    isDevMode: Boolean = true,
    serverUrl: String = "",
    filters: List<SearchFilter> = DefaultSearchFilters
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SearchLoadingAnimation")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SearchPulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SearchPulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(PillShape)
                .background(SurfaceContainerHighDark)
                .border(1.dp, OutlineVariantDark, PillShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = PeerlessIcons.Search,
                    contentDescription = "Search",
                    tint = if (isSearching) SecondaryDark else PrimaryDark,
                    modifier = Modifier.size(20.dp)
                )

                if (isSearching) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(7.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(SecondaryDark)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search lossless tracks, artists, albums...",
                        style = ExpressiveTypography.bodyMedium,
                        color = OnSurfaceVariantDark.copy(alpha = 0.6f)
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = ExpressiveTypography.bodyMedium.copy(
                        color = OnSurfaceDark,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(PrimaryDark),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isSearching) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(PillShape)
                        .background(SecondaryDark.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(SecondaryDark)
                    )
                    Text(
                        text = "SEARCHING",
                        style = SpecBadgeTypography.copy(fontSize = 8.sp),
                        color = SecondaryDark
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Close,
                        contentDescription = "Clear search",
                        tint = OnSurfaceVariantDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = PeerlessIcons.Settings,
                        contentDescription = "Server Settings",
                        tint = if (isDevMode) LosslessGold else OnSurfaceVariantDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isDevMode) LosslessGold else SecondaryDark)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filter ->
                val isSelected = filter == selectedFilter

                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelect(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = ExpressiveTypography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = when (filter) {
                        SearchFilter.CACHED -> {
                            {
                                Icon(
                                    imageVector = PeerlessIcons.CloudDone,
                                    contentDescription = null,
                                    tint = if (isSelected) SecondaryDark else OnSurfaceVariantDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        SearchFilter.QOBUZ -> {
                            {
                                Icon(
                                    imageVector = PeerlessIcons.LosslessWave,
                                    contentDescription = null,
                                    tint = if (isSelected) LosslessGold else OnSurfaceVariantDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        SearchFilter.APPLE_MUSIC -> {
                            {
                                Icon(
                                    imageVector = PeerlessIcons.MusicNote,
                                    contentDescription = null,
                                    tint = if (isSelected) LosslessPurple else OnSurfaceVariantDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        SearchFilter.TRACKS, SearchFilter.ALBUMS, SearchFilter.ARTISTS -> {
                            {
                                Icon(
                                    imageVector = when (filter) {
                                        SearchFilter.ALBUMS -> PeerlessIcons.Library
                                        SearchFilter.ARTISTS -> PeerlessIcons.Home
                                        else -> PeerlessIcons.MusicNote
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) PrimaryDark else OnSurfaceVariantDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        SearchFilter.ALL -> null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SurfaceContainerDark,
                        labelColor = OnSurfaceVariantDark,
                        selectedContainerColor = PrimaryContainerDark,
                        selectedLabelColor = OnPrimaryContainerDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = OutlineVariantDark,
                        selectedBorderColor = PrimaryDark.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}
