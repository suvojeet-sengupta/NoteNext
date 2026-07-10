package com.suvojeet.notenext.data

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class RepeatOption(val label: String) {
    NEVER("Does not repeat"),
    DAILY("Every day"),
    WEEKLY("Every week"),
    MONTHLY("Every month"),
    YEARLY("Every year")
}
