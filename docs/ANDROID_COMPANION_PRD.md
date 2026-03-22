# TRMNL Companion for Android - Product Requirements Document

**Version**: 1.0  
**Last Updated**: March 21, 2026  
**Status**: Ready for Development  

---

## Executive Summary

Build an Android companion app that enables users with M365 (Microsoft 365 / Outlook) calendars to sync their events to TRMNL devices. This provides **Android parity** with the existing iOS Companion app, which currently uses EventKit to sync non-Google calendars.

**Business Goal**: Remove friction for Android users with M365 calendars who cannot currently use TRMNL Calendar XL plugin without the iOS Companion app.

**MVP Scope**: M365/Outlook calendar sync only (future: add Fastmail, CalDAV, Apple Calendar support)

---

## Problem Statement

### Current State
- ✅ **iOS users** with M365 calendars → iOS Companion app → Events sync to TRMNL
- ✅ **Google Calendar users** (any OS) → Native TRMNL Google Calendar plugin works on all devices
- ❌ **Android users with M365 calendars** → No solution available

### User Impact
Android users with M365 accounts cannot push calendar events to TRMNL devices unless they:
1. Have those events also on iPhone (to use iOS Companion)
2. Manually enter events (not scalable)
3. Switch to Google Calendar (not always possible in enterprise)

### Solution Approach
Build an Android app that:
1. Authenticates with user's M365 account
2. Fetches calendar events (past 7 days to next 30 days, matching iOS behavior)
3. Pushes events to TRMNL servers
4. Syncs in background with user-controlled frequency

---

## Feature Requirements

### Phase 1: MVP (Required for Launch)

#### 1.1 Authentication
- **Requirement**: Users authenticate with Microsoft 365 account
- **Method**: Microsoft Authentication Library (MSAL) for Android
- **Flow**:
  - User opens app → Sees "Sign in with Microsoft" button
  - Browser/system webview opens → User logs in with M365 credentials
  - App receives OAuth token
  - Token stored securely in Android Keystore
  - Token auto-refreshed before expiration
- **Scopes**: Request minimal permissions (calendar read-only)
  - `Calendars.Read`
  - Avoid requesting email, contacts, or other sensitive scopes
- **Error Handling**: Clear messages if auth fails, expires, or is revoked

**Success Criteria**:
- ✅ User can sign in with M365 account
- ✅ Tokens persist across app restarts
- ✅ Auto-refresh works without user intervention
- ✅ Sign out clears all data securely

---

#### 1.2 Calendar Selection & Mapping
- **Requirement**: Users select which calendars to sync and map to TRMNL Calendar plugin instances
- **Flow**:
  1. After authentication, show "Select Calendars" screen
  2. List all calendars from user's M365 account (primary + secondary)
  3. Show "TRMNL Calendar Instances" user has configured (fetched from TRMNL backend)
  4. Allow user to match calendars to instances
  5. Allow multiple calendars→multiple instances (e.g., personal + work)
  6. Store mapping locally
- **UI Pattern**: Two-column layout with calendars on left, instances on right, drag-to-map or dropdown selectors
- **Data Stored Locally**: Mapping is stored in encrypted SharedPreferences or DataStore

**Success Criteria**:
- ✅ User sees all their calendars
- ✅ User can map calendars to TRMNL instances
- ✅ Mappings persist across restarts
- ✅ Can update mappings anytime

---

#### 1.3 Event Fetching
- **Requirement**: Fetch calendar events using Microsoft Graph API
- **Scope**: Past 7 days to next 30 days (match iOS Companion behavior)
- **Method**: Graph API query
  ```
  GET /me/calendarview?startDateTime={past7days}&endDateTime={next30days}
  ```
- **Event Fields Extracted**:
  - `id` (event ID)
  - `subject` (event title)
  - `start` (start datetime)
  - `end` (end datetime)
  - `isReminderOn` (reminder enabled)
  - `reminderMinutesBeforeStart` (reminder offset)
  - `isAllDay` (all-day event flag)
  - `bodyPreview` (description)
  - `categories` (calendar name/context)
  - `responseStatus` (accepted/tentative/declined)
- **Filtering**: Support calendar-specific queries if API allows; otherwise fetch all and filter client-side
- **Error Handling**: Graceful retry on network failure, clear user messaging on permission errors

**Success Criteria**:
- ✅ App fetches events within time window
- ✅ All required event fields present
- ✅ Handles network failures gracefully
- ✅ Shows loading state during fetch

---

