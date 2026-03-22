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
