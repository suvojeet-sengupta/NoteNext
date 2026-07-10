package com.suvojeet.notenext.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.suvojeet.notenext.data.Note
import com.suvojeet.notenext.data.Label
import com.suvojeet.notenext.data.Attachment
import com.suvojeet.notenext.data.Project
import com.suvojeet.notenext.data.NoteFts
import com.suvojeet.notenext.data.ChecklistItem
import com.suvojeet.notenext.data.NoteVersion
import com.suvojeet.notenext.data.TodoItem
import com.suvojeet.notenext.data.TodoSubtask
import com.suvojeet.notenext.data.ai.AIUsageEvent
import com.suvojeet.notenext.data.ai.AIUsageDao
import kotlinx.serialization.builtins.ListSerializer

@Database(entities = [Note::class, Label::class, Attachment::class, Project::class, NoteFts::class, ChecklistItem::class, NoteVersion::class, TodoItem::class, TodoSubtask::class, AIUsageEvent::class], version = 33, exportSchema = true)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun projectDao(): ProjectDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun todoDao(): TodoDao
    abstract fun aiUsageDao(): AIUsageDao

    companion object {
        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Stores the E2E key of the last share link so a re-share can reuse
                // the same link (dedup) instead of minting a new one.
                db.execSQL("ALTER TABLE notes ADD COLUMN shareKey TEXT")
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN shareDeleteToken TEXT")
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN shareId TEXT")
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDecoy INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN expiryTime INTEGER")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ai_usage_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `featureId` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `success` INTEGER NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `accepted` INTEGER
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_usage_events_timestamp` ON `ai_usage_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_usage_events_featureId` ON `ai_usage_events` (`featureId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_usage_events_provider` ON `ai_usage_events` (`provider`)")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create todo_subtasks table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todo_subtasks` (
                        `id` TEXT NOT NULL,
                        `todoId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `isChecked` INTEGER NOT NULL DEFAULT 0,
                        `position` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`todoId`) REFERENCES `todos`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_todo_subtasks_todoId` ON `todo_subtasks` (`todoId`)")

                // Update todos table
                val cursor = db.query("PRAGMA table_info(todos)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("reminderTime" !in columns) {
                    db.execSQL("ALTER TABLE todos ADD COLUMN reminderTime INTEGER")
                }
                if ("position" !in columns) {
                    db.execSQL("ALTER TABLE todos ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                }
                if ("projectId" !in columns) {
                    db.execSQL("ALTER TABLE todos ADD COLUMN projectId INTEGER")
                }
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(projects)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("parentId" !in columns) {
                    db.execSQL("ALTER TABLE projects ADD COLUMN parentId INTEGER")
                }
                if ("orderIndex" !in columns) {
                    db.execSQL("ALTER TABLE projects ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                }
                if ("color" !in columns) {
                    db.execSQL("ALTER TABLE projects ADD COLUMN color INTEGER")
                }

                // Set orderIndex based on createdAt for existing projects
                db.execSQL("""
                    UPDATE projects 
                    SET orderIndex = (
                        SELECT COUNT(*) 
                        FROM projects p2 
                        WHERE p2.createdAt <= projects.createdAt
                    )
                """)
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_lastEdited` ON `notes` (`lastEdited`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_createdAt` ON `notes` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isArchived` ON `notes` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isBinned` ON `notes` (`isBinned`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_projectId` ON `notes` (`projectId`)")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(checklist_items)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("level" !in columns) {
                    db.execSQL("ALTER TABLE checklist_items ADD COLUMN level INTEGER NOT NULL DEFAULT 0")
                }
                if ("iv" !in columns) {
                    db.execSQL("ALTER TABLE checklist_items ADD COLUMN iv TEXT")
                }
                if ("isEncrypted" !in columns) {
                    db.execSQL("ALTER TABLE checklist_items ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS4 table
                db.execSQL("DROP TABLE IF EXISTS notes_fts")
                // Recreate with FTS4. FTS4 in Room with contentEntity handles content option correctly.
                // Using backticks for content option as expected by Room for external content tables.
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`title`, `content`, `label`, content=`notes`)")
                // Rebuild the index
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(note_versions)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("iv" !in columns) {
                    db.execSQL("ALTER TABLE note_versions ADD COLUMN iv TEXT")
                }
                if ("isEncrypted" !in columns) {
                    db.execSQL("ALTER TABLE note_versions ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("iv" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN iv TEXT")
                }
                if ("isEncrypted" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL DEFAULT '',
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `priority` INTEGER NOT NULL DEFAULT 0,
                        `dueDate` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("aiSummary" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN aiSummary TEXT")
                }
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS table with incorrect options
                db.execSQL("DROP TABLE IF EXISTS notes_fts")
                // Recreate with backticks for content option as expected by Room
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`title`, `content`, `label`, content=`notes`)")
                // Rebuild the index
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(projects)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("description" !in columns) {
                    db.execSQL("ALTER TABLE projects ADD COLUMN description TEXT")
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val labelCursor = db.query("PRAGMA table_info(labels)")
                val labelColumns = mutableSetOf<String>()
                while (labelCursor.moveToNext()) {
                    labelColumns.add(labelCursor.getString(labelCursor.getColumnIndexOrThrow("name")))
                }
                labelCursor.close()

                if ("parentName" !in labelColumns) {
                    db.execSQL("ALTER TABLE labels ADD COLUMN parentName TEXT")
                }

                val noteCursor = db.query("PRAGMA table_info(notes)")
                val noteColumns = mutableSetOf<String>()
                while (noteCursor.moveToNext()) {
                    noteColumns.add(noteCursor.getString(noteCursor.getColumnIndexOrThrow("name")))
                }
                noteCursor.close()

                if ("position" !in noteColumns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS table
                db.execSQL("DROP TABLE IF EXISTS notes_fts")
                // Recreate with label column
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING FTS4(title, content, label, content='notes')")
                // Rebuild the index
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `note_versions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `noteType` TEXT NOT NULL DEFAULT 'TEXT',
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_versions_noteId` ON `note_versions` (`noteId`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("isLocked" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `checklist_items` (
                        `id` TEXT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `isChecked` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_items_noteId` ON `checklist_items` (`noteId`)")

                // Data Migration
                val cursor = db.query("SELECT id, content FROM notes WHERE noteType = 'CHECKLIST'")
                if (cursor.moveToFirst()) {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    
                    do {
                        val noteId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                        val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                        
                        try {
                            val oldItems: List<kotlinx.serialization.json.JsonObject>? = json.decodeFromString(ListSerializer(kotlinx.serialization.json.JsonObject.serializer()), content)
                            
                            oldItems?.forEachIndexed { index, itemMap ->
                                val id = itemMap["id"]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content ?: java.util.UUID.randomUUID().toString()
                                val text = itemMap["text"]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content ?: ""
                                val isChecked = (itemMap["isChecked"]?.let { it as? kotlinx.serialization.json.JsonPrimitive }?.content?.toBoolean()) == true
                                
                                db.execSQL("INSERT INTO checklist_items (id, noteId, text, isChecked, position) VALUES (?, ?, ?, ?, ?)",
                                    arrayOf<Any>(id, noteId, text, if (isChecked) 1 else 0, index))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } while (cursor.moveToNext())
                }
                cursor.close()
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the FTS4 virtual table, using 'notes' as the content table
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`title`, `content`, content='notes')")
                // Rebuild the FTS index to populate it with existing data
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("projectId" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN projectId INTEGER")
                }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("timestamp" in columns && "createdAt" !in columns) {
                    db.execSQL("ALTER TABLE notes RENAME COLUMN timestamp TO createdAt")
                }
                if ("lastEdited" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN lastEdited INTEGER NOT NULL DEFAULT 0")
                }
                if ("color" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("isPinned" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN isPinned BOOLEAN NOT NULL DEFAULT false")
                }
                if ("isArchived" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN isArchived BOOLEAN NOT NULL DEFAULT false")
                }
                if ("reminder" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN reminder INTEGER")
                }
                if ("isImportant" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN isImportant BOOLEAN NOT NULL DEFAULT false")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("label" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN label TEXT")
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `labels` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("isBinned" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN isBinned BOOLEAN NOT NULL DEFAULT false")
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("linkPreviews" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN linkPreviews TEXT NOT NULL DEFAULT '[]'")
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
                cursor.close()

                if ("noteType" !in columns) {
                    db.execSQL("ALTER TABLE notes ADD COLUMN noteType TEXT NOT NULL DEFAULT 'TEXT'")
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `uri` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent())
            }
        }
    }
}
