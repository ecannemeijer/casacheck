package nl.ecannemeijer.casacheck

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nl.ecannemeijer.casacheck.data.backup.BackupManager
import nl.ecannemeijer.casacheck.data.db.CasaDatabase
import nl.ecannemeijer.casacheck.data.files.FileStore
import nl.ecannemeijer.casacheck.data.importer.ImportManager
import nl.ecannemeijer.casacheck.data.importer.ScreenshotImportProvider
import nl.ecannemeijer.casacheck.data.importer.TextImportProvider
import nl.ecannemeijer.casacheck.data.ocr.MlKitOcrEngine
import nl.ecannemeijer.casacheck.data.repo.AgentRepository
import nl.ecannemeijer.casacheck.data.repo.AppointmentRepository
import nl.ecannemeijer.casacheck.data.repo.DefectRepository
import nl.ecannemeijer.casacheck.data.repo.DocumentRepository
import nl.ecannemeijer.casacheck.data.repo.EnvironmentRepository
import nl.ecannemeijer.casacheck.data.repo.InspectionRepository
import nl.ecannemeijer.casacheck.data.repo.MeasurementRepository
import nl.ecannemeijer.casacheck.data.repo.PhotoRepository
import nl.ecannemeijer.casacheck.data.repo.PreferenceRepository
import nl.ecannemeijer.casacheck.data.repo.PropertyRepository
import nl.ecannemeijer.casacheck.data.repo.RoomRepository
import nl.ecannemeijer.casacheck.data.repo.SnapshotRepository
import nl.ecannemeijer.casacheck.data.report.PdfReportGenerator
import nl.ecannemeijer.casacheck.data.settings.SettingsStore

/** Simple manual dependency container (no DI framework needed). */
class AppContainer(context: Context, dbName: String? = CasaDatabase.NAME) {
    val appContext: Context = context.applicationContext
    val db: CasaDatabase = CasaDatabase.build(appContext, dbName)
    val files = FileStore(appContext)
    val settings = SettingsStore(appContext)
    val inspections = InspectionRepository(appContext, db)
    val properties = PropertyRepository(db, files, settings, inspections)
    val rooms = RoomRepository(db, inspections)
    val defects = DefectRepository(db)
    val photos = PhotoRepository(db, files)
    val measurements = MeasurementRepository(db)
    val documents = DocumentRepository(db, files)
    val agent = AgentRepository(db)
    val environment = EnvironmentRepository(db)
    val preferences = PreferenceRepository(db)
    val appointments = AppointmentRepository(db)
    val snapshots = SnapshotRepository(db, settings)
    val importer = ImportManager()
    val textImport = TextImportProvider()
    val screenshotImport by lazy { ScreenshotImportProvider(MlKitOcrEngine(appContext)) }
    val backup = BackupManager(appContext, db, files)
    val reports = PdfReportGenerator(appContext, db, files, snapshots, inspections, settings)
    /** Scope for work that must outlive a screen (e.g. deleting after navigating away). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class CasaApp : Application() {
    lateinit var container: AppContainer
        private set
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CasaFiles.root = filesDir
        container = AppContainer(this)
        appScope.launch {
            container.inspections.seedLibrary()
            container.agent.seed()
        }
    }
}
