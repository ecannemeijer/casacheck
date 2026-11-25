package nl.ecannemeijer.casacheck.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room as RoomBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    version = CasaDatabase.VERSION,
    exportSchema = true,
    entities = [
        Property::class, PropertyFeature::class, PropertyListing::class, PropertyPriceHistory::class,
        Floor::class, Room::class, InspectionCategory::class, InspectionTemplate::class, InspectionItem::class,
        InspectionResult::class, InspectionSession::class, RepairProject::class, Defect::class,
        MoistureMeasurement::class, MoistureMeasurementPoint::class, CrackMeasurement::class, CostEstimate::class,
        Document::class, DocumentFile::class, Photo::class, PropertyPhoto::class, PhotoAnnotation::class,
        PhotoInspectionLink::class, AgentQuestion::class, AgentAnswer::class, PropertyPreference::class,
        Appointment::class, ImportHistory::class, EnvironmentRating::class, Destination::class,
        DestinationDistance::class, Report::class, Translation::class,
    ]
)
@TypeConverters(Converters::class)
abstract class CasaDatabase : RoomDatabase() {
    abstract fun propertyDao(): PropertyDao
    abstract fun featureDao(): FeatureDao
    abstract fun listingDao(): ListingDao
    abstract fun roomDao(): RoomDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun defectDao(): DefectDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun photoDao(): PhotoDao
    abstract fun documentDao(): DocumentDao
    abstract fun agentDao(): AgentDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun environmentDao(): EnvironmentDao
    abstract fun reportDao(): ReportDao
    abstract fun translationDao(): TranslationDao

    companion object {
        const val VERSION = 3
        const val NAME = "casacheck.db"

        fun build(context: Context, name: String? = NAME): CasaDatabase {
            val builder = if (name == null) RoomBuilder.inMemoryDatabaseBuilder(context, CasaDatabase::class.java)
            else RoomBuilder.databaseBuilder(context, CasaDatabase::class.java, name)
            // No destructive migrations: every schema change ships with an explicit Migration.
            return builder.addMigrations(*Migrations.ALL).build()
        }
    }
}
