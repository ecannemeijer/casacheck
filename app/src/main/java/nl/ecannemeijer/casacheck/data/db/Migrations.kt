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

    /** v3: cached on-device translations of listing texts. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `translation` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `propertyId` INTEGER NOT NULL, " +
                    "`field` TEXT NOT NULL, `sourceHash` TEXT NOT NULL, `sourceLang` TEXT NOT NULL, `targetLang` TEXT NOT NULL, `text` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, FOREIGN KEY(`propertyId`) REFERENCES `property`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_translation_propertyId` ON `translation` (`propertyId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_translation_propertyId_field_targetLang` ON `translation` (`propertyId`, `field`, `targetLang`)")
        }
    }

    /** v4: viewing recordings with markers and transcript segments. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recording` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uuid` TEXT NOT NULL, `propertyId` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, `filePath` TEXT NOT NULL, `mime` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `sizeBytes` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL, `transcriptStatus` TEXT NOT NULL, `transcriptLang` TEXT, " +
                    "`transcriptError` TEXT, `transcriptModel` TEXT, FOREIGN KEY(`propertyId`) REFERENCES `property`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recording_propertyId` ON `recording` (`propertyId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recording_uuid` ON `recording` (`uuid`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `recording_marker` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordingId` INTEGER NOT NULL, " +
                    "`positionMs` INTEGER NOT NULL, `label` TEXT, `itemId` INTEGER, `createdAt` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`recordingId`) REFERENCES `recording`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                    "FOREIGN KEY(`itemId`) REFERENCES `inspection_item`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recording_marker_recordingId` ON `recording_marker` (`recordingId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recording_marker_itemId` ON `recording_marker` (`itemId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `transcript_segment` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recordingId` INTEGER NOT NULL, " +
                    "`idx` INTEGER NOT NULL, `startMs` INTEGER NOT NULL, `endMs` INTEGER NOT NULL, `text` TEXT NOT NULL, " +
                    "FOREIGN KEY(`recordingId`) REFERENCES `recording`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_transcript_segment_recordingId` ON `transcript_segment` (`recordingId`)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
