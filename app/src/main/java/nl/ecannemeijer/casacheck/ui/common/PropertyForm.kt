package nl.ecannemeijer.casacheck.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import nl.ecannemeijer.casacheck.R
import nl.ecannemeijer.casacheck.data.db.Property
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

enum class FieldKind { TEXT, MULTILINE, LONG, DOUBLE, INT, BOOL, PHONE, EMAIL, URL }

data class FieldDef(val name: String, val label: Int, val kind: FieldKind, val get: (Property) -> Any?, val set: (Property, String?, Boolean?) -> Property)

private fun s(v: String?) = v?.trim()?.ifEmpty { null }

/** All editable property fields (section 4). Unknown values stay empty. */
val PROPERTY_FIELDS: List<Pair<Int, List<FieldDef>>> = listOf(
    R.string.group_basic to listOf(
        FieldDef("title", R.string.f_title, FieldKind.TEXT, { it.title }) { p, v, _ -> p.copy(title = v?.trim() ?: "") },
        FieldDef("address", R.string.f_address, FieldKind.TEXT, { it.address }) { p, v, _ -> p.copy(address = s(v)) },
        FieldDef("city", R.string.f_city, FieldKind.TEXT, { it.city }) { p, v, _ -> p.copy(city = s(v)) },
        FieldDef("province", R.string.f_province, FieldKind.TEXT, { it.province }) { p, v, _ -> p.copy(province = s(v)) },
        FieldDef("region", R.string.f_region, FieldKind.TEXT, { it.region }) { p, v, _ -> p.copy(region = s(v)) },
        FieldDef("country", R.string.f_country, FieldKind.TEXT, { it.country }) { p, v, _ -> p.copy(country = s(v)) },
        FieldDef("latitude", R.string.f_latitude, FieldKind.DOUBLE, { it.latitude }) { p, v, _ -> p.copy(latitude = v?.let(Fmt::parseDouble)?.takeIf { it in -90.0..90.0 }) },
        FieldDef("longitude", R.string.f_longitude, FieldKind.DOUBLE, { it.longitude }) { p, v, _ -> p.copy(longitude = v?.let(Fmt::parseDouble)?.takeIf { it in -180.0..180.0 }) },
    ),
    R.string.group_price to listOf(
        FieldDef("askingPrice", R.string.f_asking_price, FieldKind.LONG, { it.askingPrice }) { p, v, _ -> p.copy(askingPrice = v?.let(Fmt::parseLong)) },
        FieldDef("originalPrice", R.string.f_original_price, FieldKind.LONG, { it.originalPrice }) { p, v, _ -> p.copy(originalPrice = v?.let(Fmt::parseLong)) },
        FieldDef("pricePerSqm", R.string.f_price_sqm, FieldKind.LONG, { it.pricePerSqm }) { p, v, _ -> p.copy(pricePerSqm = v?.let(Fmt::parseLong)) },
        FieldDef("ownOffer", R.string.f_own_offer, FieldKind.LONG, { it.ownOffer }) { p, v, _ -> p.copy(ownOffer = v?.let(Fmt::parseLong)) },
    ),
    R.string.group_building to listOf(
        FieldDef("buildYear", R.string.f_build_year, FieldKind.INT, { it.buildYear }) { p, v, _ -> p.copy(buildYear = v?.trim()?.toIntOrNull()?.takeIf { it in 1000..2100 }) },
        FieldDef("propertyType", R.string.f_property_type, FieldKind.TEXT, { it.propertyType }) { p, v, _ -> p.copy(propertyType = s(v)) },
        FieldDef("livingArea", R.string.f_living_area, FieldKind.DOUBLE, { it.livingArea }) { p, v, _ -> p.copy(livingArea = v?.let(Fmt::parseDouble)) },
        FieldDef("plotArea", R.string.f_plot_area, FieldKind.DOUBLE, { it.plotArea }) { p, v, _ -> p.copy(plotArea = v?.let(Fmt::parseDouble)) },
        FieldDef("rooms", R.string.f_rooms, FieldKind.INT, { it.rooms }) { p, v, _ -> p.copy(rooms = v?.trim()?.toIntOrNull()) },
        FieldDef("bedrooms", R.string.f_bedrooms, FieldKind.INT, { it.bedrooms }) { p, v, _ -> p.copy(bedrooms = v?.trim()?.toIntOrNull()) },
        FieldDef("bathrooms", R.string.f_bathrooms, FieldKind.INT, { it.bathrooms }) { p, v, _ -> p.copy(bathrooms = v?.trim()?.toIntOrNull()) },
        FieldDef("floors", R.string.f_floors, FieldKind.INT, { it.floors }) { p, v, _ -> p.copy(floors = v?.trim()?.toIntOrNull()) },
        FieldDef("condition", R.string.f_condition, FieldKind.TEXT, { it.condition }) { p, v, _ -> p.copy(condition = s(v)) },
        FieldDef("energyLabel", R.string.f_energy_label, FieldKind.TEXT, { it.energyLabel }) { p, v, _ -> p.copy(energyLabel = s(v)?.uppercase()) },
        FieldDef("energyConsumption", R.string.f_energy_consumption, FieldKind.TEXT, { it.energyConsumption }) { p, v, _ -> p.copy(energyConsumption = s(v)) },
        FieldDef("heatingType", R.string.f_heating, FieldKind.TEXT, { it.heatingType }) { p, v, _ -> p.copy(heatingType = s(v)) },
        FieldDef("parkingSpaces", R.string.f_parking, FieldKind.INT, { it.parkingSpaces }) { p, v, _ -> p.copy(parkingSpaces = v?.trim()?.toIntOrNull()) },
    ),
    R.string.group_features to listOf(
        FieldDef("garage", R.string.f_garage, FieldKind.BOOL, { it.garage }) { p, _, b -> p.copy(garage = b) },
        FieldDef("cellar", R.string.f_cellar, FieldKind.BOOL, { it.cellar }) { p, _, b -> p.copy(cellar = b) },
        FieldDef("attic", R.string.f_attic, FieldKind.BOOL, { it.attic }) { p, _, b -> p.copy(attic = b) },
        FieldDef("balcony", R.string.f_balcony, FieldKind.BOOL, { it.balcony }) { p, _, b -> p.copy(balcony = b) },
        FieldDef("terrace", R.string.f_terrace, FieldKind.BOOL, { it.terrace }) { p, _, b -> p.copy(terrace = b) },
        FieldDef("garden", R.string.f_garden, FieldKind.BOOL, { it.garden }) { p, _, b -> p.copy(garden = b) },
        FieldDef("airConditioning", R.string.f_aircon, FieldKind.BOOL, { it.airConditioning }) { p, _, b -> p.copy(airConditioning = b) },
    ),
    R.string.group_listing to listOf(
        FieldDef("description", R.string.f_description, FieldKind.MULTILINE, { it.description }) { p, v, _ -> p.copy(description = s(v)) },
        FieldDef("listingUrl", R.string.f_listing_url, FieldKind.URL, { it.listingUrl }) { p, v, _ -> p.copy(listingUrl = s(v)) },
        FieldDef("listingId", R.string.f_listing_id, FieldKind.TEXT, { it.listingId }) { p, v, _ -> p.copy(listingId = s(v)) },
        FieldDef("referenceNumber", R.string.f_reference, FieldKind.TEXT, { it.referenceNumber }) { p, v, _ -> p.copy(referenceNumber = s(v)) },
        FieldDef("agentName", R.string.f_agent_name, FieldKind.TEXT, { it.agentName }) { p, v, _ -> p.copy(agentName = s(v)) },
        FieldDef("agentPhone", R.string.f_agent_phone, FieldKind.PHONE, { it.agentPhone }) { p, v, _ -> p.copy(agentPhone = s(v)) },
        FieldDef("agentEmail", R.string.f_agent_email, FieldKind.EMAIL, { it.agentEmail }) { p, v, _ -> p.copy(agentEmail = s(v)) },
        FieldDef("notes", R.string.f_personal_notes, FieldKind.MULTILINE, { it.notes }) { p, v, _ -> p.copy(notes = s(v)) },
    ),
)

