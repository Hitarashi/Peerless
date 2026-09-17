package org.shilpo.peerless.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.shilpo.peerless.ui.components.PeerlessIcons

/**
 * Primary navigation destinations for the Peerless shell.
 * Hosted across the Compact FloatingNavDock, Medium NavigationRail,
 * and Expanded Persistent Navigation Drawer.
 */
enum class NavigationDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    HOME(
        title = "Home",
        subtitle = "Explore & taste mixes",
        icon = PeerlessIcons.Home
    ),
    SEARCH(
        title = "Search",
        subtitle = "Lossless catalog & filters",
        icon = PeerlessIcons.Search
    ),
    LIBRARY(
        title = "Library",
        subtitle = "Favorites & playlists",
        icon = PeerlessIcons.Library
    ),
    SETTINGS(
        title = "Settings",
        subtitle = "Server & audio stream",
        icon = PeerlessIcons.Settings
    );

    companion object {
        val PrimaryDestinations = entries
    }
}
