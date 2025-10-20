package com.suvojeet.notenext.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
    small = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp),
    extraLarge = RoundedCornerShape(topStart = 32.dp, bottomEnd = 32.dp)
)