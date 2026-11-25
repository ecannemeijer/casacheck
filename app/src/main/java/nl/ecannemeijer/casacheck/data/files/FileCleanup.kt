package nl.ecannemeijer.casacheck.data.files

import java.io.File

/**
 * Pure file clean-up helpers (no Android dependencies, unit-tested).
 * Paths in the DB are relative to filesDir; nothing outside [root] is ever deleted.
 */
object FileCleanup {
    /** Sub-directories of filesDir that hold one folder per property (named after the property's uuid). */
    val PROPERTY_DIRS = listOf("photos", "thumbs", "docs", "edited")
    private val SAFE_UUID = Regex("[A-Za-z0-9-]{8,64}")

    data class Plan(val files: List<String>, val dirs: List<String>)

    /** Everything that belongs to one property on disk: its own folders plus individually referenced files (e.g. PDF reports). */
    fun planForProperty(uuid: String, referencedFiles: List<String?>): Plan {
        val dirs = if (SAFE_UUID.matches(uuid)) PROPERTY_DIRS.map { "$it/$uuid" } else emptyList()
        return Plan(referencedFiles.filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }.distinct(), dirs)
    }

    fun resolve(root: File, rel: String): File = if (rel.startsWith("/")) File(rel) else File(root, rel)

    /** True when [f] lies strictly inside [root] (after resolving "..", symlinks etc.). */
    fun isInside(root: File, f: File): Boolean {
        val r = root.canonicalFile.toPath()
        val p = f.canonicalFile.toPath()
        return p != r && p.startsWith(r)
    }

    /** Deletes one referenced file if it exists inside [root]. Returns true if something was deleted. */
    fun deleteFile(root: File, rel: String?): Boolean {
        if (rel.isNullOrBlank()) return false
        val f = resolve(root, rel)
        return f.isFile && isInside(root, f) && f.delete()
    }

    /** Executes a plan; returns the number of files and folders removed. */
    fun execute(root: File, plan: Plan): Int {
        var n = 0
        plan.files.forEach { if (deleteFile(root, it)) n++ }
        plan.dirs.forEach { rel ->
            val d = resolve(root, rel)
            if (d.isDirectory && isInside(root, d) && d.deleteRecursively()) n++
        }
        return n
    }
}