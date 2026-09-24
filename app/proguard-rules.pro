# Keep Room Database entities and schemas
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep ML Kit Barcode
-keep class com.google.mlkit.** { *; }

# Keep Model classes for serialization
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}
