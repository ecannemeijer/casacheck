package nl.ecannemeijer.casacheck.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import nl.ecannemeijer.casacheck.domain.score.ScoreCalculator
import nl.ecannemeijer.casacheck.domain.score.ScoreGroup

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Purchase costs (taxes, notary, agency) as % of the price. Indicative; editable. */
    val purchaseCostPercent: Double = 12.0,
    /** Reserve for unforeseen costs as % of the renovation costs. */
    val contingencyPercent: Double = 15.0,
    val weights: Map<String, Int> = ScoreCalculator.DEFAULT_WEIGHTS.mapKeys { it.key.name },
    /** Listing photos are only included in reports when the user explicitly allows it. */
    val listingPhotosInReports: Boolean = false,
    val moistureHeights: String = "10,50,100,150,200",
    val onboardingDone: Boolean = false,
    /** Download the ML Kit translation model (~30 MB) only over Wi-Fi. */
    val translateWifiOnly: Boolean = false,
) {
    fun weightMap(): Map<ScoreGroup, Int> = ScoreGroup.entries.associateWith { weights[it.name] ?: ScoreCalculator.DEFAULT_WEIGHTS[it] ?: 0 }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val purchase = doublePreferencesKey("purchase_cost_percent")
        val contingency = doublePreferencesKey("contingency_percent")
        val listingPhotos = booleanPreferencesKey("listing_photos_in_reports")
        val heights = stringPreferencesKey("moisture_heights")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val translateWifi = booleanPreferencesKey("translate_wifi_only")
        fun weight(g: ScoreGroup) = intPreferencesKey("weight_${g.name}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            purchaseCostPercent = p[K.purchase] ?: 12.0,
            contingencyPercent = p[K.contingency] ?: 15.0,
            weights = ScoreGroup.entries.associate { it.name to (p[K.weight(it)] ?: ScoreCalculator.DEFAULT_WEIGHTS[it] ?: 0) },
            listingPhotosInReports = p[K.listingPhotos] ?: false,
            moistureHeights = p[K.heights] ?: "10,50,100,150,200",
            onboardingDone = p[K.onboarding] ?: false,
            translateWifiOnly = p[K.translateWifi] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val n = transform(current())
        context.dataStore.edit { p ->
            p[K.theme] = n.theme.name
            p[K.purchase] = n.purchaseCostPercent
            p[K.contingency] = n.contingencyPercent
            p[K.listingPhotos] = n.listingPhotosInReports
            p[K.heights] = n.moistureHeights
            p[K.onboarding] = n.onboardingDone
            p[K.translateWifi] = n.translateWifiOnly
            ScoreGroup.entries.forEach { g -> p[K.weight(g)] = n.weights[g.name] ?: 0 }
        }
    }
}