#### 1.4 Event Sync to TRMNL
- **Requirement**: Send fetched events to TRMNL backend using existing `iPhone App` data provider endpoint
- **Architecture**:
  1. App fetches events from Microsoft Graph
  2. App transforms events to TRMNL format
  3. App POSTs to TRMNL backend endpoint (same endpoint as iOS app)
  4. TRMNL backend accepts events and makes available to Calendar plugins
- **Data Format**: Match iOS app payload structure (coordinate with backend team)
  - Example format (to be confirmed):
    ```json
    {
      "events": [
        {
          "id": "event-123",
          "title": "Team Standup",
          "start": "2026-03-21T09:00:00Z",
          "end": "2026-03-21T09:30:00Z",
          "allDay": false,
          "description": "Daily sync",
          "calendar": "Work"
        }
      ],
      "lastSync": "2026-03-21T14:30:00Z"
    }
    ```
- **Endpoint**: POST `/api/calendar-sync` or equivalent (confirm with backend)
- **Authentication**: Use user's TRMNL API token (obtained during initial setup)
- **Error Handling**: Retry failed syncs, show sync status in UI

**Success Criteria**:
- ✅ Events successfully posted to TRMNL backend
- ✅ Events appear in Calendar XL plugin on devices
- ✅ Failed syncs are retried
- ✅ User sees sync status

---

#### 1.5 Background Sync
- **Requirement**: Automatically sync events without user interaction
- **Implementation Option A** (Recommended): Android WorkManager
  - Schedule periodic work (default: every 6 hours, user-configurable)
  - Respects battery saver mode and device constraints
  - Survives app restart
  - Compatible with Android 5.0+
- **Implementation Option B**: Job Scheduler
  - Alternative if WorkManager not suitable
- **Sync Triggers**:
  - Scheduled periodic sync (user-configurable interval)
  - Manual sync via button in app
  - On-demand via notification (optional)
- **Battery Optimization**: Use exponential backoff for failed syncs
- **Data**: Cache last sync timestamp to minimize bandwidth

**Default Behavior**:
- Background sync enabled by default
- Sync every 6 hours (user can adjust: 1, 3, 6, 12, 24 hours)
- Skip sync if device is offline
- Resume when connection restored

**Success Criteria**:
- ✅ Events sync automatically in background
- ✅ User can adjust sync frequency
- ✅ Sync respects device battery state
- ✅ Sync works even if app is closed

---

#### 1.6 Manual Sync Button
- **Requirement**: Allow users to trigger sync immediately
- **UI**: Large "Sign In & Sync" button on main screen (before auth), then "Sync Now" button after auth
- **Behavior**:
  - Show loading indicator during sync
  - Display timestamp of last successful sync
  - Show success/error message after sync completes
  - Disable button briefly to prevent double-taps

**Success Criteria**:
- ✅ User can manually trigger sync
- ✅ Clear feedback on sync status
- ✅ Shows last sync timestamp

---

#### 1.7 Settings Screen
- **Requirement**: Allow users to configure app behavior
- **Settings Options**:
  - Sync Frequency (dropdown: 1h, 3h, 6h, 12h, 24h)
  - Enable/Disable Background Sync (toggle)
  - Manage Calendar Mappings (button → go to mapping screen)
  - View Last Sync Time (display only)
  - Sign Out (button → revoke token, clear data)
- **Confirmation Dialogs**: Warn user before sign-out or disabling sync

**Success Criteria**:
- ✅ User can adjust sync frequency
- ✅ User can disable background sync
- ✅ Settings persist across restarts
- ✅ Sign out properly clears all data

---

#### 1.8 Error States & User Feedback
- **Error Scenarios & Messages**:

| Scenario | Message | Action |
|----------|---------|--------|
| Not signed in | "Sign in to sync your M365 calendar" | Show sign-in button |
| Network offline | "No internet connection. Will retry when online." | Show retry option |
| Auth token expired | "Please sign in again" | Show sign-in button |
| Permission denied | "Calendar access denied. Check app permissions." | Open app settings |
| Sync failed | "Last sync failed. Check internet and try again." | Show retry button |
| No calendars selected | "Select calendars to sync in Settings" | Go to settings |
| Empty calendar list | "No calendars found in your M365 account" | Suggest checking account |

**Success Criteria**:
- ✅ Clear, non-technical error messages
- ✅ User always knows what to do next
- ✅ Error recovery paths provided

---

### Phase 2: Enhanced Features (Post-MVP)

#### 2.1 Multiple Calendar Support
- Extend to Fastmail, CalDAV, Apple Calendar
- Reuse design patterns from iOS Companion

#### 2.2 Push Notifications
- Notify user of upcoming events
- Sync status notifications (optional)

#### 2.3 Calendar Filtering
- Option to exclude certain calendars
- Filter by calendar color or category

