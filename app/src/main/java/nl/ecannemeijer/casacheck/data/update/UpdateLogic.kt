package nl.ecannemeijer.casacheck.data.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class ReleaseAsset(val name: String, val url: String, val size: Long, val contentType: String?)

data class ReleaseInfo(val tag: String, val version: List<Int>, val notes: String, val htmlUrl: String?, val apk: ReleaseAsset)

/** Pure helpers for the self-updater (unit tested). */
object UpdateLogic {
    /** "v1.2.3", "1.2", "1.0.1-debug" -> [1,2,3]; null when not a version. */
    fun parseVersion(s: String?): List<Int>? {
        val core = s?.trim()?.removePrefix("v")?.removePrefix("V")?.substringBefore('-')?.substringBefore('+') ?: return null
        val parts = core.split('.')
        if (parts.isEmpty() || parts.size > 4) return null
        val nums = parts.map { it.toIntOrNull() ?: return null }
        return (nums + List(3) { 0 }).take(maxOf(3, nums.size))
    }

    fun compare(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    fun isNewer(remoteTag: String?, current: String?): Boolean {
        val r = parseVersion(remoteTag) ?: return false
        val c = parseVersion(current) ?: return false
        return compare(r, c) > 0
    }

    /** Prefers a "CasaCheck*.apk" asset, otherwise any .apk; never non-APK files. */
    fun selectApk(assets: List<ReleaseAsset>): ReleaseAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) && it.url.startsWith("https://") }
        return apks.firstOrNull { it.name.startsWith("CasaCheck", ignoreCase = true) } ?: apks.firstOrNull()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses the GitHub "latest release" JSON; drafts and prereleases are ignored. */
    fun parseRelease(body: String): ReleaseInfo? = runCatching {
        val o = json.parseToJsonElement(body).jsonObject
        if (o.bool("draft") == true || o.bool("prerelease") == true) return null
        val tag = o["tag_name"]?.jsonPrimitive?.content ?: return null
        val version = parseVersion(tag) ?: return null
        val assets = o["assets"]?.jsonArray.orEmpty().mapNotNull { e ->
            val a = e.jsonObject
            val name = a["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val url = a["browser_download_url"]?.jsonPrimitive?.content ?: return@mapNotNull null
            ReleaseAsset(name, url, a["size"]?.jsonPrimitive?.longOrNull ?: -1, a["content_type"]?.jsonPrimitive?.content)
        }
        val apk = selectApk(assets) ?: return null
        ReleaseInfo(tag, version, o["body"]?.jsonPrimitive?.content.orEmpty(), o["html_url"]?.jsonPrimitive?.content, apk)
    }.getOrNull()

    private fun JsonObject.bool(k: String) = this[k]?.jsonPrimitive?.booleanOrNull
}