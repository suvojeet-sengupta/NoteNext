package com.suvojeet.notenext.data.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.suvojeet.notenext.data.NoteDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NoteDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate26To27() {
        var db = helper.createDatabase(TEST_DB, 26)

        // db has schema version 26. insert some data using SQL queries.
        // You cannot use DAO classes because they expect the latest schema.
        db.execSQL("INSERT INTO notes (id, title, content, createdAt, lastEdited) VALUES (1, 'Test', 'Content', 0, 0)")

        // Prepare for the next version.
        db.close()

        // Re-open the database with version 27 and provide the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 27, true)

        // MigrationTestHelper automatically verifies the schema changes, but you should verify data integrity.
    }
}
