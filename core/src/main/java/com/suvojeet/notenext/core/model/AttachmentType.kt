package com.suvojeet.notenext.core.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Represents the type of an attachment in the application.
 */
@Keep
@Serializable
enum class AttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    FILE
}
