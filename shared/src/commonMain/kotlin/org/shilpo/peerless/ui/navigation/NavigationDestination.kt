package org.shilpo.peerless.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.shilpo.peerless.ui.components.PeerlessIcons

enum class NavigationDestination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    HOME(
        title = "Home",
        subtitle = "Explore & taste mixes",
        icon = PeerlessIcons.Home,
        selectedIcon = PeerlessIcons.HomeFilled
    ),
    SEARCH(
        title = "Search",
        subtitle = "Lossless catalog & filters",
        icon = PeerlessIcons.Search,
        selectedIcon = PeerlessIcons.Search
    ),
    LIBRARY(
        title = "Library",
        subtitle = "Favorites & playlists",
        icon = PeerlessIcons.Library,
        selectedIcon = PeerlessIcons.LibraryFilled
    ),
    SETTINGS(
        title = "Settings",
        subtitle = "Server & audio stream",
        icon = PeerlessIcons.Settings,
        selectedIcon = PeerlessIcons.Settings
    );

    companion object {
        val PrimaryDestinations = entries
        val MainDestinations = listOf(HOME, SEARCH, LIBRARY)
    }
}
