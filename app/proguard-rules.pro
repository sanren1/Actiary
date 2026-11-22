# Keep Serializable domain models used in SavedStateHandle / Bundle
-keep class com.actiary.domain.WorkoutMetrics { *; }
-keep class com.actiary.domain.WorkoutScreenshot { *; }
-keep class com.actiary.domain.ParsedWorkout { *; }
-keep class com.actiary.domain.DayDraft { *; }
-keep class com.actiary.domain.DraftEntry { *; }
-keep class com.actiary.domain.DraftPhoto { *; }
-keep class com.actiary.domain.DraftPhoto$* { *; }

# Keep AppWidgetProvider entry point referenced from AndroidManifest
-keep class com.actiary.widget.TodayWidgetProvider { *; }
