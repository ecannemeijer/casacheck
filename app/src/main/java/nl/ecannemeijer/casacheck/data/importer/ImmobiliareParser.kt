package nl.ecannemeijer.casacheck.data.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.jsoup.Jsoup

/**
 * Parser for Immobiliare.it detail pages. Immobiliare is a Next.js site; the structured listing data
 * is in the `__NEXT_DATA__` JSON at props.pageProps.detailData.realEstate (verified on real pages,
 * see fixtures). Falls back to Open Graph / JSON-LD.
 */
object ImmobiliareParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(html: String, url: String?, externalId: String?): ListingData {
        val doc = Jsoup.parse(html, url ?: "https://www.immobiliare.it/")
        val generic = GenericMetaParser.parse(doc, ListingSite.IMMOBILIARE.id, url)
        val next = doc.selectFirst("script#__NEXT_DATA__")?.data() ?: return generic.copy(externalId = externalId)
        val root = runCatching { json.parseToJsonElement(next) }.getOrNull() ?: return generic.copy(externalId = externalId)
        val re = root.path("props", "pageProps", "detailData", "realEstate") as? JsonObject
            ?: return generic.copy(externalId = externalId)
        val props = (re["properties"] as? JsonArray)?.map { it as JsonObject }.orEmpty()
        val p = props.firstOrNull { it.bool("isMain") == true } ?: props.firstOrNull() ?: JsonObject(emptyMap())
        val loc = p["location"] as? JsonObject
        val address = listOfNotNull(loc.str("address"), loc.str("streetNumber")).joinToString(" ").ifBlank { null }
        val energy = p["energy"] as? JsonObject
        val features = (p["features"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
        val agency = re.path("advertiser", "agency") as? JsonObject
        val supervisor = re.path("advertiser", "supervisor") as? JsonObject
        val roomsText = p.str("rooms")
        val photos = mutableListOf<ImportPhoto>()
        val mm = p["multimedia"] as? JsonObject
        (mm?.get("photos") as? JsonArray)?.forEach { ph ->
            val o = ph as? JsonObject ?: return@forEach
            val urls = o["urls"] as? JsonObject
            val best = urls.str("large") ?: urls.str("medium") ?: urls.str("small") ?: return@forEach
            photos += ImportPhoto(best, photos.size, o.str("caption"), false, urls.str("thumb"))
        }
        (mm?.get("floorplans") as? JsonArray)?.forEach { ph ->
            val o = ph as? JsonObject ?: return@forEach
            val urls = o["urls"] as? JsonObject
            val best = urls.str("large") ?: urls.str("medium") ?: return@forEach
            photos += ImportPhoto(best, photos.size, o.str("caption"), true, urls.str("thumb"))
        }
        var d = ListingData(
            site = ListingSite.IMMOBILIARE.id,
            url = url,
            externalId = externalId ?: re.str("id"),
            title = re.str("title") ?: p.str("caption"),
            address = address,
            city = loc.str("city"),
            province = loc.str("province"),
            region = loc.str("region"),
            country = (loc?.get("nation") as? JsonObject).str("name"),
            latitude = loc?.num("latitude"),
            longitude = loc?.num("longitude"),
            price = (re["price"] as? JsonObject)?.long("value") ?: (p["price"] as? JsonObject)?.long("value"),
            pricePerSqm = ParseUtils.parseEuro((p["price"] as? JsonObject).str("pricePerSquareMeter")),
            buildYear = ((p["buildingYear"] ?: p["buildYear"]) as? JsonPrimitive)?.intOrNull,
            propertyType = (p["typology"] as? JsonObject).str("name"),
            livingArea = ParseUtils.parseArea(p.str("surface")),
            rooms = roomsText?.takeIf { !it.contains("+") }?.toIntOrNull(),
            bathrooms = p.str("bathrooms")?.takeIf { !it.contains("+") }?.toIntOrNull(),
            floors = ParseUtils.firstInt(p.str("floors")),
            condition = p.str("condition"),
            energyLabel = ((energy?.get("class") as? JsonObject).str("name")),
            energyConsumption = energy.str("epi")?.let { epi -> "EPI $epi kWh/m²/anno" },
            heatingType = (p.str("heating") ?: p.str("ga4Heating")),
            description = p.str("description"),
            referenceNumber = (re["reference"] as? JsonObject).str("code"),
            agentName = agency.str("displayName") ?: supervisor.str("displayName"),
            agentPhone = ((agency?.get("phones") as? JsonArray)?.firstOrNull() as? JsonObject).str("value"),
            photos = photos,
            listingActive = (re["state"] as? JsonObject).str("name")?.let { it.equals("attivo", true) || it.equals("active", true) },
        )
        if (roomsText != null) d = d.copy(extraFeatures = d.extraFeatures + ("ROOMS_TEXT" to roomsText))
        (p["floor"] as? JsonObject).str("value")?.let { d = d.copy(extraFeatures = d.extraFeatures + ("FLOOR_TEXT" to it)) }
        for (f in features) d = FeatureLineParser.apply(f, d)
        (p["primaryFeatures"] as? JsonArray)?.forEach { pf ->
            val o = pf as? JsonObject ?: return@forEach
            val name = o.str("name") ?: return@forEach
            val visible = o.bool("isVisible") == true
            if (visible) d = FeatureLineParser.apply(name, d)
        }
        return d.fillFrom(generic)
    }

    private fun JsonElement?.path(vararg keys: String): JsonElement? {
        var cur: JsonElement? = this
        for (k in keys) cur = (cur as? JsonObject)?.get(k) ?: return null
        return cur
    }

    private fun JsonObject?.str(key: String): String? {
        val e = this?.get(key) ?: return null
        if (e is JsonNull) return null
        return (e as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
    }

    private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.doubleOrNull
    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
}
