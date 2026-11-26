package nl.ecannemeijer.casacheck.data.transcribe

/** Callback from native code during whisper_full (called on the calling thread). */
interface WhisperListener {
    fun onProgress(percent: Int)
    fun isCancelled(): Boolean
}

/** Thin JNI wrapper around whisper.cpp (libcasawhisper.so, built from app/src/main/cpp). */
object WhisperLib {
    @Volatile private var loaded = false

    fun load(): Boolean {
        if (loaded) return true
        return runCatching { System.loadLibrary("casawhisper"); loaded = true }.isSuccess
    }

    @JvmStatic external fun initContext(modelPath: String): Long
    @JvmStatic external fun freeContext(ctx: Long)
    @JvmStatic external fun fullTranscribe(ctx: Long, threads: Int, language: String, audio: FloatArray, listener: WhisperListener?): Int
    @JvmStatic external fun segmentCount(ctx: Long): Int
    @JvmStatic external fun segmentText(ctx: Long, i: Int): ByteArray
    @JvmStatic external fun segmentT0(ctx: Long, i: Int): Long
    @JvmStatic external fun segmentT1(ctx: Long, i: Int): Long
    @JvmStatic external fun detectedLanguage(ctx: Long): String
    @JvmStatic external fun systemInfo(): String
}