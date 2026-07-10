@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AiThinkingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    primaryColor: Color = Color(0xFF00E5FF), // Cyan
    secondaryColor: Color = Color(0xFFAB47BC), // Purple
    tertiaryColor: Color = Color(0xFF2979FF) // Blue // Left parameter for compatibility
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            // LoadingIndicator uses MaterialTheme colors by default, but we can customize
            // In alpha15, we can just use the expressive default
            modifier = Modifier.padding(8.dp)
        )
    }
}
