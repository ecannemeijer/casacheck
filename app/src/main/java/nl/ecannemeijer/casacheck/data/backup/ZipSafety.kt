package nl.ecannemeijer.casacheck.data.backup

import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Safe ZIP extraction: blocks zip-slip (../, absolute paths, backslashes), limits entry count and total size. */
object ZipSafety {
    const val MAX_ENTRIES = 50_000
    const val MAX_TOTAL_BYTES = 8L * 1024 * 1024 * 1024

    fun isSafeName(name: String): Boolean {
        if (name.isEmpty() || name.length > 512) return false
        if (name.startsWith("/") || name.contains('\\') || name.contains('\u0000')) return false
        if (Regex("^[A-Za-z]:").containsMatchIn(name)) return false
        return name.split('/').none { it == ".." }
    }

    fun safeTarget(destDir: File, name: String): File {
        if (!isSafeName(name)) throw BackupException(BackupException.Reason.UNSAFE_PATH, "unsafe entry: $name")
        val target = File(destDir, name)
        val canonicalDest = destDir.canonicalPath + File.separator
        if (!target.canonicalPath.startsWith(canonicalDest)) throw BackupException(BackupException.Reason.UNSAFE_PATH, "unsafe entry: $name")
        return target
    }

    fun extract(input: InputStream, destDir: File): List<String> {
        destDir.mkdirs()
        val names = mutableListOf<String>()
        var total = 0L
        ZipInputStream(input.buffered()).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                if (names.size >= MAX_ENTRIES) throw BackupException(BackupException.Reason.TOO_LARGE)
                val target = safeTarget(destDir, e.name)
                if (e.isDirectory) { target.mkdirs(); continue }
                target.parentFile?.mkdirs()
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = zin.read(buf); if (n < 0) break
                        total += n
                        if (total > MAX_TOTAL_BYTES) throw BackupException(BackupException.Reason.TOO_LARGE)
                        out.write(buf, 0, n)
                    }
                }
                names += e.name
            }
        }
        return names
    }
}
