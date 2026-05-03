# TRMNL Device Widget

A Compose Glance home screen widget that shows the current e-ink display image from a user-selected [TRMNL](https://trmnl.com) device.

## Overview

The widget fetches the live display image via the TRMNL API and renders it directly on the Android home screen. It refreshes automatically using the cadence provided by the API (`refresh_rate`), keeping the home screen image in sync with whatever the physical device is currently showing.

## Files

| File | Description |
|------|-------------|
| [`TrmnlDeviceWidget.kt`](TrmnlDeviceWidget.kt) | `GlanceAppWidget` — widget UI with four states |
| [`TrmnlDeviceWidgetReceiver.kt`](TrmnlDeviceWidgetReceiver.kt) | `GlanceAppWidgetReceiver` — system lifecycle (add, remove, reboot) |
| [`TrmnlWidgetRefreshWorker.kt`](TrmnlWidgetRefreshWorker.kt) | `CoroutineWorker` — fetches display image, caches it, schedules next refresh |
| [`RefreshWidgetCallback.kt`](RefreshWidgetCallback.kt) | `ActionCallback` — handles in-widget refresh button tap |
| [`WidgetConfigurationActivity.kt`](WidgetConfigurationActivity.kt) | `ComponentActivity` — device-picker shown when adding the widget |

## Widget States

The widget renders one of four states depending on the current Glance preferences state:

```
┌─────────────────────────────────────────────────────────────────┐
│ deviceFriendlyId == null          → Unconfigured                │
│                                     "Tap to select a device"    │
│                                     Opens WidgetConfigActivity  │
├─────────────────────────────────────────────────────────────────┤
│ deviceFriendlyId set, no image    → Loading                     │
│                                     "Loading <device name>…"    │
├─────────────────────────────────────────────────────────────────┤
│ errorMessage set                  → Error                       │
│                                     Error text + retry button   │
├─────────────────────────────────────────────────────────────────┤
│ bitmap available                  → Content                     │
│                                     Device name + display image │
│                                     + manual refresh button     │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

```
User adds widget
      │
      ▼
WidgetConfigurationActivity
  ├─ Fetches device list via TRMNL API (GET /api/devices)
  └─ User selects a device
        │
        ▼ writes to Glance PreferencesGlanceState:
          • device_friendly_id
          • device_name
          • app_widget_id
        │
        └─► enqueues TrmnlWidgetRefreshWorker (immediate)

TrmnlWidgetRefreshWorker (WorkManager CoroutineWorker)
  1. Reads device_friendly_id from Glance state
  2. Looks up device API token from DeviceTokenRepository
  3. GET /api/display/current  (Access-Token: <device token>)
  4. Downloads PNG → filesDir/widget_images/widget_{appWidgetId}.png
  5. Writes to Glance state:
       • image_file_path
       • refresh_rate
       • last_updated
  6. Calls TrmnlDeviceWidget.update() → widget redraws
  7. Enqueues itself again with delay = max(15 min, refreshRate / 60)
```

## Image Caching

Each widget instance caches its display image locally:

```
Context.filesDir/
  widget_images/
    widget_1.png   ← widget appWidgetId = 1
    widget_2.png   ← widget appWidgetId = 2
```

The file is deleted automatically when the widget is removed from the home screen (`onDeleted`).

## Refresh Scheduling

Refreshes use chained one-time [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) work requests rather than periodic work, so the next delay is always taken directly from the API's `refresh_rate` response field.

| Scenario | `ExistingWorkPolicy` | Reason |
|----------|----------------------|--------|
| System `onUpdate` (reboot, widget restore) | `KEEP` | Don't cancel an in-progress fetch |
| Self-reschedule after successful fetch | `APPEND_OR_REPLACE` | Worker is still RUNNING when it re-enqueues; `KEEP` silently drops the request because RUNNING counts as "work present", breaking periodic refresh. `APPEND_OR_REPLACE` chains the new delayed job after the current run. |
| User taps refresh button | `REPLACE` | Force an immediate restart |

The minimum refresh interval is **15 minutes** (`MIN_REFRESH_INTERVAL_MINUTES`) regardless of the API-returned rate.

## Authentication

The widget uses a **device-specific token** (`Access-Token` header) obtained from `DeviceTokenRepository`, separate from the user's account bearer token. The token is associated with the device's `friendly_id` chosen during configuration.

## Widget Metadata

- Default size: 4 × 3 cells
- Resizable: horizontal and vertical
- Provider XML: `res/xml/trmnl_device_widget_info.xml`
- Initial placeholder layout: `res/layout/trmnl_widget_initial_layout.xml`
- Configuration activity registered in `AndroidManifest.xml`