#### 2.4 Sync Analytics
- Track sync frequency, success rate
- Help identify issues

---

## User Flows

### Flow 1: Initial Setup
```
1. User opens TRMNL Companion app (first time)
2. Sees splash screen with "Sign in with Microsoft" button
3. Taps button → MSAL login window opens
4. User enters M365 credentials
5. App receives auth token
6. App requests calendar list from Microsoft Graph
7. Shows "Select Calendars" screen
8. User selects calendars to sync
9. App fetches list of TRMNL Calendar instances (from backend)
10. User maps selected calendars to instances
11. App performs initial sync
12. Shows "Sync Complete" message with event count
13. Shows main screen with "Last Sync: just now" and "Sync Now" button
```

### Flow 2: Regular Background Sync
```
1. WorkManager triggers periodic sync (e.g., every 6 hours)
2. App checks if user is still authenticated
3. App fetches calendar events from Microsoft Graph (past 7 days to next 30 days)
4. App posts events to TRMNL backend
5. Updates local "lastSync" timestamp
6. (Optional) Posts notification if new urgent events found
7. Returns control to system
```

### Flow 3: Manual Sync
```
1. User opening app or taps "Sync Now" button
2. App shows loading spinner
3. App fetches events from Microsoft Graph
4. App posts to TRMNL backend
5. App displays "Synced X events" message
6. Updates "Last Sync" timestamp
7. Returns to normal state
```

### Flow 4: Sign Out
```
1. User opens Settings
2. Taps "Sign Out"
3. App shows confirmation: "This will delete all synced data"
4. User confirms
5. App revokes MSAL token
6. App clears all local data (mappings, timestamps, cached events)
7. Returns to login screen
```

---

## Technical Architecture

### Tech Stack
- **Language**: Kotlin (modern Android standard)
- **Build System**: Gradle
- **Min API Level**: Android 8.0 (API 26) for broad compatibility
- **Target API Level**: Android 14 (current)

### Key Dependencies
```gradle
// Authentication
com.microsoft.identity.client:msal:2.0+

// Microsoft Graph API
com.microsoft.graph:msgraph-sdk-android:3.0+

// Background Sync
androidx.work:work-runtime-ktx:2.8+

// UI Framework
androidx.compose.ui:ui:1.5+  // OR Material 3
androidx.lifecycle:lifecycle-viewmodel-ktx:2.6+

// Data Storage
androidx.datastore:datastore-preferences:1.0+
// OR
androidx.security:security-crypto:1.1+  // For SharedPreferences encryption

// Networking
com.squareup.okhttp3:okhttp:4.11+
com.squareup.retrofit2:retrofit:2.9+

// Logging
com.jakewansley:timber:5.0+

// Testing
junit:junit:4.13+
androidx.test.espresso:espresso-core:3.5+
```

### Architecture Pattern
- **MVVM** (Model-View-ViewModel) with Jetpack Compose or Material 3
- **Repository Pattern** for data access
- **Dependency Injection** with Hilt or Dagger
- **Coroutines** for async operations

### File Structure
```
trmnl-companion-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/trmnl/companion/
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   │   ├── CalendarSelectionScreen.kt
│   │   │   │   │   │   ├── MappingScreen.kt
│   │   │   │   │   │   ├── SyncScreen.kt
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   └── components/
│   │   │   │   ├── viewmodels/
│   │   │   │   │   ├── AuthViewModel.kt
│   │   │   │   │   ├── CalendarViewModel.kt
│   │   │   │   │   └── SyncViewModel.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── AuthRepository.kt
│   │   │   │   │   ├── CalendarRepository.kt
│   │   │   │   │   └── TRMNLRepository.kt
│   │   │   │   ├── service/
│   │   │   │   │   ├── GraphApiService.kt
│   │   │   │   │   ├── TRMNLApiService.kt
│   │   │   │   │   └── SyncWorker.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── models/
│   │   │   │   │   └── storage/
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/
│   │   └── test/ & androidTest/
│   └── build.gradle.kts
├── .github/workflows/  (CI/CD)
├── build.gradle.kts
└── settings.gradle.kts
```

### Data Flow Diagram
```
┌──────────────────┐
│  Android Device  │
│                  │
│  ┌────────────┐  │
│  │ TRMNL App  │  │
│  │            │  │
│  │ MSAL Login │  │   → Microsoft OAuth
│  │            │  │   ← Auth Token
│  │            │  │
│  │ Graph API  │  │   → Fetch Events
│  │ Calls      │  │   ← Event Data
│  │            │  │
│  │ TRMNL API  │  │   → POST Events
│  │ Calls      │  │   ← Sync Status
│  │            │  │
│  │ WorkManager│  │   (Background Sync)
│  │            │  │
│  └────────────┘  │
└──────────────────┘
        ↓ (Synced Data)
┌──────────────────┐
│  TRMNL Backend   │
│  Calendar DB     │
└──────────────────┘
        ↓
┌──────────────────┐
│  TRMNL Devices   │
│  Calendar XL     │
└──────────────────┘
```

