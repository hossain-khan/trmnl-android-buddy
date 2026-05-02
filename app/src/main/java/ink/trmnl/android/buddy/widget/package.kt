/**
 * # TRMNL Device Widget
 *
 * Compose Glance-based home screen widget that displays the current e-ink display
 * image from a user-selected [TRMNL](https://trmnl.com) device.
 *
 * ## Package contents
 *
 * | Class | Role |
 * |---|---|
 * | [TrmnlDeviceWidget] | `GlanceAppWidget` — renders the widget UI in four states |
 * | [TrmnlDeviceWidgetReceiver] | `GlanceAppWidgetReceiver` — handles system lifecycle (add / remove / reboot) |
 * | [TrmnlWidgetRefreshWorker] | `CoroutineWorker` — fetches the display image and schedules the next refresh |
 * | [RefreshWidgetCallback] | `ActionCallback` — handles the in-widget refresh button tap |
 * | [WidgetConfigurationActivity] | `ComponentActivity` — device-picker shown when adding the widget |
 *
 * ## Widget states
 *
 * ```
 * ┌─────────────────────────────────────────┐
 * │  deviceFriendlyId == null               │  → Unconfigured  (tap to open config)
 * │  deviceFriendlyId set, no image yet     │  → Loading       (spinner text)
 * │  errorMessage set                       │  → Error         (message + retry button)
 * │  bitmap available                       │  → Content       (display image + refresh)
 * └─────────────────────────────────────────┘
 * ```
 *
 * ## Data flow
 *
 * ```
 * WidgetConfigurationActivity
 *   │  user picks a device
 *   ▼
 * Glance PreferencesGlanceState
 *   (device_friendly_id, device_name, app_widget_id)
 *   │
 *   ▼
 * TrmnlWidgetRefreshWorker  ──► TRMNL API  GET /api/display/current
 *   │  downloads PNG → filesDir/widget_images/widget_{id}.png
 *   │  writes image_file_path, refresh_rate, last_updated into state
 *   ▼
 * TrmnlDeviceWidget.update()  ──►  widget redraws with new bitmap
 *   │
 *   └─► schedules next TrmnlWidgetRefreshWorker (≥ 15 min)
 * ```
 *
 * ## Image caching
 *
 * Each widget instance stores its display image at:
 * `Context.filesDir / widget_images / widget_{appWidgetId}.png`
 *
 * The file is deleted when the widget is removed (`onDeleted`).
 *
 * ## Refresh scheduling
 *
 * Refreshes are driven by one-time [androidx.work.WorkManager] work requests chained
 * to themselves.  The delay for each subsequent request is taken from the
 * `refresh_rate` field returned by the API (in seconds), floored at
 * [TrmnlDeviceWidget.MIN_REFRESH_INTERVAL_MINUTES] (15 min) to avoid excessive polling.
 *
 * System-initiated `onUpdate` calls (e.g. after reboot) use
 * [androidx.work.ExistingWorkPolicy.KEEP] so an in-progress worker is never
 * cancelled; user-triggered refreshes (tap on refresh icon) use
 * [androidx.work.ExistingWorkPolicy.REPLACE] to force an immediate restart.
 */
package ink.trmnl.android.buddy.widget
