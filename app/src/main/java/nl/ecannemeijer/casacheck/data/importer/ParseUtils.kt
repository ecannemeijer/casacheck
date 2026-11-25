package nl.ecannemeijer.casacheck.data.importer

object ParseUtils {
    /** "269.000 €" / "€ 1.250.000" / "420000" -> 269000. Returns null when no plausible amount. */
    fun parseEuro(text: String?): Long? {
        if (text == null) return null
        val m = Regex("(\\d{1,3}(?:[. \u00A0']\\d{3})+|\\d+)(?:,\\d{1,2})?").find(text) ?: return null
        return m.groupValues[1].replace(Regex("[. \u00A0']"), "").toLongOrNull()
    }

    /** "126 m²" / "1.200 mq" / "135,5 m2" -> 126.0 */
    fun parseArea(text: String?): Double? {
        if (text == null) return null
        val m = Regex("(\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,(\\d+))?\\s*(?:m²|m2|mq|sqm|m\\b)", RegexOption.IGNORE_CASE).find(text) ?: return null
        val whole = m.groupValues[1].replace(".", "")
        val dec = m.groupValues[2]
        return (if (dec.isNotEmpty()) "$whole.$dec" else whole).toDoubleOrNull()
    }

    fun firstInt(text: String?): Int? = text?.let { Regex("\\d+").find(it)?.value?.toIntOrNull() }

    fun clean(s: String?): String? = s?.replace('\u00A0', ' ')?.replace(Regex("[ \\t]+"), " ")?.trim()?.takeIf { it.isNotEmpty() }

    fun containsAny(text: String, vararg words: String) = words.any { text.contains(it, ignoreCase = true) }
}

/**
 * Interprets single feature lines as shown on listing sites ("4 locali", "2 badkamers",
 * "Built in 1970", "Riscaldamento autonomo: Gas naturale") in Italian, Dutch and English.
 * Patterns were taken from real Idealista pages (IT/NL/EN) and Immobiliare.it data.
 */
object FeatureLineParser {
    private val rooms = Regex("^(\\d+)\\s*(locali|locale|vani|kamers|kamer|kmr\\.?|rooms?)\\b", RegexOption.IGNORE_CASE)
    private val bedrooms = Regex("(\\d+)\\s*(camere da letto|camere|slaapkamers?|bedrooms?)\\b", RegexOption.IGNORE_CASE)
    private val baths = Regex("(\\d+)\\s*(bagni|bagno|badkamers?|bathrooms?)\\b", RegexOption.IGNORE_CASE)
    private val year = Regex("(costruito nel|anno di costruzione|bouwjaar|built in|year built)\\D{0,5}(1[5-9]\\d{2}|20\\d{2})", RegexOption.IGNORE_CASE)
    private val plot = Regex("(terreno|giardino|perceel|plot|land)[^\\d]{0,20}(\\d[\\d.]*)\\s*(m²|m2|mq)", RegexOption.IGNORE_CASE)
    private val plotAfter = Regex("(\\d[\\d.]*)\\s*(m²|m2|mq)\\s*(di )?(terreno|perceel|plot|grond|land)", RegexOption.IGNORE_CASE)
    private val usable = Regex("(\\d[\\d.,]*)\\s*(m²|m2|mq)\\s*(calpestabili|utili|woonoppervlakte|floor area|usable)", RegexOption.IGNORE_CASE)
    private val built = Regex("^(\\d[\\d.,]*)\\s*(m²|m2|mq)\\s*(commerciali|bebouwd|built|lordi)?", RegexOption.IGNORE_CASE)
    private val kwh = Regex("\\(?([\\d.,]+\\s*kWh/m²[^)]*)\\)?", RegexOption.IGNORE_CASE)

