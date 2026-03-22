# calendar-sync Consumer ProGuard Rules

# Keep all calendar sync classes
-keep class ink.trmnl.android.buddy.calendar.** { *; }

# Keep data classes for serialization
-keepclassmembers class ink.trmnl.android.buddy.calendar.models.** {
    <fields>;
    <methods>;
}
