package com.suvojeet.notenext.ui.theme

/**
 * Theme moods.
 *
 *  SYSTEM / LIGHT / DARK / AMOLED — follow-OS + classic toggles (kept for
 *      backwards compatibility with previously persisted preferences).
 *  MOCHA / SAGE — bespoke warm-dark editorial moods from the redesign.
 *
 * PAPER (light) maps to LIGHT; MIDNIGHT (dark) maps to DARK. The settings
 * picker presents them as named moods.
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK, AMOLED, MOCHA, SAGE
}
