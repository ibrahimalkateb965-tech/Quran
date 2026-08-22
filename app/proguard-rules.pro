# ProGuard & R8 Optimization Rules for Quran Blind App

# 1. General Android & Line Numbers preservation for Crash Reports
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*
-renamesourcefileattribute SourceFile

# 2. Kotlin Coroutines & Flow
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# 3. AndroidX Media3 (ExoPlayer & MediaSession)
-keep class androidx.media3.** { *; }
-keepclassmembers class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.example.service.QuranAudioService { *; }

# 4. Room Database & SQLite
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# 5. Moshi & Retrofit Serialization
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# 6. Models & Data Transfer Objects (Keep all data classes)
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }

# 7. AndroidX Security Crypto & SharedPreferences
-keep class androidx.security.crypto.** { *; }

# 8. Compose UI & Runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# 9. Security & Licensing
-keep class com.example.security.** { *; }
-keepclassmembers class com.example.security.** { *; }

