package com.suvojeet.notenext.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

enum class ShapeFamily {
    ROUNDED,
    EXPRESSIVE,
    CUT
}

val RoundedShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
    small = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp),
    extraLarge = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp)
)

val CutShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(8.dp),
    medium = CutCornerShape(12.dp),
    large = CutCornerShape(16.dp),
    extraLarge = CutCornerShape(24.dp)
)
