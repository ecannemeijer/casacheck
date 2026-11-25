package nl.ecannemeijer.casacheck.importer

import com.google.common.truth.Truth.assertThat
import nl.ecannemeijer.casacheck.data.importer.BlockDetector
import nl.ecannemeijer.casacheck.data.importer.IdealistaParser
import nl.ecannemeijer.casacheck.data.importer.ImmobiliareParser
import nl.ecannemeijer.casacheck.data.importer.TextListingParser
import org.junit.Test

/** HTML parsing against stored real pages (archived copies, see DEVELOPMENT.md) – no live websites. */
class ParserTest {
    @Test fun idealistaItalianPage() {
        val d = IdealistaParser.parse(Fixtures.load("idealista_it_10015684.html"), "https://www.idealista.it/immobile/10015684/", "10015684")
        assertThat(d.title).isEqualTo("Appartamento su due piani in vendita in Piazza del Vecchio Ghetto")
        assertThat(d.city).isEqualTo("Verucchio")
        assertThat(d.address).isEqualTo("Piazza del Vecchio Ghetto")
        assertThat(d.province).isEqualTo("Rimini")
        assertThat(d.price).isEqualTo(269_000)
        assertThat(d.pricePerSqm).isEqualTo(2_135)
        assertThat(d.livingArea).isEqualTo(126.0)
        assertThat(d.rooms).isEqualTo(4)
        assertThat(d.bathrooms).isEqualTo(2)
        assertThat(d.buildYear).isEqualTo(2007)
        assertThat(d.balcony).isTrue()
        assertThat(d.garage).isTrue()
        assertThat(d.heatingType).contains("Gas naturale")
        assertThat(d.energyConsumption).contains("55,98")
        assertThat(d.energyLabel).isNull() // no title attribute on the icon -> not guessed
        assertThat(d.condition).isEqualTo("Buono stato")
        assertThat(d.referenceNumber).isEqualTo("1547P (55207350)")
        assertThat(d.agentName).isEqualTo("casavilla immobiliare di angeli paolo")
        assertThat(d.description).startsWith("VILLA VERUCCHIO")
        assertThat(d.extraFeatures["LIFT"]).isEqualTo("true")
        assertThat(d.photos.size).isEqualTo(14)
        assertThat(d.photos.first().url).isEqualTo("https://img4.idealista.it/blur/WEB_DETAIL/0/id.pro.it.image.master/e2/90/5d/157906832.jpg")
        assertThat(d.photos.first().caption).isEqualTo("Cucina")
        assertThat(d.photos.map { it.position }).isEqualTo((0 until 14).toList())
        assertThat(d.photos.map { it.url }.toSet().size).isEqualTo(14)
    }

    @Test fun idealistaDutchAndEnglishPages() {
        val nl = IdealistaParser.parse(Fixtures.load("idealista_nl_10003361.html"), "https://www.idealista.it/nl/immobile/10003361/", "10003361")
        assertThat(nl.price).isEqualTo(900)
        assertThat(nl.livingArea).isEqualTo(135.0)
        assertThat(nl.rooms).isEqualTo(4)
        assertThat(nl.bathrooms).isEqualTo(2)
        assertThat(nl.buildYear).isEqualTo(1970)
        assertThat(nl.energyLabel).isEqualTo("G")
        assertThat(nl.airConditioning).isTrue()
        assertThat(nl.heatingType).isEqualTo("Centrale verwarming")
        assertThat(nl.city).isEqualTo("Bari")
        val en = IdealistaParser.parse(Fixtures.load("idealista_en_10003361.html"), "https://www.idealista.it/en/immobile/10003361/", "10003361")
        assertThat(en.livingArea).isEqualTo(135.0)
        assertThat(en.buildYear).isEqualTo(1970)
        assertThat(en.bathrooms).isEqualTo(2)
        assertThat(en.heatingType).isEqualTo("Central heating")
        assertThat(en.photos).isNotEmpty()
    }

