package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.suvojeet.notenext.core.R

/**
 * Editorial "Ink & Paper" type families, bundled offline.
 *
 * Fraunces  — the display/serif voice (soft, optical, alive).
 * DM Sans   — the quiet body/UI voice.
 * JetBrains Mono — the factual meta voice (dates, counts, labels).
 *
 * The .ttf files live in core/src/main/res/font/. See core/FONTS_README.txt
 * for the exact files and where to download them (all OFL).
 */

// Fraunces ships static instances for Light/Regular/SemiBold (no Medium);
// Medium (500) requests resolve to the nearest available weight.
val Fraunces = FontFamily(
    Font(R.font.fraunces_light, FontWeight.Light),
    Font(R.font.fraunces_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.fraunces_semibold, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
)

val DmSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_semibold, FontWeight.SemiBold),
    Font(R.font.dmsans_bold, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_regular, FontWeight.Normal),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
)
