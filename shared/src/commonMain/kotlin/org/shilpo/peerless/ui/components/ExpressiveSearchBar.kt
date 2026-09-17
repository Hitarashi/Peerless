package org.shilpo.peerless.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.theme.*

@Composable
fun ExpressiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    selectedFilter: String,
    onFilterSelect: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isDevMode: Boolean = true,
    serverUrl: String = ""
) {
    val filters = listOf("All", "Cached", "Apple Music", "Qobuz")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Floating Search Input Bar
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
            Icon(
                imageVector = PeerlessIcons.Search,
                contentDescription = "Search",
                tint = PrimaryDark,
                modifier = Modifier.size(20.dp)
            )

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

            // Server Settings Button with status indicator dot
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

                // Small pulse/status badge dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isDevMode) LosslessGold else SecondaryDark)
                )
            }
        }

        // Horizontal Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filter ->
                val isSelected = filter == selectedFilter
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) PrimaryContainerDark else SurfaceContainerDark,
                    animationSpec = tween(200)
                )
                val chipText by animateColorAsState(
                    targetValue = if (isSelected) OnPrimaryContainerDark else OnSurfaceVariantDark,
                    animationSpec = tween(200)
                )
                val chipBorder by animateColorAsState(
                    targetValue = if (isSelected) PrimaryDark.copy(alpha = 0.6f) else OutlineVariantDark,
                    animationSpec = tween(200)
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(chipBg)
                        .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                        .clickable { onFilterSelect(filter) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (filter == "Cached") {
                        Icon(
                            imageVector = PeerlessIcons.CloudDone,
                            contentDescription = null,
                            tint = if (isSelected) SecondaryDark else OnSurfaceVariantDark,
                            modifier = Modifier.size(14.dp)
                        )
                    } else if (filter == "Qobuz") {
                        Icon(
                            imageVector = PeerlessIcons.LosslessWave,
                            contentDescription = null,
                            tint = if (isSelected) LosslessGold else OnSurfaceVariantDark,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = filter,
                        style = ExpressiveTypography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = chipText
                    )
                }
            }
        }
    }
}
