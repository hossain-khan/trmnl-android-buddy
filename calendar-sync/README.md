# Calendar Sync Module

This module provides calendar synchronization capability for TRMNL devices, enabling users to display their calendar events on their TRMNL e-ink displays.

## Overview

The `calendar-sync` module is the Android equivalent of the [TRMNL Companion iOS app](https://help.trmnl.com/en/articles/12294875-trmnl-companion-for-ios-calendar-sync). It allows users to:

- **Select which calendars to sync** from their device (Google, Outlook, Exchange, CalDAV, local calendars)
- **Automatically sync events** to TRMNL calendar plugins
- **Disconnect at any time** if they no longer want calendar syncing
- **Read-only access** - no calendar modifications

## Permissions

This module uses **READ-ONLY** access:
```xml
<uses-permission android:name="android.permission.READ_CALENDAR" />
```

❌ Does NOT request `WRITE_CALENDAR` permission.

## Architecture

### Data Flow

```
Device Calendar Provider
    ↓
CalendarProvider (read from Android Calendar API)
    ↓
CalendarSyncRepository (manage preferences & filtering)
    ↓
CalendarSyncWorker (periodic background sync)
    ↓
TrmnlApiService.syncCalendarEvents() [in api module]
    ↓
TRMNL Server (calendar plugin receives events)
```

### Components

#### `CalendarProvider`
- Directly queries Android Calendar Provider
- Returns calendars and events in system timezone
- Handles permission errors gracefully
- Supports time-window filtering (default: 7 days past + 30 days future, matching iOS)

#### `CalendarSyncRepository`
- Public API for calendar sync operations
- Manages user preferences (selected calendars, sync enabled/disabled)
- Handles SharedPreferences for persistent state
- Tracks sync status and errors

#### `SyncCalendar` & `SyncEvent`
Data models that map Android Calendar Provider fields to TRMNL API format:

```kotlin
SyncEvent {
    start: "16:00",                                    // HH:mm format
    end: "18:00",                                      // HH:mm format
    start_full: "2025-10-02T16:00:00.000Z",           // ISO 8601
    end_full: "2025-10-02T18:00:00.000Z",             // ISO 8601
    date_time: "2025-10-02T16:00:00.000Z",            // ISO 8601
    summary: "Support chats",                         // Event title
    description: "Respond to customers...",           // Event description
    all_day: false,                                   // Boolean
    status: "accepted",                               // Attendance status
    calname: "user@gmail.com"                         // Calendar identifier
}
```

#### `CalendarSyncWorker`
- WorkManager background task for periodic syncing
- Scheduled externally (managed by `WorkerScheduler` in app module)
- Implements exponential backoff (retry up to 2 times)
- Records sync status and errors

## Usage

### Basic Integration

1. **Add Metro DI binding**:
```kotlin
// In DI setup
@ContributesIntoSet
fun provideCalendarSyncModule(): ModuleBinding {
    return binding<CalendarSyncModule>()
}
```

2. **Inject repository into screens/presenters**:
```kotlin
class MyPresenter @Inject constructor(
    private val calendarSyncRepository: CalendarSyncRepository,
) {
    suspend fun getAvailableCalendars(): List<SyncCalendar> {
        return calendarSyncRepository.getAvailableCalendars()
    }
}
```

3. **Handle user calendar selection**:
```kotlin
// User selects calendars to sync
val selectedIds = listOf(123L, 456L)
calendarSyncRepository.updateCalendarSelection(selectedIds)

// Enable sync
calendarSyncRepository.setSyncEnabled(true)
```

4. **Get events for API sync**:
```kotlin
val events = calendarSyncRepository.getEventsForSync()
// Send to TRMNL API via api module
```

### Disconnecting

To disable calendar sync completely:

```kotlin
calendarSyncRepository.disconnect()
```

This will:
- Set sync enabled = false
- Clear selected calendars list
- Keep historical sync data (last sync time, errors) for recovery

## Time Window

Events are synced within a **7-day past to 30-day future window** (matching iOS Companion app):

```
7 days ago ← [SYNC WINDOW] → 30 days from now
```

This is configurable in `CalendarSyncRepository.getEventsForSync()`:

```kotlin
suspend fun getEventsForSync(
    startTime: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
    endTime: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000),
): List<SyncEvent>
```

## Calendar Types Supported

Through Android's Calendar Provider, this module supports:
- ✅ Google Calendar
- ✅ Outlook / Exchange
- ✅ FastMail
- ✅ Nextcloud
- ✅ CalDAV
- ✅ Local calendars (not synced to server)

User can mix and match (e.g., personal Google Calendar + work Outlook calendar).

## Testing

### Unit Tests

```bash
./gradlew :calendar-sync:test
```

Key test coverage:
- Calendar query with permissions denied
- Event parsing and timezone handling
- Preference persistence
- Sync status tracking
- Time window filtering

### Integration Testing

```bash
./gradlew :calendar-sync:connectedAndroidTest
```

## Security & Privacy

- **READ-ONLY**: No calendar data is modified
- **Time window**: Only events from 7 days past to 30 days future are synced
- **User control**: Users explicitly select which calendars to sync
- **Disconnection**: Users can disconnect at any time
- **Local storage**: SharedPreferences used for preferences only (no event caching)

## Future Enhancements

- [ ] Event filtering (exclude busy time, specific calendars)
- [ ] Custom sync frequency configuration
- [ ] Recurring event expansion options
- [ ] Multi-device sync coordination
- [ ] Sync conflict resolution (if user modifies calendar manually)

## Related Documentation

- [Android Calendar Provider API](https://developer.android.com/guide/topics/providers/calendar-provider)
- [TRMNL Companion iOS Calendar Sync](https://help.trmnl.com/en/articles/12294875-trmnl-companion-for-ios-calendar-sync)
- [TRMNL Calendar Sync Implementation Plan](../docs/CALENDAR_SYNC_IMPLEMENTATION_PLAN.md)

## TRMNL API Integration

This module integrates with the TRMNL API to send calendar events to TRMNL displays.

### API Workflow (3-Step Process)

The sync process follows the TRMNL Companion app pattern:

#### 1. Validate API Key
```http
GET https://usetrmnl.com/api/me
Authorization: Bearer {api_key}
```

#### 2. Get Plugin Settings
```http
GET https://usetrmnl.com/api/plugin_settings?plugin_id=calendars
Authorization: Bearer {api_key}
```
Response includes plugin setting ID required for next step.

#### 3. Sync Events
```http
POST https://usetrmnl.com/api/plugin_settings/{setting_id}/data
Authorization: Bearer {api_key}
Content-Type: application/json

{
  "merge_variables": {
    "events": [
      {
        "summary": "Team Meeting",
        "start": "14:30",
        "start_full": "2025-08-24T14:30:00.000-04:00",
        "date_time": "2025-08-24T14:30:00.000-04:00",
        "end": "15:30",
        "end_full": "2025-08-24T15:30:00.000-04:00",
        "all_day": false,
        "description": "Discuss Q3 roadmap",
        "status": "confirmed",
        "calendar_identifier": "user@gmail.com"
      }
    ]
  }
}
```

### Event Transformation

Events are transformed from Android Calendar Provider format to TRMNL API format:

| Android Provider | TRMNL API | Example |
|------------------|-----------|---------|
| Event title | `summary` | "Team Meeting" |
| Start time | `start` + `start_full` | "14:30" + "2025-08-24T14:30:00.000-04:00" |
| End time | `end` + `end_full` | "15:30" + "2025-08-24T15:30:00.000-04:00" |
| Description | `description` | "Discuss Q3 roadmap" |
| Status | `status` | "confirmed" or "tentative" |
| Calendar ID | `calendar_identifier` | "user@gmail.com" or calendar unique ID |
| All-day flag | `all_day` | true/false |

### Date/Time Formatting

- **ISO8601 with timezone**: `2025-08-24T14:30:00.000-04:00`
  - Use device timezone
  - Include milliseconds
  - Include timezone offset

- **Time-only (HH:mm)**: `14:30`
  - 24-hour format
  - No timezone info

### Sync Characteristics

- **Full Sync** (not delta): Send complete event list for entire window every sync
- **Time Window**: 6 days in past + 30 days in future
- **Event Merging**: Multiple calendars merged into single request per plugin
- **Server Deduplication**: Server deduplicates based on calendar_identifier + start_full + summary

## Notes

- This module does NOT include UI screens - UI is managed by the `app` module
- API integration (sending events to TRMNL server) is handled by the `api` module
- Background scheduling is managed by the `app` module's WorkerScheduler
