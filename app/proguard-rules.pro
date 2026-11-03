# ProGuard / R8 configuration for MeloGroove (Fixes N-10)
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Keep Moshi & Retrofit DTOs
-keep class com.example.data.api.** { *; }
-keep class com.example.data.model.** { *; }
-keep class com.squareup.moshi.** { *; }

# Keep Media3 session service
-keep class com.example.playback.MediaPlaybackService { *; }