---

## Data Model

### Event Object
```kotlin
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean,
    val calendarName: String,
    val responseStatus: String,  // "accepted", "tentative", "declined", "notResponded"
    val reminderEnabled: Boolean,
    val reminderMinutesBeforeStart: Int?
)
```

### Calendar Object
```kotlin
data class Calendar(
    val id: String,
    val name: String,
    val ownerEmail: String,
    val hexColor: String?,
    val isDefault: Boolean
)
```

### Calendar Mapping
```kotlin
data class CalendarMapping(
    val localCalendarId: String,
    val localCalendarName: String,
    val trmnlInstanceId: String,
    val trmnlInstanceName: String
)
```

### Sync Metadata
```kotlin
data class SyncState(
    val lastSyncTime: LocalDateTime?,
    val lastSyncStatus: SyncStatus,  // SUCCESS, FAILED, IN_PROGRESS
    val lastErrorMessage: String?,
    val syncCount: Int,
    val failureCount: Int
)
```

---

## Security Considerations

### Authentication Security
- ✅ Use MSAL for OAuth 2.0 (industry standard)
- ✅ Tokens stored in Android Keystore (hardware-backed when available)
- ✅ Tokens never logged or displayed to user
- ✅ Support token refresh automatically
- ✅ Require user to re-authenticate monthly (or after revocation)

### Data Storage
- ✅ Calendar mappings encrypted with Android Keystore
- ✅ Never store calendar events locally longer than 24 hours
- ✅ Clear all sensitive data on sign out
- ✅ No event data in SharedPreferences; use DataStore with encryption
- ✅ Clear app cache on uninstall

### Network Communication
- ✅ HTTPS only (TLS 1.2+)
- ✅ Certificate pinning for TRMNL API (optional, recommended)
- ✅ Request signing/validation with TRMNL backend token
- ✅ No sensitive data in URLs or headers

### Permissions
- ✅ Request minimal permissions (Calendar read-only from M365)
- ✅ Explain why each permission is needed
- ✅ Support runtime permissions (Android 6.0+)
- ✅ Handle permission denial gracefully

### Data Privacy
- ✅ No event data shared with third parties
- ✅ No tracking or analytics of user's calendar content
- ✅ Clear privacy policy in app
- ✅ No syncing of deleted events to TRMNL (design decision: always overwrite)

---

## API Contracts

### Microsoft Graph API (Read-Only)
```
GET /me/calendars
- Returns list of user's calendars

GET /me/calendarview?startDateTime={ISO}&endDateTime={ISO}
- Returns events in time range
- Includes recurring event instances
```

### TRMNL Backend API (to be confirmed with backend team)
```
POST /api/calendar-sync
Authorization: Bearer {trmnl_api_token}

Request Body:
{
  "source": "android-companion",
  "events": [
    {
      "id": "string",
      "title": "string",
      "start": "ISO-8601",
      "end": "ISO-8601",
      "allDay": boolean,
      "description": "string",
      "calendar": "string",
      "responseStatus": "string"
    }
  ],
  "lastSync": "ISO-8601"
}

Response:
{
  "status": "success|error",
  "message": "string",
  "eventCount": number,
  "nextSync": "ISO-8601"
}
```

---

## Success Metrics

### User Adoption
- [ ] 500+ Android installs within 30 days
- [ ] 70%+ of users complete initial setup
- [ ] 80%+ retention after 7 days

### Functionality
- [ ] 95%+ sync success rate
- [ ] <5 second average sync time
- [ ] <100MB app size

### User Satisfaction
- [ ] 4.0+ rating on Google Play
- [ ] <1% crash rate
- [ ] <5% permission denial rate (users who deny calendar access)

---

## Testing Strategy

### Unit Tests
- AuthRepository tests (login/logout, token refresh)
- CalendarRepository tests (event filtering, mapping)
- SyncWorker tests (sync logic, retry behavior)

### Integration Tests
- End-to-end flow: login → select calendars → sync → verify events
- Error handling: network failure, auth expiration, invalid token

