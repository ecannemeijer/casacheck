package nl.ecannemeijer.casacheck.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit, non-destructive schema migrations. Each version's schema is exported to app/schemas
 * and verified by MigrationTest (instrumented).
 */
object Migrations {
    /** v2: stored PDF reports. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `report` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `propertyId` INTEGER, " +
                    "`type` TEXT NOT NULL, `language` TEXT NOT NULL, `filePath` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                    "`title` TEXT NOT NULL, FOREIGN KEY(`propertyId`) REFERENCES `property`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_report_propertyId` ON `report` (`propertyId`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
