package nl.ecannemeijer.casacheck.data.importer

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Parser for Idealista detail pages. Selectors are based on real pages (IT/NL/EN) captured in
 * test fixtures (see DEVELOPMENT.md): .main-info__title-main, .main-info__title-minor,
 * .info-data-price, .details-property_features li, .header-map-list, .adCommentsLanguage,
 * .txt-ref, .professional-name, and the inline `adMultimediasInfo.fullScreenGalleryPics` gallery.
 */
object IdealistaParser {
    fun parse(html: String, url: String?, externalId: String?): ListingData {
        val doc = Jsoup.parse(html, url ?: "https://www.idealista.it/")
        var d = ListingData(site = ListingSite.IDEALISTA.id, url = url, externalId = externalId)
        val title = ParseUtils.clean(doc.selectFirst(".main-info__title-main")?.text())
        val minor = ParseUtils.clean(doc.selectFirst(".main-info__title-minor")?.text())
        val mapItems = doc.select("li.header-map-list").mapNotNull { ParseUtils.clean(it.text()) }
        val price = ParseUtils.parseEuro(doc.selectFirst(".info-data-price")?.text())
        val sqm = doc.select(".squaredmeterprice .flex-feature-details").lastOrNull()?.text()?.let { ParseUtils.parseEuro(it) }
        val typology = ParseUtils.clean(doc.selectFirst("strong.typology")?.text())
        d = d.copy(
            title = title ?: ParseUtils.clean(meta(doc, "og:title")?.substringBefore(" — ")?.substringBefore(" &#8212;")),
            address = mapItems.firstOrNull(),
            city = minor?.substringAfterLast(",")?.trim()?.ifBlank { null } ?: mapItems.lastOrNull(),
            province = mapItems.lastOrNull()?.takeIf { it.contains(",") }?.substringAfterLast(",")?.trim(),
            country = if (url?.contains("idealista.it") != false) "Italia" else null,
            price = price,
            pricePerSqm = sqm,
            propertyType = typology?.replaceFirstChar { it.uppercase() },
            description = doc.selectFirst(".adCommentsLanguage")?.let { el ->
                el.select("br").forEach { br -> br.before(org.jsoup.nodes.TextNode("\n")) }
                el.wholeText().lines().map { it.trim() }.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim().ifBlank { null }
            },
            referenceNumber = ParseUtils.clean(doc.selectFirst(".txt-ref")?.text()),
            agentName = ParseUtils.clean(doc.selectFirst(".professional-name span")?.ownText())
                ?: ParseUtils.clean(doc.selectFirst("input[name=user-name]")?.attr("value")),
        )
        for (li in doc.select(".details-property_features li")) d = FeatureLineParser.apply(li.text(), d)
        // Energy label: Idealista renders it as an icon with a title attribute ("g"); without title we leave it empty.
        doc.selectFirst("span[class^=icon-energy][title]")?.attr("title")?.takeIf { it.isNotBlank() }?.let {
            d = d.copy(energyLabel = it.uppercase())
        }
        // GPS (only when the page actually exposes coordinates)
        Regex("latitude:\\s*'(-?\\d+\\.\\d+)'").find(html)?.let { lat ->
            Regex("longitude:\\s*'(-?\\d+\\.\\d+)'").find(html)?.let { lon ->
                d = d.copy(latitude = lat.groupValues[1].toDoubleOrNull(), longitude = lon.groupValues[1].toDoubleOrNull())
            }
        }
        var photos = parseGallery(html)
        if (photos.isEmpty()) meta(doc, "og:image")?.let { photos = listOf(ImportPhoto(it, 0)) }
        d = d.copy(photos = photos)
        val jsonLd = GenericMetaParser.parse(doc, ListingSite.IDEALISTA.id, url)
        return d.fillFrom(jsonLd.copy(photos = emptyList()))
    }

    /** Extracts the full gallery from the inline JavaScript object (keys partly unquoted, so regex based). */
    fun parseGallery(html: String): List<ImportPhoto> {
        val start = html.indexOf("fullScreenGalleryPics")
        if (start < 0) return emptyList()
        val arrStart = html.indexOf('[', start)
        if (arrStart < 0) return emptyList()
        var depth = 0
        var end = -1
        for (i in arrStart until minOf(html.length, arrStart + 500_000)) {
            when (html[i]) { '[' -> depth++; ']' -> { depth--; if (depth == 0) { end = i; break } } }
        }
        if (end < 0) return emptyList()
        val arr = html.substring(arrStart, end + 1)
        val objects = arr.split(Regex("\\},\\s*\\{"))
        val result = mutableListOf<ImportPhoto>()
        val seen = HashSet<String>()
        objects.forEachIndexed { idx, obj ->
            val src = Regex("imageDataService\"?\\s*:\\s*\"([^\"]+)\"").find(obj)?.groupValues?.get(1) ?: return@forEachIndexed
            val url = src.replace("\\/", "/")
            if (!seen.add(url)) return@forEachIndexed
            val pos = Regex("absolutePosition\"?\\s*:\\s*(\\d+)").find(obj)?.groupValues?.get(1)?.toIntOrNull()
            val isPlan = Regex("\"?isPlan\"?\\s*:\\s*true").containsMatchIn(obj)
            val tag = Regex("\"?tag\"?\\s*:\\s*\"([^\"]*)\"").find(obj)?.groupValues?.get(1)?.ifBlank { null }
            result += ImportPhoto(url = url, position = (pos?.minus(1)) ?: idx, caption = tag, isFloorplan = isPlan || (tag?.contains("plan", true) == true))
        }
        return result.sortedBy { it.position }.mapIndexed { i, p -> p.copy(position = i) }
    }

    private fun meta(doc: Document, property: String) =
        doc.selectFirst("meta[property=$property]")?.attr("content")?.takeIf { it.isNotBlank() }
}
