# kotlinx.serialization (backup format)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class nl.ecannemeijer.casacheck.** {
    *** Companion;
}
-keepclasseswithmembers class nl.ecannemeijer.casacheck.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class nl.ecannemeijer.casacheck.**$$serializer { *; }
# Jsoup optional re2j
-dontwarn com.google.re2j.**
# OkHttp platform classes
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# whisper.cpp JNI bridge: native method names and the callback interface used from C
-keep class nl.ecannemeijer.casacheck.data.transcribe.WhisperLib { *; }
-keep interface nl.ecannemeijer.casacheck.data.transcribe.WhisperListener { *; }
-keepclassmembers class * implements nl.ecannemeijer.casacheck.data.transcribe.WhisperListener { *; }

