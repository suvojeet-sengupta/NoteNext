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

# Keep data classes for Gson serialization
-keep class com.suvojeet.notenext.data.** { *; }
