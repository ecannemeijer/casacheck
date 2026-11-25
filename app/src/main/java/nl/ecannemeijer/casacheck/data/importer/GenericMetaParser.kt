package nl.ecannemeijer.casacheck.data.importer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jsoup.nodes.Document

/** Standards-based extraction: schema.org JSON-LD and Open Graph metadata. Works for any website. */
object GenericMetaParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(doc: Document, site: String, url: String?): ListingData {
        var d = ListingData(site = site, url = url)
        fun og(p: String) = doc.selectFirst("meta[property=$p]")?.attr("content")?.takeIf { it.isNotBlank() }
        d = d.copy(
            title = ParseUtils.clean(og("og:title")),
            description = ParseUtils.clean(og("og:description")),
        )
        og("og:image")?.let { d = d.copy(photos = listOf(ImportPhoto(doc.selectFirst("meta[property=og:image]")!!.absUrl("content").ifBlank { it }, 0))) }
        for (script in doc.select("script[type=application/ld+json]")) {
            val el = runCatching { json.parseToJsonElement(script.data()) }.getOrNull() ?: continue
            val objects = flatten(el)
            for (o in objects) {
                val type = (o["@type"] as? JsonPrimitive)?.content.orEmpty() + (o["@type"] as? JsonArray)?.joinToString().orEmpty()
                if (type.contains("Offer", true)) {
                    ParseUtils.parseEuro((o["price"] as? JsonPrimitive)?.content)?.let { d = d.copy(price = d.price ?: it) }
                }
                if (listOf("Residence", "House", "Apartment", "SingleFamilyResidence", "Accommodation", "RealEstateListing", "Product").any { type.contains(it, true) }) {
                    d = d.copy(
                        title = (o["name"] as? JsonPrimitive)?.content ?: d.title,
                        description = (o["description"] as? JsonPrimitive)?.content ?: d.description,
                    )
                    (o["address"] as? JsonObject)?.let { a ->
                        d = d.copy(
                            address = (a["streetAddress"] as? JsonPrimitive)?.content ?: d.address,
                            city = (a["addressLocality"] as? JsonPrimitive)?.content ?: d.city,
                            region = (a["addressRegion"] as? JsonPrimitive)?.content ?: d.region,
                        )
                    }
                    (o["geo"] as? JsonObject)?.let { g ->
                        d = d.copy(
                            latitude = (g["latitude"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                            longitude = (g["longitude"] as? JsonPrimitive)?.content?.toDoubleOrNull(),
                        )
                    }
                    val images = when (val img = o["image"]) {
                        is JsonArray -> img.mapNotNull { (it as? JsonPrimitive)?.content ?: ((it as? JsonObject)?.get("url") as? JsonPrimitive)?.content }
                        is JsonPrimitive -> listOf(img.content)
                        else -> emptyList()
                    }
                    if (images.size > d.photos.size) d = d.copy(photos = images.mapIndexed { i, u -> ImportPhoto(u, i) })
                    (o["offers"] as? JsonObject)?.let { off ->
                        ParseUtils.parseEuro((off["price"] as? JsonPrimitive)?.content)?.let { d = d.copy(price = d.price ?: it) }
                    }
                }
            }
        }
        return d
    }

    private fun flatten(el: JsonElement): List<JsonObject> = when (el) {
        is JsonArray -> el.flatMap { flatten(it) }
        is JsonObject -> listOf(el) + ((el["@graph"] as? JsonArray)?.flatMap { flatten(it) } ?: emptyList())
        else -> emptyList()
    }
}