    fun apply(line: String, d: ListingData): ListingData {
        val l = ParseUtils.clean(line) ?: return d
        var r = d
        usable.find(l)?.let { r = r.copy(livingArea = r.livingArea ?: ParseUtils.parseArea(it.value)) }
        if (r.livingArea == null) built.find(l)?.let { if (!l.contains("terreno", true) && !l.contains("perceel", true)) r = r.copy(livingArea = ParseUtils.parseArea(it.value)) }
        (plot.find(l) ?: plotAfter.find(l))?.let { m -> r = r.copy(plotArea = r.plotArea ?: ParseUtils.parseArea(m.value)) }
        rooms.find(l)?.let { r = r.copy(rooms = r.rooms ?: it.groupValues[1].toIntOrNull()) }
        bedrooms.find(l)?.let { if (!l.contains("bagn", true)) r = r.copy(bedrooms = r.bedrooms ?: it.groupValues[1].toIntOrNull()) }
        baths.find(l)?.let { r = r.copy(bathrooms = r.bathrooms ?: it.groupValues[1].toIntOrNull()) }
        year.find(l)?.let { r = r.copy(buildYear = r.buildYear ?: it.groupValues[2].toIntOrNull()) }
        val lower = l.lowercase()
        when {
            ParseUtils.containsAny(lower, "classe energetica", "energielabel", "energy efficiency rating", "energy rating", "prestazione energetica") ->
                kwh.find(l)?.let { r = r.copy(energyConsumption = r.energyConsumption ?: ParseUtils.clean(it.groupValues[1])) }
            ParseUtils.containsAny(lower, "riscaldamento", "verwarming", "heating") ->
                r = r.copy(heatingType = r.heatingType ?: l)
            ParseUtils.containsAny(lower, "buono stato", "ottimo stato", "da ristrutturare", "ristrutturato", "nuova costruzione",
                "goede staat", "te renoveren", "gerenoveerd", "nieuwbouw", "good condition", "to renovate", "renovated", "new build", "abitabile") ->
                r = r.copy(condition = r.condition ?: l)
        }
        if (ParseUtils.containsAny(lower, "balcone", "balconi", "balkon", "balcony")) r = r.copy(balcony = true)
        if (ParseUtils.containsAny(lower, "terrazzo", "terrazza", "terras", "terrace")) r = r.copy(terrace = true)
        if (ParseUtils.containsAny(lower, "giardino", "tuin", "garden")) r = r.copy(garden = true)
        if (ParseUtils.containsAny(lower, "cantina", "kelder", "cellar", "basement", "storage room", "berging")) r = r.copy(cellar = true)
        if (ParseUtils.containsAny(lower, "soffitta", "mansarda", "zolder", "attic", "loft")) r = r.copy(attic = true)
        if (ParseUtils.containsAny(lower, "aria condizionata", "climatizzat", "airconditioning", "air conditioning")) r = r.copy(airConditioning = true)
        if (Regex("\\b(box|garage|autorimessa)\\b", RegexOption.IGNORE_CASE).containsMatchIn(l)) r = r.copy(garage = true)
        if (ParseUtils.containsAny(lower, "posto auto", "parkeerplaats", "parking space")) {
            r = r.copy(parkingSpaces = r.parkingSpaces ?: (Regex("(\\d+)\\s*(posti auto|parkeerplaatsen|parking spaces)", RegexOption.IGNORE_CASE).find(l)?.groupValues?.get(1)?.toIntOrNull()))
        }
        if (ParseUtils.containsAny(lower, "ascensore", "met lift", "with lift", "elevator")) r = r.copy(extraFeatures = r.extraFeatures + ("LIFT" to "true"))
        if (ParseUtils.containsAny(lower, "senza ascensore", "zonder lift", "without lift")) r = r.copy(extraFeatures = r.extraFeatures + ("LIFT" to "false"))
        if (Regex("^(\\d+|piano terra|bajo|ground)\\s*(º|°|e|th|st|nd|rd)?\\s*(piano|verdieping|floor)", RegexOption.IGNORE_CASE).containsMatchIn(l) || ParseUtils.containsAny(lower, "piano terra", "begane grond", "ground floor"))
            r = r.copy(extraFeatures = r.extraFeatures + ("FLOOR_TEXT" to l))
        if (ParseUtils.containsAny(lower, "orientamento", "esposizione", "oriëntatie", "orientatie", "orientation")) r = r.copy(extraFeatures = r.extraFeatures + ("ORIENTATION" to l))
        if (ParseUtils.containsAny(lower, "fossa settica", "fossa biologica", "septic")) r = r.copy(extraFeatures = r.extraFeatures + ("SEPTIC_TANK" to "true"))
        if (ParseUtils.containsAny(lower, "indipendente", "vrijstaand", "detached")) r = r.copy(extraFeatures = r.extraFeatures + ("DETACHED" to "true"))
        return r
    }
}
