// JNI bridge between Kotlin (nl.ecannemeijer.casacheck.data.transcribe.WhisperLib) and whisper.cpp.
#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>
#include "whisper.h"

#define TAG "CasaWhisper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

struct callback_ctx {
    JNIEnv *env;
    jobject listener;
    jmethodID on_progress;
    jmethodID is_cancelled;
};

static void progress_cb(struct whisper_context *ctx, struct whisper_state *state, int progress, void *user_data) {
    (void) ctx; (void) state;
    struct callback_ctx *cb = (struct callback_ctx *) user_data;
    if (cb && cb->listener) (*cb->env)->CallVoidMethod(cb->env, cb->listener, cb->on_progress, progress);
}

static bool abort_cb(void *user_data) {
    struct callback_ctx *cb = (struct callback_ctx *) user_data;
    if (!cb || !cb->listener) return false;
    return (*cb->env)->CallBooleanMethod(cb->env, cb->listener, cb->is_cancelled) == JNI_TRUE;
}

static void quiet_log(enum ggml_log_level level, const char *text, void *user_data) {
    (void) user_data;
    if (level >= GGML_LOG_LEVEL_WARN) __android_log_write(ANDROID_LOG_WARN, TAG, text);
}

JNIEXPORT jlong JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_initContext(JNIEnv *env, jclass clazz, jstring model_path) {
    (void) clazz;
    whisper_log_set(quiet_log, NULL);
    const char *path = (*env)->GetStringUTFChars(env, model_path, NULL);
    struct whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    (*env)->ReleaseStringUTFChars(env, model_path, path);
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_freeContext(JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    (void) env; (void) clazz;
    if (ctx_ptr) whisper_free((struct whisper_context *) ctx_ptr);
}

JNIEXPORT jint JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_fullTranscribe(JNIEnv *env, jclass clazz, jlong ctx_ptr, jint n_threads,
                                                                       jstring language, jfloatArray audio, jobject listener) {
    (void) clazz;
    struct whisper_context *ctx = (struct whisper_context *) ctx_ptr;
    if (!ctx) return -100;
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.n_threads = n_threads;
    params.no_context = true;
    params.single_segment = false;
    params.suppress_blank = true;

    const char *lang = (*env)->GetStringUTFChars(env, language, NULL);
    params.language = lang; // "auto" = detect

    struct callback_ctx cb = {0};
    if (listener) {
        jclass lc = (*env)->GetObjectClass(env, listener);
        cb.env = env;
        cb.listener = listener;
        cb.on_progress = (*env)->GetMethodID(env, lc, "onProgress", "(I)V");
        cb.is_cancelled = (*env)->GetMethodID(env, lc, "isCancelled", "()Z");
        params.progress_callback = progress_cb;
        params.progress_callback_user_data = &cb;
        params.abort_callback = abort_cb;
        params.abort_callback_user_data = &cb;
    }

    jfloat *samples = (*env)->GetFloatArrayElements(env, audio, NULL);
    jsize n = (*env)->GetArrayLength(env, audio);
    int rc = whisper_full(ctx, params, samples, n);
    (*env)->ReleaseFloatArrayElements(env, audio, samples, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, language, lang);
    return rc;
}

JNIEXPORT jint JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_segmentCount(JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    (void) env; (void) clazz;
    return whisper_full_n_segments((struct whisper_context *) ctx_ptr);
}

/** Returns raw UTF-8 bytes: whisper may cut multi-byte characters, which NewStringUTF would reject. */
JNIEXPORT jbyteArray JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_segmentText(JNIEnv *env, jclass clazz, jlong ctx_ptr, jint i) {
    (void) clazz;
    const char *text = whisper_full_get_segment_text((struct whisper_context *) ctx_ptr, i);
    jsize len = text ? (jsize) strlen(text) : 0;
    jbyteArray out = (*env)->NewByteArray(env, len);
    if (len > 0) (*env)->SetByteArrayRegion(env, out, 0, len, (const jbyte *) text);
    return out;
}

JNIEXPORT jlong JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_segmentT0(JNIEnv *env, jclass clazz, jlong ctx_ptr, jint i) {
    (void) env; (void) clazz;
    return whisper_full_get_segment_t0((struct whisper_context *) ctx_ptr, i);
}

JNIEXPORT jlong JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_segmentT1(JNIEnv *env, jclass clazz, jlong ctx_ptr, jint i) {
    (void) env; (void) clazz;
    return whisper_full_get_segment_t1((struct whisper_context *) ctx_ptr, i);
}

JNIEXPORT jstring JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_detectedLanguage(JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    (void) clazz;
    int id = whisper_full_lang_id((struct whisper_context *) ctx_ptr);
    const char *s = id >= 0 ? whisper_lang_str(id) : NULL;
    return (*env)->NewStringUTF(env, s ? s : "");
}

JNIEXPORT jstring JNICALL
Java_nl_ecannemeijer_casacheck_data_transcribe_WhisperLib_systemInfo(JNIEnv *env, jclass clazz) {
    (void) clazz;
    return (*env)->NewStringUTF(env, whisper_print_system_info());
}