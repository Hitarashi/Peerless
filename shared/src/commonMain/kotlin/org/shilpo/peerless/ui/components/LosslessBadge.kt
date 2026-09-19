package org.shilpo.peerless.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.shilpo.peerless.theme.PillShape
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
    showTierTag: Boolean = true
) {
    val tier = determineLosslessTier(bitDepth, sampleRate, codec)
    val colorScheme = MaterialTheme.colorScheme
    val badgeAccentColor = when (tier) {
        LosslessTier.HI_RES_LOSSLESS -> colorScheme.tertiary
        LosslessTier.LOSSLESS -> colorScheme.secondary
        LosslessTier.HIGH_QUALITY -> colorScheme.primary
    }
    val badgeBgColor = badgeAccentColor.copy(alpha = 0.12f)
    val badgeBorderColor = badgeAccentColor.copy(alpha = 0.35f)

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

    val shape = if (compact) RoundedCornerShape(6.dp) else PillShape

    Row(
        modifier = modifier
            .background(badgeBgColor, shape)
            .border(width = 1.dp, color = badgeBorderColor, shape = shape)
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = PeerlessIcons.LosslessWave,
            contentDescription = "Lossless Audio",
            tint = badgeAccentColor,
            modifier = Modifier.size(if (compact) 10.dp else 13.dp)
        )

        Text(
            text = labelText,
            style = if (compact) SpecBadgeTypography else SpecBadgeLargeTypography,
            color = badgeAccentColor
        )
    }
}
