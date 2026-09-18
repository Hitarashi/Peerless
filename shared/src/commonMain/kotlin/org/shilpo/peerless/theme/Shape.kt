package org.shilpo.peerless.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val PillShape = RoundedCornerShape(50)
val SquircleShapeLarge = RoundedCornerShape(28.dp)
val SquircleShapeMedium = RoundedCornerShape(20.dp)
val SquircleShapeSmall = RoundedCornerShape(14.dp)
val ArtworkShape = RoundedCornerShape(14.dp)
val HeroArtworkShape = RoundedCornerShape(26.dp)

val AsymmetricCardShape = RoundedCornerShape(
    topStart = CornerSize(24.dp),
    topEnd = CornerSize(12.dp),
    bottomEnd = CornerSize(24.dp),
    bottomStart = CornerSize(12.dp)
)
