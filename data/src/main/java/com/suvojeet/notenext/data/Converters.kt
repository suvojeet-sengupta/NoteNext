package com.suvojeet.notenext.data

import androidx.room.TypeConverter
import com.suvojeet.notenext.core.model.AttachmentType
import com.suvojeet.notenext.core.model.NoteType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class Converters {
    @TypeConverter
    fun fromLinkPreviewList(value: List<LinkPreview>): String {
        return Json.encodeToString(ListSerializer(LinkPreview.serializer()), value)
    }

    @TypeConverter
    fun toLinkPreviewList(value: String): List<LinkPreview> {
        return try {
            Json.decodeFromString(ListSerializer(LinkPreview.serializer()), value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromNoteType(value: NoteType): String {
        return value.name
    }

    @TypeConverter
    fun toNoteType(value: String): NoteType {
        return try {
            NoteType.valueOf(value)
        } catch (e: Exception) {
            NoteType.TEXT
        }
    }

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String {
        return value.name
    }

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType {
        return try {
            AttachmentType.valueOf(value)
        } catch (e: Exception) {
            AttachmentType.FILE
        }
    }
}
