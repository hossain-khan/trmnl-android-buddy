# Calendar Sync Feature Implementation Plan

**Version**: 1.0  
**Date**: March 22, 2026  
**Status**: Phase 1 Complete (Core Module Foundation)  
**Module**: `calendar-sync`

## Executive Summary

This document outlines the implementation plan for adding **Calendar Sync** capability to TRMNL Android Buddy. This feature allows users to sync their device calendars (Google, Outlook, Exchange, CalDAV, etc.) to their TRMNL e-ink displays via a dedicated calendar plugin.

The implementation mirrors the [TRMNL Companion iOS app](https://help.trmnl.com/en/articles/12294875-trmnl-companion-for-ios-calendar-sync) but uses Android's native Calendar Provider API for secure, read-only access.

---

## Feature Overview

### What Users Can Do

1. **View available calendars** - See all calendars on their Android device (Google, Outlook, local, etc.)
2. **Select calendars to sync** - Choose which calendars should appear on their TRMNL display
3. **Automatic sync** - Events sync periodically in the background (configurable frequency)
4. **Mixed calendar sources** - Combine calendars from different providers (e.g., personal Google + work Outlook)
5. **Disconnect anytime** - Stop syncing and clear selection with one action
6. **View sync status** - See last sync time and any error messages

### Key Constraints

- ✅ **READ-ONLY** - Uses only `READ_CALENDAR` permission, never modifies calendars
- ✅ **Time window** - Syncs events from 7 days past to 30 days future (like iOS)
- ✅ **User control** - Users explicitly select which calendars to sync
- ✅ **Privacy** - Minimal data sent to TRMNL: event title, time, description, calendar name

---

## Architecture Design

### High-Level Data Flow

```
User Device Calendar Provider
        ↓
   CalendarProvider (read via Android Calendar Provider API)
    ↓        ↓         ↓
 Calendars Events  Timezone info
    ↓        ↓         ↓
   CalendarSyncRepository (manage preferences & filtering)
    ↓
   CalendarSyncWorker (periodic background task via WorkManager)
    ↓
   TrmnlApiService.syncCalendarEvents() [in api module]
    ↓
   TRMNL Server (calendar plugin receives events)
```

### Module Structure

```
calendar-sync/                           # New gradle module
├── src/main/
│   ├── java/ink/trmnl/android/buddy/calendar/
│   │   ├── CalendarProvider.kt          # Direct Calendar Provider API access
│   │   ├── repository/
│   │   │   └── CalendarSyncRepository.kt # Public repository API
│   │   ├── models/
│   │   │   └── SyncCalendar.kt          # Data classes for calendars & events
│   │   ├── workers/
│   │   │   └── CalendarSyncWorker.kt    # WorkManager periodic sync task
│   │   └── di/
│   │       └── CalendarSyncModule.kt    # Metro DI bindings
│   └── AndroidManifest.xml              # READ_CALENDAR permission only
├── src/test/
│   └── java/ink/trmnl/android/buddy/calendar/
│       ├── CalendarProviderTest.kt
│       └── CalendarSyncRepositoryTest.kt
├── build.gradle.kts                     # Module dependencies
├── README.md                            # Detailed module documentation
├── proguard-rules.pro                   # ProGuard configuration
└── consumer-rules.pro                   # Consumer ProGuard rules
```

### Component Responsibilities

| Component | Responsibility |
|-----------|-----------------|
| **CalendarProvider** | Direct read access to Android Calendar Provider. Queries calendars & events with proper handling of timezones, recurring events, and permission errors. |
| **CalendarSyncRepository** | Public API for calendar sync. Manages SharedPreferences for user selections. Provides sync status tracking. |
| **SyncCalendar & SyncEvent** | Data models mapping Android Calendar Provider → TRMNL API format. Includes all required fields (ISO 8601 timestamps, event details, calendar identifier). |
| **CalendarSyncWorker** | WorkManager task for periodic sync. Checks if sync enabled, fetches events, triggers API call. Implements retry logic. |
| **CalendarSyncModule** | Metro DI setup for dependency injection. |

---

## Data Format Mapping

### Input (Android Calendar Provider)

```
CalendarContract.Calendars:
  - _ID: Long
  - CALENDAR_DISPLAY_NAME: String
  - ACCOUNT_NAME: String (e.g., "user@gmail.com")
  - ACCOUNT_TYPE: String (e.g., "com.google")
  - OWNER_ACCOUNT: String

CalendarContract.Events:
  - _ID: Long
  - TITLE: String
  - DESCRIPTION: String
  - DTSTART: Long (milliseconds since epoch)
  - DTEND: Long
  - ALL_DAY: Int (0/1)
  - EVENT_TIMEZONE: String
```

### Output (TRMNL API Format)

```json
{
  "start": "16:00",
  "end": "18:00",
  "start_full": "2025-10-02T16:00:00.000Z",
  "end_full": "2025-10-02T18:00:00.000Z",
  "date_time": "2025-10-02T16:00:00.000Z",
  "summary": "Support chats",
  "description": "Respond to customers from my walking standup desk",
  "all_day": false,
  "status": "accepted",
  "calname": "user@gmail.com"
}
```

---

## Implementation Phases

### Phase 1: Core Module Foundation ✅ COMPLETE

**Status**: Delivered March 22, 2026

**Deliverables**:
- ✅ `calendar-sync` gradle module structure
- ✅ `CalendarProvider` - READ-only Calendar Provider wrapper
- ✅ `CalendarSyncRepository` - Public API with preference management
- ✅ `SyncCalendar` & `SyncEvent` data models
- ✅ `CalendarSyncWorker` - WorkManager background task skeleton
- ✅ Metro DI module (`CalendarSyncModule`)
- ✅ AndroidManifest with READ_CALENDAR permission
- ✅ Comprehensive module README

**Key Features**:
- Query all calendars from device (Google, Outlook, Exchange, CalDAV, local)
- Parse Calendar Provider data into TRMNL format
- Manage user calendar selection via SharedPreferences
- Track sync status (last sync time, errors)
- Support time-window filtering (7 days past + 30 days future)
- Proper timezone handling (ISO 8601 formatting)

### Phase 2: Calendar Management UI (TODO)

**Planned Components**:

#### 2.1 Calendar Selection Screen
- Show list of available calendars with:
  - Calendar name and icon
  - Account email/type
  - Calendar color indicator
  - Toggle checkbox for selection
- "Sync Now" button for immediate sync
- Sync status indicator (last sync time, current status, errors)
- Refresh button to reload calendar list

#### 2.2 Integration Points
- Add to Settings screen → "Calendar Sync" subsection
- Navigation via Circuit router
- State management via Circuit presenters

#### 2.3 User Flows
1. **Initial Setup**:
   - User opens Settings
   - Taps "Calendar Sync"
   - Reviews available calendars
   - Selects calendars to sync
   - Taps "Enable Sync"
   - System shows "Sync enabled" confirmation

2. **Disable Sync**:
   - User opens Calendar Sync settings
   - Taps "Disconnect Sync"
   - Shows confirmation dialog
   - Clears selection and disables sync

3. **Sync Now**:
   - User taps "Sync Now" button
   - Shows loading spinner
   - On success: displays "Last synced: just now"
   - On error: displays error message with "Retry" button

### Phase 3: TRMNL API Integration (TODO)

**Planned Additions to `api` module**:

#### 3.1 New API Endpoint
```kotlin
@POST("plugins/calendar/sync")
suspend fun syncCalendarEvents(
    @Header("Authorization") authorization: String,
    @Body request: CalendarSyncRequest,
): ApiResult<CalendarSyncResponse, ApiError>
```

#### 3.2 Request/Response Models
```kotlin
data class CalendarSyncRequest(
    val events: List<SyncEvent>,
    val calendarIds: List<String>? = null,
)

data class CalendarSyncResponse(
    val status: String,
    val eventsSynced: Int? = null,
    val error: String? = null,
)
```

#### 3.3 Repository Enhancement
```kotlin
// In app module's data repositories
suspend fun syncSelectedCalendars(): Result<CalendarSyncResponse> {
    val events = calendarSyncRepository.getEventsForSync()
    val request = CalendarSyncRequest(events = events)
    
    return when (val result = apiService.syncCalendarEvents(authHeader, request)) {
        is ApiResult.Success -> Result.success(result.value)
        is ApiResult.Failure -> Result.failure(...)
    }
}
```

### Phase 4: Background Sync Scheduling (TODO)

**Planned Components**:

#### 4.1 WorkManager Configuration
- Add to `WorkerScheduler` class
- Periodic sync job (configurable: hourly, daily, etc.)
- Network constraint (only sync when online via WorkRequest)
- Battery optimization (flexible scheduling)

#### 4.2 Sync Constraints
- Only run when WiFi or mobile available
- Batch with other sync tasks if possible
- Exponential backoff (retry up to 2 times on failure)

### Phase 5: Unit & Integration Tests (TODO)

**Test Coverage**:

#### 5.1 CalendarProvider Tests
- Query calendars with/without permissions
- Parse events and handle timezones
- Handle recurring events
- Empty result sets
- Permission denied gracefully

#### 5.2 CalendarSyncRepository Tests
- Load/save calendar selection
- Preferences persistence
- Sync status tracking
- Disconnect functionality
- Time window filtering

#### 5.3 End-to-End Flow Tests
- Select calendars → enable sync → verify sync triggered
- Sync failure → retry logic → recovery
- Disable sync → verify no further syncs

### Phase 6: Documentation & Polish (TODO)

**Deliverables**:
- Update main README with calendar sync feature
- Add user guide for calendar sync setup
- Update CHANGELOG with version notes
- Ensure all code is documented with KDoc comments
- ProGuard configuration validation

---

## Security & Privacy Considerations

### Permissions
- ✅ **READ-ONLY**: Only `android.permission.READ_CALENDAR`
- ✅ **Never requests WRITE_CALENDAR**
- ✅ **Runtime permissions**: Handled gracefully, errors logged

### Data Minimization
- Only syncs: title, description, time, calendar identifier
- Does NOT sync: attendees, reminders, locations (unless in description)
- Time-window: Only 7 days past to 30 days future
- User control: Explicit selection of which calendars to sync

### Local Storage
- **SharedPreferences** only for user preferences (selected calendars, sync state)
- **No event caching** - events fetched fresh on each sync
- **No credential storage** - uses system calendar authentication

### TRMNL Server
- Events sent via HTTPS to TRMNL API
- User account authentication required (existing Bearer token)
- No calendar data exposed to third parties

---

## Timeline & Milestones

| Phase | Milestone | Timeline | Status |
|-------|-----------|----------|--------|
| 1 | Core module foundation | Week 1 | ✅ Complete |
| 2 | Calendar selection UI | Week 2-3 | ⏳ Planned |
| 3 | TRMNL API integration | Week 3 | ⏳ Planned |
| 4 | Background sync scheduling | Week 4 | ⏳ Planned |
| 5 | Testing & QA | Week 4-5 | ⏳ Planned |
| 6 | Documentation & polish | Week 5 | ⏳ Planned |
| - | **Release Ready** | **Week 6** | ⏳ Planned |

---

## Testing Strategy

### Unit Tests
```bash
./gradlew calendar-sync:test
```
- CalendarProvider query logic
- Event parsing and formatting
- Timezone conversions
- Preference persistence
- Error handling

### Integration Tests
```bash
./gradlew calendar-sync:connectedAndroidTest
```
- Permission handling
- Sync workflow end-to-end
- UI state management
- API integration

### Manual Testing Checklist
- [ ] Add calendars from different providers (Google, Outlook, etc.)
- [ ] Enable/disable sync
- [ ] Verify correct events in time window
- [ ] Test sync on WiFi and mobile data
- [ ] Verify offline behavior (graceful degradation)
- [ ] Check timezone handling (different timezones)
- [ ] Test all-day events
- [ ] Verify recurring event expansion
- [ ] Test error states (no permission, API error, etc.)

---

## File Changes Summary

### New Files Created

#### calendar-sync Module
```
calendar-sync/
├── build.gradle.kts (93 lines)
├── src/main/AndroidManifest.xml (7 lines)
├── src/main/java/ink/trmnl/android/buddy/calendar/
│   ├── models/SyncCalendar.kt (98 lines)
│   ├── repository/CalendarProvider.kt (316 lines)
│   ├── repository/CalendarSyncRepository.kt (205 lines)
│   ├── workers/CalendarSyncWorker.kt (73 lines)
│   └── di/CalendarSyncModule.kt (16 lines)
├── proguard-rules.pro (2 lines)
├── consumer-rules.pro (6 lines)
└── README.md (220 lines)

Total: ~936 lines of code + documentation
```

### Modified Files (TBD)
- `settings.gradle.kts` - add `:calendar-sync` module
- `app/build.gradle.kts` - add calendar-sync dependency
- `api/build.gradle.kts` - add calendar sync API endpoints (Phase 3)
- `CHANGELOG.md` - document new feature
- Root `README.md` - mention calendar sync feature

---

## Dependencies

### Added to calendar-sync/build.gradle.kts
- `androidx.core` - database cursor helpers
- `androidx.work.runtime` - WorkManager
- `kotlinx.coroutines.core` - async operations
- `kotlinx.serialization.json` - data serialization
- `dev.zacsweers.metro` - dependency injection
- `timber` - logging

### No new external dependencies beyond existing stack

---

## Known Limitations & Future Enhancements

### Current Limitations
1. **Attendance status** - Always sends "accepted" (Android doesn't expose user's RSVP status for own events)
2. **No event filtering** - Syncs all events in calendar (future: allow filtering)
3. **Fixed time window** - Always 7 days past + 30 days future (future: configurable)
4. **Manual sync only** - Requires WorkManager scheduling (future: adaptive based on user behavior)

### Planned Enhancements
- [ ] Event filtering (busy types, specific calendars)
- [ ] Custom sync frequency (hourly, daily, weekly)
- [ ] Sync statistics dashboard (events synced, last sync time)
- [ ] Multiple TRMNL device sync (send to different devices)
- [ ] Recurring event expansion options
- [ ] Conflict detection (if calendar modified externally)
- [ ] Export/import calendar configurations

---

## TRMNL Companion API Specification (Actual Implementation)

This section details the **actual API specification** used by the TRMNL Companion iOS app. The Android Buddy implementation must align with these endpoints and data formats.

### API Base URL
```
https://trmnl.com/api
```

### Authentication
All endpoints require Bearer token authentication:
```
Authorization: Bearer {api_key}
```

### Calendar Sync Workflow (3-Step Process)

#### Step 1: Validate API Key - GET /me
**Purpose**: Validate the user's API key and retrieve user information.

**Request**:
```http
GET /me HTTP/1.1
Authorization: Bearer {api_key}
Accept: application/json
```

**Response (200 OK)**:
```json
{
  "data": {
    "name": "John Doe",
    "email": "john@example.com",
    "first_name": "John",
    "last_name": "Doe",
    "locale": "en",
    "time_zone": "America/New_York",
    "time_zone_iana": "America/New_York",
    "utc_offset": -300,
    "api_key": "user_..."
  }
}
```

**Error Responses**:
- `401 Unauthorized`: Invalid or expired API key

---

#### Step 2: Get Plugin Settings - GET /plugin_settings
**Purpose**: Retrieve plugin settings for the calendar plugin to get the plugin setting ID.

**Request**:
```http
GET /plugin_settings?plugin_id=calendars HTTP/1.1
Authorization: Bearer {api_key}
Accept: application/json
```

**Query Parameters**:
- `plugin_id` (string): Must be `"calendars"` for calendar plugin

**Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 12345,
      "name": "Calendar",
      "plugin_id": 58
    }
  ]
}
```

**Response Fields**:
- `id` (integer): **Plugin setting ID** - Required for Step 3
- `name` (string): Display name of the plugin setting
- `plugin_id` (integer): Plugin identifier

**Error Responses**:
- `401 Unauthorized`: Invalid API key
- `404 Not Found`: Plugin not found or user doesn't have access

---

#### Step 3: Sync Events - POST /plugin_settings/{id}/data
**Purpose**: Send calendar events to TRMNL for display on the calendar plugin.

**Request**:
```http
POST /plugin_settings/{plugin_setting_id}/data HTTP/1.1
Authorization: Bearer {api_key}
Content-Type: application/json
Accept: application/json

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
        "calendar_identifier": "unique-calendar-id-123"
      }
    ]
  }
}
```

**Path Parameters**:
- `plugin_setting_id` (integer): From Step 2 response

**Request Body Format**:
```
{
  "merge_variables": {
    "events": CalendarEvent[]
  }
}
```

**CalendarEvent Structure**:
```typescript
{
  "summary": string,              // Event title (required)
  "start": string,                // Time only HH:mm format: "14:30" (required)
  "start_full": string,           // ISO8601 with timezone: "2025-08-24T14:30:00.000-04:00" (required)
  "date_time": string,            // ISO8601 with timezone (duplicate of start_full) (required)
  "end": string,                  // Time only HH:mm format: "15:30" (required)
  "end_full": string,             // ISO8601 with timezone: "2025-08-24T15:30:00.000-04:00" (required)
  "all_day": boolean,             // Whether event is all-day (required)
  "description": string,          // Event notes/description (optional)
  "status": string,               // "confirmed" or "tentative" (required)
  "calendar_identifier": string   // Unique calendar ID (required)
}
```

**Response (200-299 Any 2xx)**:
```json
{
  "success": true,
  "message": "Events synced successfully"
}
```

**Response Notes**:
- Any HTTP status code in 200-299 range indicates success
- Server returns confirmation of successful sync

**Error Responses**:
- `400 Bad Request`: Invalid payload format
- `401 Unauthorized`: Invalid API key
- `404 Not Found`: Plugin setting not found
- `422 Unprocessable Entity`: Data cannot be modified (permission or configuration issue)
- `500-599 Server Error`: Server error - retry with exponential backoff recommended

---

### Key Implementation Details

#### Date/Time Formatting
- **ISO8601 with timezone**: `2025-08-24T14:30:00.000-04:00`
  - Format: `YYYY-MM-DDTHH:mm:ss.sssZ±HH:mm`
  - Include milliseconds (`.000`)
  - Include timezone offset
  - Example: `2025-08-24T14:30:00.000-04:00` for EDT (UTC-4)

- **Time-only (HH:mm)**: `14:30`
  - 24-hour format
  - No timezone info in time-only fields
  - Example: `14:30` or `09:00`

- **All-day events**: 
  - `start`: "00:00", `end`: "23:59" (or use midnight + 1 second)
  - Full datetime: Include full day with timezone
  - Example end: `2025-08-24T23:59:59.999-04:00`

#### Time Window
- **Sync range**: 6 days in the past to 30 days in the future
- Example: If today is 2025-08-24, sync events from 2025-08-18 to 2025-09-23

#### Event Merging
- If syncing multiple calendars to one plugin: **merge all events into single request**
- Send one `POST /plugin_settings/{id}/data` request with all merged events
- Server handles deduplication based on: calendar_identifier + start_full + summary

#### Sync Type
- **Full Sync** (not delta): Always send the complete event list for the date range
- Do **not** send only changed events - send all events every time
- Server-side deduplication handles duplicates

#### Calendar Identifier
- Use event's `calendarItemExternalIdentifier` if available
- Fallback to `calendarItemIdentifier` if external ID not available
- Must be unique per calendar and consistent across syncs

---

### Error Handling Strategy

| Status | Meaning | Action |
|--------|---------|--------|
| `401` | Invalid API key | Ask user to re-authenticate |
| `404` | Plugin not found | Verify plugin enabled in TRMNL account |
| `422` | Validation error | Check event data format and retry |
| `500-599` | Server error | Retry with exponential backoff (recommended: max 5 retries) |

---

### Typical Sync Flow (Complete Example)

```
1. User presses "Sync Now" button or scheduled sync triggers
   ↓
