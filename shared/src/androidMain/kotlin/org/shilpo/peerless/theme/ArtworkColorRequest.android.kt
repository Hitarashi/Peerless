package org.shilpo.peerless.theme

import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun createSoftwareArtworkRequest(
    context: PlatformContext,
    artworkUrl: String,
    size: Int
): ImageRequest = ImageRequest.Builder(context)
    .data(artworkUrl)
    .size(size, size)
    .allowHardware(false)
    .build()
