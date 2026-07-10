@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
package com.suvojeet.notenext.ui.components
 
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
 
// Indeterminate — use when progress is unknown (e.g. AI loading, syncing)
@Composable
fun WavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LinearWavyProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = color.copy(alpha = 0.2f),
    )
}
 
// Determinate — use when progress value is known (e.g. file upload)
@Composable
fun WavyProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LinearWavyProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = color.copy(alpha = 0.2f),
    )
}
