package nl.ecannemeijer.casacheck.importer

import com.google.common.truth.Truth.assertThat
import nl.ecannemeijer.casacheck.data.importer.ListingSite
import nl.ecannemeijer.casacheck.data.importer.UrlRecognizer
import org.junit.Test

class UrlRecognizerTest {
    @Test fun recognisesIdealistaDutchUrl() {
        val r = UrlRecognizer.recognize("https://www.idealista.it/nl/immobile/37222881/")!!
        assertThat(r.site).isEqualTo(ListingSite.IDEALISTA)
        assertThat(r.listingId).isEqualTo("37222881")
        assertThat(r.language).isEqualTo("nl")
        assertThat(r.normalizedUrl).isEqualTo("https://www.idealista.it/nl/immobile/37222881/")
    }

    @Test fun recognisesLanguageVariantsAndMobileHost() {
        assertThat(UrlRecognizer.recognize("https://www.idealista.it/immobile/37222806/")!!.listingId).isEqualTo("37222806")
        assertThat(UrlRecognizer.recognize("https://www.idealista.it/en/immobile/37222806/?utm=x#foto")!!.language).isEqualTo("en")
        assertThat(UrlRecognizer.recognize("https://m.idealista.it/it/immobile/37222806")!!.normalizedUrl)
            .isEqualTo("https://www.idealista.it/it/immobile/37222806/")
        assertThat(UrlRecognizer.recognize("http://idealista.it/de/immobile/1234567/")!!.normalizedUrl).startsWith("https://")
    }

    @Test fun recognisesImmobiliare() {
        val r = UrlRecognizer.recognize("https://www.immobiliare.it/annunci/100001050/?utm_source=facebook")!!
        assertThat(r.site).isEqualTo(ListingSite.IMMOBILIARE)
        assertThat(r.listingId).isEqualTo("100001050")
        assertThat(UrlRecognizer.recognize("https://www.immobiliare.it/en/annunci/119452311/")!!.language).isEqualTo("en")
    }

    @Test fun rejectsUnsupportedOrUnsafeUrls() {
        assertThat(UrlRecognizer.recognize("https://evil.example/immobile/37222881/")).isNull()
        assertThat(UrlRecognizer.recognize("https://idealista.it.evil.com/immobile/37222881/")).isNull()
        assertThat(UrlRecognizer.recognize("ftp://www.idealista.it/immobile/37222881/")).isNull()
        assertThat(UrlRecognizer.recognize("javascript:alert(1)")).isNull()
        assertThat(UrlRecognizer.recognize("https://user:pw@www.idealista.it/immobile/37222881/")).isNull()
        assertThat(UrlRecognizer.recognize("https://www.idealista.it/vendita-case/savona/")).isNull()
        assertThat(UrlRecognizer.recognize("")).isNull()
        assertThat(UrlRecognizer.recognize(null)).isNull()
    }

    @Test fun extractsUrlFromSharedText() {
        val shared = "Guarda questo immobile su idealista: https://www.idealista.it/immobile/37222881/ Bellissimo!"
        assertThat(UrlRecognizer.extractUrl(shared)).isEqualTo("https://www.idealista.it/immobile/37222881/")
        assertThat(UrlRecognizer.extractUrl("geen link hier")).isNull()
    }
}
