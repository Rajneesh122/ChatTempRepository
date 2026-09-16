-keepattributes *Annotation*

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.rws.learningproject01.core.model.**$$serializer { *; }
-keepclassmembers class com.rws.learningproject01.core.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
