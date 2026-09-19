package org.shilpo.peerless.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.shilpo.peerless.theme.SpecBadgeLargeTypography
import org.shilpo.peerless.theme.SpecBadgeTypography

enum class LosslessTier {
    HI_RES_LOSSLESS,
    LOSSLESS,
    HIGH_QUALITY
}

fun determineLosslessTier(bitDepth: Int?, sampleRate: Int?, codec: String?): LosslessTier {
    val depth = bitDepth ?: 16
    val rate = sampleRate ?: 44100
    val isLosslessCodec = codec?.uppercase() in listOf("FLAC", "ALAC", "WAV", "AIFF")

    return when {
        isLosslessCodec && (depth >= 24 || rate >= 88200) -> LosslessTier.HI_RES_LOSSLESS
        isLosslessCodec -> LosslessTier.LOSSLESS
        else -> LosslessTier.HIGH_QUALITY
    }
}

fun formatSampleRate(sampleRate: Int?): String? {
    if (sampleRate == null || sampleRate <= 0) return null
    val rate = if (sampleRate < 1000) sampleRate * 1000 else sampleRate
    val khz = rate / 1000.0
    return if (khz % 1.0 == 0.0) {
        "${khz.toInt()}kHz"
    } else {
        "${((khz * 10).toInt() / 10.0)}kHz"
    }
}

fun formatBitDepth(bitDepth: Int?): String? {
    return if (bitDepth != null && bitDepth > 0) "${bitDepth}-bit" else null
}

fun formatCodec(codec: String?): String {
    return codec?.trim()?.uppercase() ?: "AUDIO"
}

@Composable
fun LosslessBadge(
    bitDepth: Int?,
    sampleRate: Int?,
    codec: String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showTierTag: Boolean = true,
    provider: String? = null,
    onClick: (() -> Unit)? = null
) {
    val tier = determineLosslessTier(bitDepth, sampleRate, codec)
    val colorScheme = MaterialTheme.colorScheme
    val badgeAccentColor = when (tier) {
        LosslessTier.HI_RES_LOSSLESS -> colorScheme.tertiary
        LosslessTier.LOSSLESS -> colorScheme.secondary
        LosslessTier.HIGH_QUALITY -> colorScheme.primary
    }

    val depthStr = formatBitDepth(bitDepth)
    val rateStr = formatSampleRate(sampleRate)
    val codecStr = formatCodec(codec)

    val labelText = if (compact) {
        buildString {
            if (depthStr != null) append("$depthStr ")
            append(codecStr)
        }
    } else {
        buildString {
            if (depthStr != null && rateStr != null) {
                append("$depthStr / $rateStr • ")
            } else if (rateStr != null) {
                append("$rateStr • ")
            } else if (depthStr != null) {
                append("$depthStr • ")
            }
            append(codecStr)
            if (showTierTag) {
                when (tier) {
                    LosslessTier.HI_RES_LOSSLESS -> append(" • Hi-Res")
                    LosslessTier.LOSSLESS -> append(" • Lossless")
                    LosslessTier.HIGH_QUALITY -> {}
                }
            }
        }
    }

    val hasApple = provider?.contains("apple", ignoreCase = true) == true
    val hasQobuz = provider?.contains("qobuz", ignoreCase = true) == true
    val hasDolby = codec?.contains("ec-3", ignoreCase = true) == true ||
            codec?.contains("ec3", ignoreCase = true) == true ||
            codec?.contains("atmos", ignoreCase = true) == true

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier
                        .background(if (isHovered) colorScheme.onSurfaceVariant.copy(alpha = 0.08f) else Color.Transparent)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = badgeAccentColor),
                            onClick = onClick
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hasApple) {
            PeerlessIcon(
                icon = PeerlessIcons.AppleLogo,
                contentDescription = "Apple Music",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.size(if (compact) 11.dp else 13.dp)
            )
        }
        if (hasQobuz) {
            PeerlessIcon(
                icon = PeerlessIcons.QobuzLogo,
                contentDescription = "Qobuz",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier
                    .height(if (compact) 10.dp else 12.dp)
                    .width(if (compact) 25.dp else 30.dp)
            )
        }
        if (provider != null && !hasApple && !hasQobuz && provider.isNotBlank()) {
            Text(
                text = formatProviderLabel(provider),
                style = SpecBadgeTypography.copy(
                    fontSize = if (compact) 8.5.sp else 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                ),
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        if (hasDolby) {
            PeerlessIcon(
                icon = PeerlessIcons.DolbyAtmos,
                contentDescription = "Dolby Atmos",
                tint = colorScheme.tertiary,
                modifier = Modifier
                    .height(if (compact) 9.dp else 11.dp)
                    .width(if (compact) 14.dp else 17.dp)
            )
        }

        if (tier == LosslessTier.HI_RES_LOSSLESS) {
            PeerlessIcon(
                icon = PeerlessIcons.HiRes,
                contentDescription = "Hi-Res Audio",
                tint = badgeAccentColor,
                modifier = Modifier.size(if (compact) 13.dp else 16.dp)
            )
        } else {
            PeerlessIcon(
                icon = PeerlessIcons.LosslessWave,
                contentDescription = "Lossless Audio",
                tint = badgeAccentColor,
                modifier = Modifier.size(if (compact) 11.dp else 13.dp)
            )
        }

        Text(
            text = labelText,
            style = if (compact) SpecBadgeTypography else SpecBadgeLargeTypography,
            color = badgeAccentColor
        )
    }
}
