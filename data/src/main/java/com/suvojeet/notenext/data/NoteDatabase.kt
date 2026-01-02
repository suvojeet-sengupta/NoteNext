
package com.suvojeet.notenext.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class, Label::class, Attachment::class, Project::class, NoteFts::class, ChecklistItem::class, NoteVersion::class], version = 19, exportSchema = false)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun labelDao(): LabelDao
    abstract fun projectDao(): ProjectDao
    abstract fun checklistItemDao(): ChecklistItemDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                .build()
                INSTANCE = instance
                instance
            }
        }
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN aiSummary TEXT")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS table with incorrect options
                db.execSQL("DROP TABLE IF EXISTS notes_fts")
                // Recreate with backticks for content option as expected by Room
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`title`, `content`, `label`, content=`notes`)")
                // Rebuild the index
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN description TEXT")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE labels ADD COLUMN parentName TEXT")
                db.execSQL("ALTER TABLE notes ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop the old FTS table
                db.execSQL("DROP TABLE IF EXISTS notes_fts")
                // Recreate with label column
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING FTS4(title, content, label, content='notes')")
                // Rebuild the index
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
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

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
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
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                    
                    do {
                        val noteId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                        val content = cursor.getString(cursor.getColumnIndexOrThrow("content"))
                        
                        try {
                            val oldItems: List<Map<String, Any>>? = gson.fromJson(content, type)
                            
                            oldItems?.forEachIndexed { index, itemMap ->
                                val id = itemMap["id"] as? String ?: java.util.UUID.randomUUID().toString()
                                val text = itemMap["text"] as? String ?: ""
                                val isChecked = (itemMap["isChecked"] as? Boolean) == true
                                
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

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the FTS4 virtual table, using 'notes' as the content table
                db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(`title`, `content`, content='notes')")
                // Rebuild the FTS index to populate it with existing data
                db.execSQL("INSERT INTO notes_fts(notes_fts) VALUES ('rebuild')")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN projectId INTEGER")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes RENAME COLUMN timestamp TO createdAt")
                db.execSQL("ALTER TABLE notes ADD COLUMN lastEdited INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned BOOLEAN NOT NULL DEFAULT false")
                db.execSQL("ALTER TABLE notes ADD COLUMN isArchived BOOLEAN NOT NULL DEFAULT false")
                db.execSQL("ALTER TABLE notes ADD COLUMN reminder INTEGER")
                db.execSQL("ALTER TABLE notes ADD COLUMN isImportant BOOLEAN NOT NULL DEFAULT false")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN label TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `labels` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isBinned BOOLEAN NOT NULL DEFAULT false")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN linkPreviews TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN noteType TEXT NOT NULL DEFAULT 'TEXT'")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
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