### Manual Testing
- Test on Android 8.0, 10, 12, 14 (different API levels)
- Test on various device types (phones, tablets)
- Test with different M365 account types (personal, business, edu)
- Test calendar with 0, 50, 500, 5000 events
- Test on WiFi and cellular
- Test offline sync behavior

---

## Deployment & Release Plan

### Pre-Launch (Internal Testing)
- Internal TestFlight equivalent (Google Play Internal Testing)
- 2 weeks of QA on production Microsoft Graph endpoints
- Coordinate with backend team for API endpoint testing

### Beta Release
- Google Play beta (opt-in)
- 500-1000 external testers
- 2 weeks feedback collection
- Bug fixes and polish

### General Release
- Submit to Google Play
- Press release / announcement in TRMNL help center
- Add to TRMNL plugin documentation

### Post-Launch Support
- Monitor crash metrics and error reports
- Iteratively improve UX based on user feedback
- Plan Phase 2 enhancements (Fastmail, CalDAV, etc.)

---

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|-----------|
| Microsoft Graph API rate limits | Sync failures | Medium | Implement caching, reduce sync frequency |
| Auth token expiration during sync | Session loss | Medium | Implement auto-refresh, graceful retry |
| User revokes permissions | Sync breaks silently | Medium | Monitor for 403 errors, prompt re-auth |
| TRMNL backend API changes | Sync failures | Low | Maintain compatibility, version API |
| Large event payloads | Network timeouts | Low | Batch events, compression on wire |
| Android OS updates | Breaking changes | Low | Test with beta Android versions early |

---

## Success Definition

**MVP is complete when**:

1. ✅ User can sign in with M365 account (MSAL OAuth)
2. ✅ User can select calendars and map to TRMNL instances
3. ✅ App fetches events from Microsoft Graph (7 days past, 30 days future)
4. ✅ App syncs events to TRMNL backend
5. ✅ Background WorkManager sync works without user intervention
6. ✅ Manual "Sync Now" button works
7. ✅ Settings screen allows sync frequency adjustment
8. ✅ Clear error messages for all failure scenarios
9. ✅ Sign out properly clears all data
10. ✅ App is <100MB and works on Android 8.0+
11. ✅ <1% crash rate in internal testing

---

## References

- [Microsoft Authentication Library (MSAL) for Android](https://learn.microsoft.com/en-us/azure/active-directory/develop/msal-android-overview)
- [Microsoft Graph API Calendars](https://learn.microsoft.com/en-us/graph/api/resources/calendar)
- [Android WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Android Security & Privacy](https://developer.android.com/privacy-and-security)
- [TRMNL Companion for iOS](./../../GETTING_STARTED.md) (existing implementation for reference)
- [TRMNL Framework Documentation](https://trmnl.com/framework)

---

## Appendix A: API Request Examples

### Microsoft Graph: Get Calendars
```
GET https://graph.microsoft.com/v1.0/me/calendars
Authorization: Bearer {access_token}

Response:
{
  "value": [
    {
      "id": "calendar-123",
      "name": "Calendar",
      "owner": { "emailAddress": { "address": "user@example.com" } }
    }
  ]
}
```

### Microsoft Graph: Get Calendar Events
```
GET https://graph.microsoft.com/v1.0/me/calendarview?startDateTime=2026-03-14T00:00:00Z&endDateTime=2026-04-20T23:59:59Z
Authorization: Bearer {access_token}

Response:
{
  "value": [
    {
      "id": "event-123",
      "subject": "Team Standup",
      "start": { "dateTime": "2026-03-21T09:00:00", "timeZone": "UTC" },
      "end": { "dateTime": "2026-03-21T09:30:00", "timeZone": "UTC" },
      "isAllDay": false,
      "bodyPreview": "Daily sync",
      "responseStatus": "organizer",
      "isReminderOn": true,
      "reminderMinutesBeforeStart": 15
    }
  ]
}
```

### TRMNL Backend: Sync Events
```
POST https://api.trmnl.com/calendar-sync
Authorization: Bearer {trmnl_api_token}
Content-Type: application/json

Request:
{
  "source": "android-companion",
  "events": [
    {
      "id": "event-123",
      "title": "Team Standup",
      "start": "2026-03-21T09:00:00Z",
      "end": "2026-03-21T09:30:00Z",
      "allDay": false,
      "description": "Daily sync",
      "calendar": "Calendar",
      "responseStatus": "accepted"
    }
  ],
  "lastSync": "2026-03-21T14:30:00Z"
}

Response:
{
  "status": "success",
  "eventCount": 1,
  "nextSync": "2026-03-21T20:30:00Z"
}
```

---

**Document Owner**: Hossain Khan  
**Last Reviewed**: March 21, 2026  
**Next Review**: Upon project completion
