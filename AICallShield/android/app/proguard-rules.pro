# ProGuard rules for AICallShield

# Keep Retrofit models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.aicallshield.data.model.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Firebase
-keep class com.google.firebase.** { *; }

# Compose
-dontwarn androidx.compose.**
