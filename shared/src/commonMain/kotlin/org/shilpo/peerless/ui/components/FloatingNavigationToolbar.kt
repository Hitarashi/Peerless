package org.shilpo.peerless.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.ui.navigation.NavigationDestination

val NavigationBarHorizontalPadding = 12.dp
val NavigationBarBottomPadding = 10.dp
val NavigationBarMaxWidth = 420.dp
val NavigationBarHeight = 78.dp
val MiniPlayerHeight = 70.dp
val MiniPlayerBottomSpacing = 4.dp
val FloatingBarStandaloneCornerRadius = 32.dp
val FloatingBarOuterCornerRadius = 28.dp
val FloatingBarJunctionCornerRadius = 12.dp

private val NavigationItemsMaxWidth = 360.dp
private val NavigationItemVerticalPadding = 8.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingNavigationToolbar(
    items: List<NavigationDestination>,
    selectedDestination: NavigationDestination,
    onSelectDestination: (NavigationDestination) -> Unit,
    modifier: Modifier = Modifier,
    isPairedWithMiniPlayer: Boolean = false,
    pureBlack: Boolean = false
) {
    val navigationShape = remember(isPairedWithMiniPlayer) {
        if (isPairedWithMiniPlayer) {
            RoundedCornerShape(
                topStart = FloatingBarJunctionCornerRadius,
                topEnd = FloatingBarJunctionCornerRadius,
                bottomStart = FloatingBarOuterCornerRadius,
                bottomEnd = FloatingBarOuterCornerRadius,
            )
        } else {
            RoundedCornerShape(FloatingBarStandaloneCornerRadius)
        }
    }

    val navigationContainerColor =
        if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val motionScheme = MaterialTheme.motionScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = NavigationBarMaxWidth)
                .fillMaxWidth()
                .height(NavigationBarHeight),
            shape = navigationShape,
            color = navigationContainerColor,
            tonalElevation = NavigationBarDefaults.Elevation,
            shadowElevation = NavigationBarDefaults.Elevation,
        ) {
            ShortNavigationBar(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets(0, 0, 0, 0),
                arrangement = ShortNavigationBarArrangement.EqualWeight,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = NavigationItemsMaxWidth)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(vertical = NavigationItemVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEach { dest ->
                            val selected = dest == selectedDestination

                            ShortNavigationBarItem(
                                selected = selected,
                                onClick = { onSelectDestination(dest) },
                                modifier = Modifier.weight(1f),
                                colors = ShortNavigationBarItemDefaults.colors(
                                    selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                ),
                                icon = {
                                    val rotation by animateFloatAsState(
                                        targetValue = when (dest) {
                                            NavigationDestination.SETTINGS -> if (selected) 45f else 0f
                                            else -> 0f
                                        },
                                        animationSpec = spring(
                                            dampingRatio = 0.65f,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "navIconRotation"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                rotationZ = rotation
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (dest) {
                                            NavigationDestination.HOME -> {
                                                HomeMorphIcon(
                                                    selected = selected,
                                                    size = 24.dp,
                                                    contentDescription = dest.title,
                                                )
                                            }

                                            NavigationDestination.SEARCH -> {
                                                SearchMorphIcon(
                                                    selected = selected,
                                                    size = 24.dp,
                                                    contentDescription = dest.title,
                                                )
                                            }

                                            NavigationDestination.LIBRARY -> {
                                                LibraryMorphIcon(
                                                    selected = selected,
                                                    size = 24.dp,
                                                    contentDescription = dest.title,
                                                )
                                            }

                                            NavigationDestination.SETTINGS -> {
                                                SettingsMorphIcon(
                                                    selected = selected,
                                                    size = 24.dp,
                                                    contentDescription = dest.title,
                                                    animateRotation = false,
                                                )
                                            }
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = dest.title,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
