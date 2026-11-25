package nl.ecannemeijer.casacheck.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.db.Migrations
import nl.ecannemeijer.casacheck.data.db.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CasaDatabase::class.java)

    private val insertProperty = "INSERT INTO property (id, uuid, title, favorite, archived, featuresConfirmed, manualFields, createdAt, updatedAt) " +
        "VALUES (1, 'u-1', 'Casa in pietra', 0, 0, 0, '', 1, 1)"

    @Test fun migrate2To3KeepsDataAndAddsTranslationTable() {
        val name = "mig-2-3.db"
        helper.createDatabase(name, 2).apply { execSQL(insertProperty); close() }
        val db = helper.runMigrationsAndValidate(name, 3, true, Migrations.MIGRATION_2_3)
        db.query("SELECT title FROM property WHERE id = 1").use { c -> assertTrue(c.moveToFirst()); assertEquals("Casa in pietra", c.getString(0)) }
        db.execSQL("INSERT INTO translation (propertyId, field, sourceHash, sourceLang, targetLang, text, createdAt) VALUES (1, 'description', 'h', 'it', 'nl', 'Stenen huis', 1)")
        db.query("SELECT COUNT(*) FROM translation").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
        db.close()
    }

    @Test fun migrate1To3AllMigrations() {
        val name = "mig-1-3.db"
        helper.createDatabase(name, 1).apply { execSQL(insertProperty); close() }
        helper.runMigrationsAndValidate(name, 3, true, *Migrations.ALL).close()
    }

    @Test fun roomOpensMigratedDbAndCascadesTranslations() = runBlocking<Unit> {
        val name = "mig-room.db"
        helper.createDatabase(name, 2).apply { execSQL(insertProperty); close() }
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.databaseBuilder(ctx, CasaDatabase::class.java, name).addMigrations(*Migrations.ALL).build()
        try {
            val dao = db.translationDao()
            dao.upsert(Translation(propertyId = 1, field = "description", sourceHash = "a", sourceLang = "it", targetLang = "nl", text = "een"))
            dao.upsert(Translation(propertyId = 1, field = "description", sourceHash = "b", sourceLang = "it", targetLang = "nl", text = "twee"))
            assertEquals("twee", dao.get(1, "description", "nl")?.text)
            assertEquals(1, dao.count())
            db.openHelper.writableDatabase.execSQL("DELETE FROM property WHERE id = 1")
            assertNull(dao.get(1, "description", "nl"))
        } finally { db.close(); ctx.deleteDatabase(name) }
    }
}