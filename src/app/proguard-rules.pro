# Keep MNN classes
-keep class com.alibaba.mnnllm.** { *; }
-keep class com.alibaba.mnn.** { *; }

# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keep class com.tiggy.gemma4mnn.data.db.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.util.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <fields>;
}

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.tiggy.gemma4mnn.** {
    *** Companion;
}

# Keep Markwon
-keep class io.noties.markwon.** { *; }
-keep class org.commonmark.** { *; }
