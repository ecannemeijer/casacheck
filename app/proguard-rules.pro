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
