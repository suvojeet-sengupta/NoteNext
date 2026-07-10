package com.suvojeet.notenext.core.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Represents the type of a note in the application.
 */
@Keep
@Serializable
enum class NoteType {
    TEXT,
    CHECKLIST
}
