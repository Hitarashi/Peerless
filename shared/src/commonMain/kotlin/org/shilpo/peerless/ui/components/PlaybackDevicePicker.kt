package org.shilpo.peerless.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.shilpo.peerless.sync.ConnectedDevice
import org.shilpo.peerless.sync.LocalPlaybackSyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackDeviceIndicator(
    modifier: Modifier = Modifier,
    showLabel: Boolean,
    inverted: Boolean = false
) {
    val syncManager = LocalPlaybackSyncManager.current
    val devices by syncManager.connectedDevices.collectAsState()
    val activeDeviceId by syncManager.activeDeviceId.collectAsState()
    val isConnected by syncManager.isConnected.collectAsState()
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }

    val activeDevice = devices.firstOrNull { it.device_id == activeDeviceId }
    val isPlayingHere = activeDeviceId == null || activeDeviceId == syncManager.selfDeviceId
    val label = when {
        !isConnected -> "Device sync offline"
        isPlayingHere -> "Playing on this device"
        else -> "Playing on ${activeDevice?.device_name ?: "another device"}"
    }
    val contentColor = if (inverted) Color.White else MaterialTheme.colorScheme.primary

    if (showLabel) {
        Row(
            modifier = modifier
                .clickable(enabled = isConnected) { showPicker = true }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = PeerlessIcons.Devices,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        IconButton(
            onClick = { showPicker = true },
            enabled = isConnected,
            modifier = modifier,
            colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor)
        ) {
            Icon(
                imageVector = PeerlessIcons.Devices,
                contentDescription = label,
                modifier = Modifier.size(21.dp)
            )
        }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Choose where to play", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Only one device plays audio. Your controls always operate the selected device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                devices.sortedByDescending { it.device_id == activeDeviceId }.forEach { device ->
                    DeviceRow(
                        device = device,
                        isThisDevice = device.device_id == syncManager.selfDeviceId,
                        isActive = device.device_id == activeDeviceId,
                        onClick = {
                            if (device.device_id != activeDeviceId) {
                                scope.launch { syncManager.transferPlayback(device.device_id) }
                            }
                            showPicker = false
                        }
                    )
                }

                if (devices.isEmpty()) {
                    Text(
                        "No connected devices found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: ConnectedDevice,
    isThisDevice: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(PeerlessIcons.Devices, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = device.device_name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        isActive && isThisDevice -> "Playing here · This device"
                        isActive -> "Currently playing"
                        isThisDevice -> "This device · ${device.platform}"
                        else -> device.platform
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                Icon(
                    PeerlessIcons.CheckCircle,
                    contentDescription = "Active playback device",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text("Select", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
