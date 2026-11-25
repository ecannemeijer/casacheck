package nl.ecannemeijer.casacheck.data.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Result of a page fetch. */
sealed interface FetchResult {
    data class Ok(val finalUrl: String, val body: String, val code: Int) : FetchResult
    data class Blocked(val code: Int, val reason: String) : FetchResult
    data class HttpError(val code: Int) : FetchResult
    data class NetworkError(val message: String) : FetchResult
    data class Rejected(val reason: String) : FetchResult
}

class TooLargeException(limit: Long) : IOException("Response larger than $limit bytes")

/**
 * Careful HTTP client for listing pages and photos:
 * - TLS verification stays on (default OkHttp trust manager, no custom trust).
 * - Redirects are followed manually (max 5) and every hop is validated against [hostPolicy].
 * - Responses are size-limited.
 * - A minimal delay between requests to the same host keeps request volume low.
 * - Anti-bot/CAPTCHA pages are detected and reported; they are never bypassed.
 */
class HttpFetcher(
    private val client: OkHttpClient = defaultClient(),
    private val hostPolicy: (String) -> Boolean = UrlRecognizer::isAllowedPageHost,
    private val userAgent: String = USER_AGENT,
    private val minIntervalMs: Long = 1500,
    private val allowHttp: Boolean = false,
) {
    private val lastRequest = ConcurrentHashMap<String, Long>()

    suspend fun fetchPage(url: String, acceptLanguage: String = "it-IT,it;q=0.9,en;q=0.6", maxBytes: Long = MAX_PAGE_BYTES): FetchResult =
        withContext(Dispatchers.IO) {
            var current = url.toHttpUrlOrNull() ?: return@withContext FetchResult.Rejected("invalid url")
            repeat(MAX_REDIRECTS + 1) {
                if (!current.isHttps && !allowHttp) {
                    current = current.newBuilder().scheme("https").build()
                }
                if (!hostPolicy(current.host)) return@withContext FetchResult.Rejected("host not allowed: ${current.host}")
                throttle(current.host)
                coroutineContext.ensureActive()
                val request = Request.Builder().url(current)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", acceptLanguage)
                    .build()
                val response = try { client.newCall(request).await() } catch (e: IOException) {
                    return@withContext FetchResult.NetworkError(e.message ?: e.javaClass.simpleName)
                }
                response.use { resp ->
                    if (resp.isRedirect) {
                        val location = resp.header("Location") ?: return@withContext FetchResult.HttpError(resp.code)
                        current = current.resolve(location) ?: return@withContext FetchResult.Rejected("bad redirect")
                        return@use
                    }
                    val body = try { readLimited(resp, maxBytes).toString(Charsets.UTF_8) } catch (e: TooLargeException) {
                        return@withContext FetchResult.Rejected("too large")
                    } catch (e: IOException) {
                        return@withContext FetchResult.NetworkError(e.message ?: "read error")
                    }
                    BlockDetector.detect(resp.code, body, resp.headers.toMultimap())?.let {
                        return@withContext FetchResult.Blocked(resp.code, it)
                    }
                    if (!resp.isSuccessful) return@withContext FetchResult.HttpError(resp.code)
                    return@withContext FetchResult.Ok(current.toString(), body, resp.code)
                }
            }
            FetchResult.Rejected("too many redirects")
        }

    /** Downloads binary data (photos) with a hard size limit. Returns bytes and content type. */
    suspend fun fetchBytes(url: String, maxBytes: Long, imageHostPolicy: (String) -> Boolean): Pair<ByteArray, String?> =
        withContext(Dispatchers.IO) {
            var current = url.toHttpUrlOrNull() ?: throw IOException("invalid url")
            repeat(MAX_REDIRECTS + 1) {
                if (!current.isHttps && !allowHttp) current = current.newBuilder().scheme("https").build()
                if (!imageHostPolicy(current.host)) throw IOException("image host not allowed: ${current.host}")
                throttle(current.host, minIntervalMs / 3)
                coroutineContext.ensureActive()
                val req = Request.Builder().url(current).header("User-Agent", userAgent)
                    .header("Accept", "image/avif,image/webp,image/jpeg,image/png,image/*;q=0.8").build()
                client.newCall(req).await().use { resp ->
                    if (resp.isRedirect) {
                        current = resp.header("Location")?.let { current.resolve(it) } ?: throw IOException("bad redirect")
                        return@use
                    }
                    if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                    val len = resp.body?.contentLength() ?: -1
                    if (len > maxBytes) throw TooLargeException(maxBytes)
                    return@withContext readLimited(resp, maxBytes) to resp.header("Content-Type")
                }
            }
            throw IOException("too many redirects")
        }

    private suspend fun throttle(host: String, interval: Long = minIntervalMs) {
        val last = lastRequest[host]
        val now = System.currentTimeMillis()
        if (last != null && now - last < interval) delay(interval - (now - last))
        lastRequest[host] = System.currentTimeMillis()
    }

    companion object {
        const val MAX_REDIRECTS = 5
        const val MAX_PAGE_BYTES = 6L * 1024 * 1024
        const val MAX_IMAGE_BYTES = 15L * 1024 * 1024
        /** Honest user agent: CasaCheck identifies itself and does not pretend to be a browser. */
        const val USER_AGENT = "CasaCheck/1.0 (Android; personal home-inspection app)"

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        fun readLimited(resp: Response, maxBytes: Long): ByteArray {
            val body = resp.body ?: return ByteArray(0)
            val out = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buf = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) throw TooLargeException(maxBytes)
                    out.write(buf, 0, n)
                }
            }
            return out.toByteArray()
        }
    }
}

/** Suspends on an OkHttp call and cancels the call when the coroutine is cancelled (import cancel button). */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    cont.invokeOnCancellation { runCatching { cancel() } }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) cont.resume(response) else response.close()
        }
    })
}

/** Recognises anti-bot / CAPTCHA interstitials (Cloudflare challenge, DataDome) and rate limits. */
object BlockDetector {
    private val markers = listOf(
        "captcha-delivery.com" to "DataDome",
        "challenges.cloudflare.com" to "Cloudflare challenge",
        "<title>Just a moment...</title>" to "Cloudflare challenge",
        "cf-chl-" to "Cloudflare challenge",
        "Please enable JS and disable any ad blocker" to "anti-bot",
        "g-recaptcha" to "reCAPTCHA",
        "hcaptcha.com" to "hCaptcha",
    )

    fun detect(code: Int, body: String, headers: Map<String, List<String>> = emptyMap()): String? {
        val head = if (body.length > 20_000) body.substring(0, 20_000) else body
        markers.firstOrNull { head.contains(it.first, ignoreCase = true) }?.let {
            // A normal listing page could theoretically mention reCAPTCHA in a contact form; only treat
            // reCAPTCHA as a block if the page is not a success.
            if (it.second == "reCAPTCHA" && code in 200..299) return null
            return it.second
        }
        if (headers.keys.any { it.equals("x-datadome", true) } && code == 403) return "DataDome"
        if (code == 429) return "rate limited"
        if (code == 403) return "access denied"
        return null
    }
}
