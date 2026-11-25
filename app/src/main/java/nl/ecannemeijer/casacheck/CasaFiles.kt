package nl.ecannemeijer.casacheck

import java.io.File

/** Resolves DB-relative file paths for UI image loading. Initialised by [CasaApp]. */
object CasaFiles {
    @Volatile var root: File? = null
    fun file(rel: String?): File? = rel?.let { if (it.startsWith("/")) File(it) else root?.let { r -> File(r, it) } }
}
