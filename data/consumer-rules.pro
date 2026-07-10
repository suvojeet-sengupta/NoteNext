# Room
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# Kotlin Serialization
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

# Keep the data classes and their members
-keep class com.suvojeet.notenext.data.** { *; }
