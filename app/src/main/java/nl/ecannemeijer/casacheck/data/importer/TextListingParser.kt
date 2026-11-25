package nl.ecannemeijer.casacheck.data.importer

/**
 * Extracts listing data from free text: copied advertisement text or OCR output of screenshots.
 * Recognises Italian, Dutch and English wording. Only clearly recognisable values are filled in.
 */
object TextListingParser {
    private val priceRx = listOf(
        Regex("(?:€|eur|euro)[ \\t\u00A0]*([\\d.' \u00A0]{4,})", RegexOption.IGNORE_CASE),
        Regex("([\\d.' \u00A0]{4,})[ \\t]*(?:€|eur\\b|euro)", RegexOption.IGNORE_CASE),
        Regex("(?:prezzo|vraagprijs|prijs|price|asking price)\\s*:?\\s*([\\d.]{4,})", RegexOption.IGNORE_CASE),
    )
    private val sqmPriceRx = Regex("([\\d.]+)\\s*€\\s*/\\s*m(²|2|q)", RegexOption.IGNORE_CASE)
    private val emailRx = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val phoneRx = Regex("(?:\\+39|\\+31|0039)?[\\s(]*0?\\d{2,4}[)\\s./-]*\\d{3,4}[\\s./-]*\\d{2,5}")
    private val refRx = Regex("(?:rif\\.?|riferimento|codice annuncio|codice dell'annuncio|referentie|referentienummer|ref\\.?|reference)\\s*:?\\s*([A-Za-z0-9/_\\-. ()]{2,30})", RegexOption.IGNORE_CASE)
    private val energyRx = Regex("(?:classe energetica|classe|energielabel|energy (?:class|rating|label)|APE)\\s*:?\\s*\\b(A4|A3|A2|A1|A\\+{1,4}|[A-G])\\b", RegexOption.IGNORE_CASE)
    private val agentRx = Regex("(?:agenzia|makelaar|agency|agent|inserzionista)\\s*:?\\s*([^\\n]{3,60})", RegexOption.IGNORE_CASE)

    fun parse(text: String, site: String = "text"): ListingData {
        val clean = text.replace('\u00A0', ' ').replace("\r", "")
        val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var d = ListingData(site = site)
        val title = lines.firstOrNull { it.length in 8..140 && !it.startsWith("http") && !emailRx.containsMatchIn(it) }
        val price = priceRx.firstNotNullOfOrNull { rx ->
            rx.findAll(clean).mapNotNull { ParseUtils.parseEuro(it.groupValues[1]) }.firstOrNull { it in 5_000..50_000_000 }
        }
        d = d.copy(
            title = title,
            price = price,
            pricePerSqm = sqmPriceRx.find(clean)?.let { ParseUtils.parseEuro(it.groupValues[1]) },
            agentEmail = emailRx.find(clean)?.value,
            agentPhone = phoneRx.findAll(clean).map { it.value.trim() }.firstOrNull { it.count(Char::isDigit) in 8..13 },
            referenceNumber = refRx.find(clean)?.groupValues?.get(1)?.trim(),
            energyLabel = energyRx.find(clean)?.groupValues?.get(1)?.uppercase(),
            agentName = agentRx.find(clean)?.groupValues?.get(1)?.trim(),
            description = clean.trim().takeIf { it.length > 40 },
            url = UrlRecognizer.extractUrl(clean),
        )
        for (line in lines) for (segment in line.split(Regex(",\\s+|\\s[·|•]\\s"))) d = FeatureLineParser.apply(segment, d)
        // Phrases inside running text ("casa di 120 mq con 3 camere")
        if (d.livingArea == null) Regex("(\\d{2,4})\\s*(mq|m²|m2)\\b", RegexOption.IGNORE_CASE).find(clean)?.let { m ->
            val after = clean.substring(m.range.last + 1).take(25).lowercase()
            if (!after.contains("terreno") && !after.contains("perceel") && !after.contains("giardino")) d = d.copy(livingArea = m.groupValues[1].toDoubleOrNull())
        }
        Regex("(?:terreno|giardino|perceel|plot)[^\\d\\n]{0,25}(\\d[\\d.]*)\\s*(mq|m²|m2)", RegexOption.IGNORE_CASE).find(clean)?.let {
            d = d.copy(plotArea = d.plotArea ?: ParseUtils.parseArea(it.groupValues[1] + " m²"))
        }
        UrlRecognizer.recognize(d.url)?.let { d = d.copy(externalId = it.listingId) }
        return d
    }
}
