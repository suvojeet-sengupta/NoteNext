# Add project specific ProGuard rules here.
# You can find more information about ProGuard in the official documentation:
# https://www.guardsquare.com/en/products/proguard/manual/introduction

# Google API Client & Drive API
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.gson.** { *; }
-keep class * extends com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.client.util.** { *; }

-dontwarn com.google.api.client.extensions.android.**
-dontwarn com.google.api.client.googleapis.extensions.android.**

# Apache HttpClient & Missing Java Classes on Android
-dontwarn javax.naming.**
-dontwarn javax.net.ssl.**
-dontwarn org.ietf.jgss.**
-dontwarn org.apache.http.**
-dontwarn org.apache.commons.**

# Keep data classes for Gson and Kotlin Serialization
-keep class com.suvojeet.notenext.data.** { *; }
-keep class com.suvojeet.notenext.data.remote.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class kotlinx.serialization.** { *; }
-keep class **$$serializer { *; }
-keepclassmembers class * {
    public static ** Companion;
    public static ** serializer(...);
}
-dontwarn kotlinx.serialization.internal.**

# Suppress R8/Kotlin metadata warnings (Kotlin 2.3.0 vs AGP 8.13.0)
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.Metadata
-ignorewarnings

# Task 1: Obfuscate NetworkModule and AiRepository
-keep class com.suvojeet.notenext.BuildConfig { *; }
-keepclassmembers class com.suvojeet.notenext.di.NetworkModule { *; }
-keepclassmembers class com.suvojeet.notenext.data.repository.AiRepository { *; }
# Allow full obfuscation of internal methods and fields
-repackageclasses ''
-allowaccessmodification

# Google Play Core (App Update & Review)
-keep class com.google.android.play.core.appupdate.** { *; }
-keep class com.google.android.play.core.install.** { *; }
-keep class com.google.android.play.core.review.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }
-keep class com.google.android.play.core.common.** { *; }

# Also keep the specific status constants
-keep class com.google.android.play.core.install.model.** { *; }
-keep class com.google.android.play.core.appupdate.model.** { *; }
