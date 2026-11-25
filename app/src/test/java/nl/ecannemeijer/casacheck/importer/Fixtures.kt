package nl.ecannemeijer.casacheck.importer

object Fixtures {
    fun load(name: String): String =
        requireNotNull(javaClass.classLoader!!.getResourceAsStream("fixtures/$name")) { "missing fixture $name" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
}