2. Get API token from UserPreferencesRepository
   ↓
3. Call GET /me to validate token
   ├─ Success: Continue to step 4
   └─ 401: Show auth error, clear token, ask to re-authenticate
   ↓
4. Call GET /plugin_settings?plugin_id=calendars
   ├─ Success: Extract setting ID from response, continue to step 5
   └─ 404: User must enable calendar plugin in TRMNL account
   ↓
5. Fetch calendar events from Android Calendar Provider
   - 6 days past to 30 days future
   - Transform to CalendarEvent format
   - Merge multiple calendars
   ↓
6. Call POST /plugin_settings/{id}/data with merged events
   ├─ Success (200-299): Show "Sync complete"
   ├─ 422: Show validation error
   └─ 500-599: Retry with exponential backoff
   ↓
7. Record sync timestamp and status
```

---

## References

- [Android Calendar Provider API](https://developer.android.com/guide/topics/providers/calendar-provider)
- [TRMNL Companion iOS Calendar Sync](https://help.trmnl.com/en/articles/12294875-trmnl-companion-for-ios-calendar-sync)
- [TRMNL API Documentation](https://trmnl.com/api-docs/index.html)
- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Android Permissions Best Practices](https://developer.android.com/guide/topics/permissions/overview)

---

## Sign-Off

**Implementation Lead**: Hossain Khan  
**Start Date**: March 2026  
**Phase 1 Completion**: March 22, 2026  
**Next Review**: After Phase 2 completion

---

## Changelog

### Version 1.0 - March 22, 2026
- Initial implementation plan
- Phase 1 (Core Module Foundation) delivered
- Phases 2-6 planned and sequenced
