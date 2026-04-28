package com.suvojeet.notenext.core.util

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class SortType {
    DATE_CREATED,
    DATE_MODIFIED,
    TITLE,
    CUSTOM
}
