# ==============================================================================
# 📖 BookSpread — ProGuard / R8 Rules for Release Optimization
# ==============================================================================

# General Android & Kotlin attributes
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**

# ------------------------------------------------------------------------------
# 1. KotlinX Serialization
# ------------------------------------------------------------------------------
-keepattributes *Annotation*
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    *** Companion;
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

-keep @kotlinx.serialization.Serializable class * { *; }

# ------------------------------------------------------------------------------
# 2. Room Database & SQLite
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * implements androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 3. BookSpread Data Models & Entities
# ------------------------------------------------------------------------------
-keep class com.example.calibretv.data.model.** { *; }
-keep class com.example.calibretv.data.storage.** { *; }
-keep class com.example.calibretv.data.epub.** { *; }
-keep class com.example.calibretv.data.comic.** { *; }
-keep class com.example.calibretv.data.update.** { *; }
-keep class com.example.calibretv.data.server.** { *; }
-keep class com.example.calibretv.data.provider.** { *; }

# ------------------------------------------------------------------------------
# 4. AndroidX Security Crypto (EncryptedSharedPreferences)
# ------------------------------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keepclassmembers class androidx.security.crypto.** { *; }

# ------------------------------------------------------------------------------
# 5. Jetpack Compose & Navigation
# ------------------------------------------------------------------------------
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.navigation.** { *; }
-keep class androidx.navigation3.** { *; }

# ------------------------------------------------------------------------------
# 6. Coroutines
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.** { *; }
