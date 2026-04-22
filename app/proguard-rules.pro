# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.gitlinked.models.** { *; }

# Gson
-keep class com.google.gson.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
