package org.shilpo.peerless.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.shilpo.peerless.theme.*

@Composable
fun ServerSettingsDialog(
    currentServerUrl: String,
    onSave: (serverUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(min = 320.dp, max = 460.dp)
                .clip(SquircleShapeLarge)
                .background(SurfaceContainerHighDark)
                .border(1.dp, OutlineVariantDark, SquircleShapeLarge)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Server & Playback",
                            style = ExpressiveTypography.headlineSmall,
                            color = OnSurfaceDark
                        )
                        Text(
                            text = "Configure your Peerless daemon endpoint",
                            style = ExpressiveTypography.bodySmall,
                            color = OnSurfaceVariantDark
                        )
                    }

                    Icon(
                        imageVector = PeerlessIcons.Settings,
                        contentDescription = null,
                        tint = PrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                HorizontalDivider(color = OutlineVariantDark)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "API Base URL",
                        style = ExpressiveTypography.labelMedium,
                        color = OnSurfaceDark
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "http://localhost:4444",
                                color = OnSurfaceVariantDark.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = OnSurfaceDark,
                            unfocusedTextColor = OnSurfaceDark,
                            focusedBorderColor = PrimaryDark,
                            unfocusedBorderColor = OutlineDark,
                            cursorColor = PrimaryDark,
                            focusedContainerColor = SurfaceContainerDark,
                            unfocusedContainerColor = SurfaceContainerDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = PillShape
                    ) {
                        Text("Cancel", color = OnSurfaceVariantDark)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val sanitized = urlInput.trim()
                            onSave(sanitized)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryDark,
                            contentColor = OnPrimaryDark
                        ),
                        shape = PillShape
                    ) {
                        Text("Apply Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
