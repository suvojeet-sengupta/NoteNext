package com.suvojeet.notenext.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromLinkPreviewList(value: List<LinkPreview>): String {
        val gson = Gson()
        val type = object : TypeToken<List<LinkPreview>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toLinkPreviewList(value: String): List<LinkPreview> {
        val gson = Gson()
        val type = object : TypeToken<List<LinkPreview>>() {}.type
        return gson.fromJson(value, type)
    }
}