    @Test fun immobiliareNextData() {
        val d = ImmobiliareParser.parse(Fixtures.load("immobiliare_100001050.html"), "https://www.immobiliare.it/annunci/100001050/", "100001050")
        assertThat(d.price).isEqualTo(420_000)
        assertThat(d.pricePerSqm).isEqualTo(933)
        assertThat(d.city).isEqualTo("Presicce-Acquarica")
        assertThat(d.province).isEqualTo("Lecce")
        assertThat(d.region).isEqualTo("Puglia")
        assertThat(d.country).isEqualTo("Italia")
        assertThat(d.address).isEqualTo("Corso Italia 31")
        assertThat(d.latitude).isWithin(1e-6).of(39.89989853)
        assertThat(d.buildYear).isEqualTo(1600)
        assertThat(d.bathrooms).isEqualTo(2)
        assertThat(d.rooms).isNull() // "5+" is not an exact number
        assertThat(d.extraFeatures["ROOMS_TEXT"]).isEqualTo("5+")
        assertThat(d.livingArea).isEqualTo(450.0)
        assertThat(d.floors).isEqualTo(2)
        assertThat(d.energyLabel).isEqualTo("G")
        assertThat(d.condition).isEqualTo("Da ristrutturare")
        assertThat(d.balcony).isTrue()
        assertThat(d.referenceNumber).isEqualTo("102")
        assertThat(d.agentName).isEqualTo("Dimora nel Salento di Andrea Imperato")
        assertThat(d.agentPhone).isEqualTo("0833 172 0103")
        assertThat(d.listingActive).isTrue()
        assertThat(d.photos.count { !it.isFloorplan }).isEqualTo(16)
        assertThat(d.photos.count { it.isFloorplan }).isEqualTo(2)
        assertThat(d.photos.first().url).endsWith("/xxl.jpg")
        assertThat(d.photos.none { it.url.contains("imagenoresize") }).isTrue()
    }

    @Test fun blockPagesAreDetected() {
        assertThat(BlockDetector.detect(403, Fixtures.load("idealista_blocked.html"))).isEqualTo("Cloudflare challenge")
        assertThat(BlockDetector.detect(403, Fixtures.load("immobiliare_blocked.html"))).isEqualTo("DataDome")
        assertThat(BlockDetector.detect(200, Fixtures.load("idealista_it_10015684.html"))).isNull()
        assertThat(BlockDetector.detect(200, Fixtures.load("immobiliare_100001050.html"))).isNull()
        assertThat(BlockDetector.detect(429, "")).isEqualTo("rate limited")
    }

    @Test fun pastedItalianAdvertisementText() {
        val text = """
            Casa indipendente in vendita a Millesimo
            € 89.000
            120 mq, 5 locali, 2 bagni
            Terreno di 1.500 mq
            Anno di costruzione 1930
            Riscaldamento autonomo a legna
            Classe energetica: G
            Rif. SV-221
            Contatti: info@agenzia-esempio.it  tel. 019 123 4567
        """.trimIndent()
        val d = TextListingParser.parse(text)
        assertThat(d.title).isEqualTo("Casa indipendente in vendita a Millesimo")
        assertThat(d.price).isEqualTo(89_000)
        assertThat(d.livingArea).isEqualTo(120.0)
        assertThat(d.plotArea).isEqualTo(1500.0)
        assertThat(d.rooms).isEqualTo(5)
        assertThat(d.bathrooms).isEqualTo(2)
        assertThat(d.buildYear).isEqualTo(1930)
        assertThat(d.energyLabel).isEqualTo("G")
        assertThat(d.referenceNumber).isEqualTo("SV-221")
        assertThat(d.agentEmail).isEqualTo("info@agenzia-esempio.it")
        assertThat(d.heatingType).contains("legna")
        assertThat(d.extraFeatures["DETACHED"]).isEqualTo("true")
    }

    @Test fun unknownValuesStayEmpty() {
        val d = TextListingParser.parse("Mooi huis met uitzicht")
        assertThat(d.price).isNull()
        assertThat(d.livingArea).isNull()
        assertThat(d.rooms).isNull()
        assertThat(d.energyLabel).isNull()
        assertThat(d.foundFields()).containsExactly("title")
    }

    @Test fun wordStartingWithRifIsNotAReference() {
        val d = TextListingParser.parse("Casa in pietra. Tetto rifatto nel 2015. Prezzo 120000 euro")
        assertThat(d.referenceNumber).isNull()
        assertThat(TextListingParser.parse("Rif. AB-12 Casa").referenceNumber).startsWith("AB-12")
    }
}