fun fieldText(v: Any?): String? = when (v) { null -> null; is Double -> Fmt.num(v); else -> v.toString() }

/**
 * Generic property form. [found] marks fields that were found automatically during import.
 * [onChange] receives the field name and a transform so callers can apply it to the latest stored value.
 */
@Composable
fun PropertyForm(p: Property, found: Set<String>?, onChange: (String, (Property) -> Property) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PROPERTY_FIELDS.forEach { (group, fields) ->
            SectionCard(stringResource(group)) {
                fields.forEach { f ->
                    val auto = found?.contains(if (f.name == "askingPrice") "price" else f.name) == true
                    val sup = when { found == null -> null; auto -> stringResource(R.string.found_automatically); else -> stringResource(R.string.not_found_fill_in) }
                    if (f.kind == FieldKind.BOOL) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { TriStateRow(stringResource(f.label), f.get(p) as Boolean?) { b -> onChange(f.name) { x -> f.set(x, null, b) } } }
                            if (auto) Icon(Icons.Filled.AutoAwesome, stringResource(R.string.found_automatically), Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        AutoSaveField(stringResource(f.label), fieldText(f.get(p)), { v -> onChange(f.name) { x -> f.set(x, v, null) } },
                            singleLine = f.kind != FieldKind.MULTILINE, minLines = if (f.kind == FieldKind.MULTILINE) 3 else 1,
                            keyboardType = when (f.kind) {
                                FieldKind.LONG, FieldKind.INT -> KeyboardType.Number; FieldKind.DOUBLE -> KeyboardType.Decimal
                                FieldKind.PHONE -> KeyboardType.Phone; FieldKind.EMAIL -> KeyboardType.Email; FieldKind.URL -> KeyboardType.Uri
                                else -> KeyboardType.Text
                            }, supporting = sup)
                    }
                }
                if (group == R.string.group_listing) ViewingPicker(p.viewingAt) { t -> onChange("viewingAt") { x -> x.copy(viewingAt = t) } }
            }
        }
    }
}

@Composable
fun ViewingPicker(value: Long?, onPick: (Long?) -> Unit) {
    val ctx = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.f_viewing) + ": " + (value?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) } ?: "–"),
            Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = {
            val cal = Calendar.getInstance().apply { value?.let { timeInMillis = it } }
            DatePickerDialog(ctx, { _, y, m, d ->
                TimePickerDialog(ctx, { _, h, min ->
                    cal.set(y, m, d, h, min, 0); onPick(cal.timeInMillis)
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }) { Icon(Icons.Filled.Event, null); Text(stringResource(R.string.pick)) }
        if (value != null) TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.clear)) }
    }
}
