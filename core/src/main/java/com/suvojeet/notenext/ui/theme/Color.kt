package com.suvojeet.notenext.ui.theme

import androidx.compose.ui.graphics.Color

// =====================================================================
//  INK & PAPER  —  editorial palette
//  Warm paper as the canvas, deep ink as the voice, a single sienna
//  accent used like punctuation. Light = "Paper", Dark = "Midnight".
// =====================================================================

// ---- Raw palette (shared reference) ----
val PaperBg       = Color(0xFFF2EDE3)
val PaperSheet    = Color(0xFFFBF7EE)
val PaperSheet2   = Color(0xFFF7F1E6)
val Paper3        = Color(0xFFECE6D8)
val Ink           = Color(0xFF14110E)
val Ink2          = Color(0xFF2A2620)
val Muted         = Color(0xFF6B6357)
val Muted2        = Color(0xFF9A9082)
val Hairline      = Color(0xFFDBD1C0)
val Hairline2     = Color(0xFFE8E0CF)
val Sienna        = Color(0xFFC2553D)
val SiennaDeep    = Color(0xFFA2422D)
val SiennaSoft    = Color(0xFFE8B5A4)
val Gold          = Color(0xFFB58A3A)

val NightBg       = Color(0xFF14120F)
val Night2        = Color(0xFF1F1B16)
val Night3        = Color(0xFF2A251E)
val NightText     = Color(0xFFEDE5D6)
val NightMuted    = Color(0xFF8C8270)
val NightHair     = Color(0xFF2F2A22)
val NightAccent   = Color(0xFFE47A5C)

// ---- Light (Paper) scheme tokens ----
val primaryLight = Ink
val onPrimaryLight = PaperSheet
val primaryContainerLight = Paper3
val onPrimaryContainerLight = Ink
val primaryFixedLight = Paper3
val primaryFixedDimLight = Color(0xFFDED6C4)
val onPrimaryFixedLight = Ink
val onPrimaryFixedVariantLight = Ink2

val secondaryLight = Muted
val onSecondaryLight = PaperSheet
val secondaryContainerLight = Paper3
val onSecondaryContainerLight = Ink2
val secondaryFixedLight = Paper3
val secondaryFixedDimLight = Color(0xFFDED6C4)
val onSecondaryFixedLight = Ink
val onSecondaryFixedVariantLight = Muted

val tertiaryLight = Sienna
val onTertiaryLight = PaperSheet
val tertiaryContainerLight = SiennaSoft
val onTertiaryContainerLight = Color(0xFF4A1B10)
val tertiaryFixedLight = SiennaSoft
val tertiaryFixedDimLight = Color(0xFFDFA08C)
val onTertiaryFixedLight = Color(0xFF4A1B10)
val onTertiaryFixedVariantLight = SiennaDeep

val errorLight = SiennaDeep
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFF1DACF)
val onErrorContainerLight = Color(0xFF3A140B)

val backgroundLight = PaperBg
val onBackgroundLight = Ink
val surfaceLight = PaperBg
val onSurfaceLight = Ink
val surfaceVariantLight = Paper3
val onSurfaceVariantLight = Muted

val surfaceContainerLowestLight = PaperSheet
val surfaceContainerLowLight = PaperSheet2
val surfaceContainerLight = Paper3
val surfaceContainerHighLight = Color(0xFFE5DECE)
val surfaceContainerHighestLight = Color(0xFFDED6C4)

val outlineLight = Hairline
val outlineVariantLight = Hairline2
val scrimLight = Color(0xFF000000)
val shadowLight = Color(0xFF000000)
val inverseSurfaceLight = Ink2
val inverseOnSurfaceLight = PaperBg
val inversePrimaryLight = SiennaSoft

// ---- Dark (Midnight) scheme tokens ----
val primaryDark = NightText
val onPrimaryDark = NightBg
val primaryContainerDark = Night3
val onPrimaryContainerDark = NightText
val primaryFixedDark = Night3
val primaryFixedDimDark = Night2
val onPrimaryFixedDark = NightText
val onPrimaryFixedVariantDark = Color(0xFFC9BFAE)

val secondaryDark = Color(0xFFC9BFAE)
val onSecondaryDark = Night2
val secondaryContainerDark = Night3
val onSecondaryContainerDark = NightText
val secondaryFixedDark = Night3
val secondaryFixedDimDark = Night2
val onSecondaryFixedDark = NightText
val onSecondaryFixedVariantDark = NightMuted

val tertiaryDark = NightAccent
val onTertiaryDark = NightBg
val tertiaryContainerDark = Color(0xFF5C2A1C)
val onTertiaryContainerDark = Color(0xFFF4C3B3)
val tertiaryFixedDark = SiennaSoft
val tertiaryFixedDimDark = Color(0xFFDFA08C)
val onTertiaryFixedDark = Color(0xFF4A1B10)
val onTertiaryFixedVariantDark = SiennaDeep

val errorDark = Color(0xFFE8967D)
val onErrorDark = NightBg
val errorContainerDark = Color(0xFF5C2A1C)
val onErrorContainerDark = Color(0xFFF4C3B3)

val backgroundDark = NightBg
val onBackgroundDark = NightText
val surfaceDark = NightBg
val onSurfaceDark = NightText
val surfaceVariantDark = Night3
val onSurfaceVariantDark = NightMuted

val surfaceContainerLowestDark = Color(0xFF100E0C)
val surfaceContainerLowDark = Night2
val surfaceContainerDark = Color(0xFF211C17)
val surfaceContainerHighDark = Night3
val surfaceContainerHighestDark = Color(0xFF352F26)

val outlineDark = Color(0xFF564E40)
val outlineVariantDark = NightHair
val scrimDark = Color(0xFF000000)
val shadowDark = Color(0xFF000000)
val inverseSurfaceDark = NightText
val inverseOnSurfaceDark = Night2
val inversePrimaryDark = Ink
