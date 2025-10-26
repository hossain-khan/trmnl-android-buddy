# Product Requirements Document (PRD)
## TRMNL Android Buddy

**Version:** 2.1.0  
**Last Updated:** October 26, 2025  
**Document Owner:** Product Team  
**Status:** Active

---

## 1. Executive Summary

### 1.1 Product Overview
TRMNL Android Buddy is a companion Android application for managing and monitoring [TRMNL](https://usetrmnl.com) e-ink display devices. The app provides users with real-time device status monitoring, battery health tracking, content feed access, and comprehensive device management capabilities—all optimized for mobile use with a privacy-first approach.

### 1.2 Product Vision
To be the essential mobile companion for TRMNL device owners, providing seamless device monitoring, proactive health insights, and easy access to TRMNL content updates while maintaining user privacy and following Material You design principles.

### 1.3 Target Audience
- **Primary**: TRMNL device owners who want to monitor and manage their devices remotely
- **Secondary**: Potential TRMNL customers exploring the ecosystem before purchasing
- **Technical Level**: Beginner to advanced Android users
- **Geographic**: Global (English language only)

### 1.4 Success Metrics
- User retention rate > 70% after 30 days
- Daily active users engaging with device monitoring
- Positive user ratings (4+ stars on Google Play)
- Low battery alert effectiveness (users finding value in notifications)
- Minimal app crashes and API errors

---

## 2. Product Architecture

### 2.1 Technical Stack
- **Language**: Kotlin 2.2.20
- **UI Framework**: Jetpack Compose with Material Design 3
- **Architecture**: Circuit (Compose-driven UDF architecture)
- **Dependency Injection**: Metro with KSP
- **Networking**: Retrofit 3.0.0, OkHttp 5.1.0, EitherNet 2.0.0
- **Database**: Room (TrmnlDatabase, ContentDatabase)
- **Background Processing**: WorkManager
- **Image Loading**: Coil
- **Authentication**: AndroidX Biometric library
- **Build**: Gradle 9.1.0 with Kotlin DSL

### 2.2 Module Structure
```
trmnl-android-buddy/
├── app/              # Main application module
├── api/              # TRMNL API integration
└── content/          # RSS feed and content management
```

### 2.3 Data Layer
- **TrmnlDatabase**: Battery history tracking (Room)
  - BatteryHistoryEntity
  - BatteryHistoryDao
  - BatteryHistoryRepository
  
- **ContentDatabase**: RSS feed content (Room)
  - AnnouncementEntity & AnnouncementDao
  - BlogPostEntity & BlogPostDao
  - AnnouncementRepository
  - BlogPostRepository
  - ContentFeedRepository

- **DataStore Preferences**:
  - UserPreferencesRepository (app settings)
  - DeviceTokenRepository (device-level API keys)

### 2.4 Background Workers
- **BatteryCollectionWorker**: Weekly battery data collection
- **LowBatteryNotificationWorker**: Weekly battery health checks
- **AnnouncementSyncWorker**: Syncs announcements every 2 days
- **BlogPostSyncWorker**: Syncs blog posts every 2 days

---

## 3. Core Features

### 3.1 User Onboarding & Authentication

#### 3.1.1 Welcome Screen
**Purpose**: First-time user experience and returning user welcome

**Requirements**:
- FR-1.1.1: Display welcome message with TRMNL branding
- FR-1.1.2: Show "Welcome back!" message for returning users with 800ms delay and fade-in animation
- FR-1.1.3: Display "What's New" section showing count of recent announcements and blog posts (when content exists)
- FR-1.1.4: Provide "Get Started" button (outlined style) to begin setup
- FR-1.1.5: One-tap access to ContentHubScreen for content exploration (no API token required)
- FR-1.1.6: Display TRMNL "Works With" badge (theme-aware) in bottom right corner (80dp size)
- FR-1.1.7: Support landscape mode with scrollable content
- FR-1.1.8: Hide "What's New" section when RSS feed content is disabled in preferences

**UI Components**:
- Welcome message with EB Garamond typography
- Shimmer effect on "What's New" button (plays once for 2 seconds)
- Theme-aware badge with light/dark variants
- Scrollable content handling

#### 3.1.2 Access Token Setup
**Purpose**: Configure TRMNL user API key for authentication

**Requirements**:
- FR-1.2.1: Provide input field for TRMNL API key with validation
- FR-1.2.2: Display clickable links to TRMNL account settings (https://usetrmnl.com/account)
- FR-1.2.3: Show help documentation link (https://usetrmnl.com/help)
- FR-1.2.4: Validate API key format (alphanumeric string)
- FR-1.2.5: Securely store API key in encrypted DataStore
- FR-1.2.6: Show "Save" button (enabled only when valid key entered)
- FR-1.2.7: Display inline help text with API key location instructions
- FR-1.2.8: Support landscape mode with scrollable content
- FR-1.2.9: Navigate to TRMNL Devices screen upon successful save

**Security**:
- API keys stored in encrypted DataStore Preferences
- Never logged or displayed in plain text after entry
- Cleared on user logout

#### 3.1.3 Biometric Authentication
**Purpose**: Lock app dashboard with device authentication

**Requirements**:
- FR-1.3.1: Support biometric authentication (fingerprint, face recognition)
- FR-1.3.2: Support device credentials (PIN, pattern, password) as fallback
- FR-1.3.3: Use AndroidX Biometric library 1.4.0-alpha04 with BIOMETRIC_STRONG | DEVICE_CREDENTIAL
- FR-1.3.4: Require explicit user interaction (button click) to trigger authentication
- FR-1.3.5: Always show "Unlock" button on authentication screen (no auto-trigger)
- FR-1.3.6: Handle authentication failures gracefully
- FR-1.3.7: Allow users to enable/disable authentication in Settings
- FR-1.3.8: Show error state when device lock is not available
- FR-1.3.9: Follows official Android guidelines: https://developer.android.com/identity/sign-in/biometric-auth

**User Flow**:
1. User enables authentication in Settings
2. App checks if device lock is configured
3. On app launch, authentication screen is shown
4. User taps "Unlock" button
5. BiometricPrompt is triggered
6. Upon success, user accesses device list

---

### 3.2 Device Management

#### 3.2.1 TRMNL Devices Screen (Home)
**Purpose**: Primary dashboard showing all TRMNL devices and their status

**Requirements**:
- FR-2.1.1: Display list of all user's TRMNL devices from API
- FR-2.1.2: Show device cards with: name, device ID, MAC address, battery level, WiFi signal strength
- FR-2.1.3: Display current screen preview image (when device token configured)
- FR-2.1.4: Show refresh rate indicator overlay on preview images (clickable for explanation)
- FR-2.1.5: Visual battery indicators (8 levels) with theme-aware colors
- FR-2.1.6: Visual WiFi indicators (4 levels) with theme-aware colors
- FR-2.1.7: Animated progress indicators on first load (smooth transitions)
- FR-2.1.8: Pull-to-refresh functionality (syncs devices, announcements, and blog posts in parallel)
- FR-2.1.9: Privacy toggle button to show/hide Device IDs and MAC addresses
- FR-2.1.10: Settings icon in top app bar for navigation to Settings
- FR-2.1.11: Loading state with progress indicator
- FR-2.1.12: Error state with "Reset Token" button (on 401 unauthorized)
- FR-2.1.13: Empty state when no devices found
- FR-2.1.14: Material 3 design with dynamic color theming

**Privacy Features**:
- Device IDs: Show only first character and last 2 characters (e.g., `A•••23`)
- MAC addresses: Show only first and last segments (e.g., `AB:••:••:••:••:56`)
- Privacy enabled by default
- Toggle state preserved during navigation

**Content Carousel** (when RSS feed content enabled):
- FR-2.1.15: Display combined announcements and blog posts carousel (latest 3 items)
- FR-2.1.16: Show only unread content (filters out items where isRead = true)
- FR-2.1.17: Hide carousel completely when all content has been read
- FR-2.1.18: Auto-rotation every 5 seconds with smooth fade animation
- FR-2.1.19: Stop auto-rotation permanently when user manually swipes
- FR-2.1.20: Pause rotation on touch interactions
- FR-2.1.21: Page indicators showing current position
- FR-2.1.22: Click to open content in Chrome Custom Tabs and mark as read
- FR-2.1.23: "View All" button to navigate to ContentHubScreen
- FR-2.1.24: Post type indicators: notification icon for announcements, list icon for blog posts
- FR-2.1.25: Display unread indicator, title (max 2 lines), summary (max 2 lines), metadata

**User Actions**:
- Tap device card → Navigate to Device Detail Screen
- Tap device preview → Navigate to Device Preview Screen
- Tap settings icon on device card → Navigate to Device Token Screen
- Tap refresh rate indicator → Show explanation snackbar
- Pull down → Refresh all data (devices + content)
- Toggle privacy → Show/hide PII with snackbar feedback

#### 3.2.2 Device Detail Screen
**Purpose**: Comprehensive view of individual device status and battery history

**Requirements**:
- FR-2.2.1: Display device name in top app bar title (using EB Garamond font)
- FR-2.2.2: Show current battery level with animated progress indicator
- FR-2.2.3: Show battery voltage (when available)
- FR-2.2.4: Show WiFi signal strength with animated progress indicator
- FR-2.2.5: Display battery history chart using Vico library
- FR-2.2.6: Chart features:
  - Line chart with date-based x-axis (MM/dd format)
  - Battery percentage on y-axis (0-100%)
  - Data point dots (8dp circular indicators in primary color)
  - Horizontal scrolling (50dp per point to prevent label truncation)
  - Material 3 theming with dynamic colors
- FR-2.2.7: Show prediction disclaimer about battery trajectory accuracy
- FR-2.2.8: Manual battery recording button (disables if already recorded today)
- FR-2.2.9: Show success message when battery already logged today
- FR-2.2.10: Display empty state when no battery history data available
- FR-2.2.11: Display special message when battery tracking is disabled
- FR-2.2.12: Settings icon in app bar to navigate to Device Token configuration
- FR-2.2.13: Icon shows "Settings" when device token configured, "Configure" when not set
- FR-2.2.14: Icon color changes based on configuration status

**Empty States**:
- No data yet: "No battery history data available yet. Data is collected weekly."
- Tracking disabled: "Battery tracking is disabled. Enable it in Settings to start collecting data."

#### 3.2.3 Device Preview Screen
**Purpose**: View current screen content displayed on TRMNL device

**Requirements**:
- FR-2.3.1: Display full-screen preview of device's current display image
- FR-2.3.2: Show last refresh time indicator (positioned top-right corner)
- FR-2.3.3: Download button in top app bar to save image
- FR-2.3.4: Refresh button in top app bar to reload current display
- FR-2.3.5: Download functionality:
  - Uses Coil's image cache (no re-download)
  - Saves to Pictures directory using MediaStore API
  - No runtime permissions required (scoped storage)
  - Filename format: `Device_Name_YYYYMMDD_HHMMSS.png`
  - Sanitizes special characters in filename
  - Shows snackbar on success/error
- FR-2.3.6: Refresh functionality:
  - Calls `/display/current` API endpoint
  - Shows loading indicator during refresh
  - Displays success/error messages via snackbar
  - Handles HTTP 500 and 429 with user-friendly messages
  - Updates preview image URL on success
- FR-2.3.7: Only accessible when device token is configured
- FR-2.3.8: Loading state with progress indicator
- FR-2.3.9: Error state when preview unavailable

**Note**: Dark mode color inversion is disabled for e-ink display images

#### 3.2.4 Device Token Configuration
**Purpose**: Store device-level API keys for accessing display previews

**Requirements**:
- FR-2.4.1: Input field for device API key with validation
- FR-2.4.2: Display device information (name, ID, MAC address)
- FR-2.4.3: Show format guide: "20+ character hexadecimal string"
- FR-2.4.4: Validate minimum token length of 20 characters
- FR-2.4.5: Clickable link to TRMNL devices page (https://usetrmnl.com/devices/)
- FR-2.4.6: Show help text for finding device API keys
- FR-2.4.7: Securely store device tokens in encrypted DataStore (isolated from user token)
- FR-2.4.8: "Save" button enabled only when valid key entered
- FR-2.4.9: Display success message after saving
- FR-2.4.10: Allow updating existing device token

**Security**:
- Device tokens stored separately from user API key
- Each device has its own encrypted token storage
- Tokens never displayed in plain text after entry

---

### 3.3 Battery Health Tracking

#### 3.3.1 Automatic Battery Collection
**Purpose**: Weekly background collection of battery data for health monitoring

**Requirements**:
- FR-3.1.1: Run BatteryCollectionWorker every 7 days
- FR-3.1.2: Collect battery percentage and voltage for all devices
- FR-3.1.3: Store data in TrmnlDatabase (BatteryHistoryEntity)
- FR-3.1.4: Require network connectivity for worker execution
- FR-3.1.5: Respect user preference (isBatteryTrackingEnabled)
- FR-3.1.6: Run automatically in background without user intervention
- FR-3.1.7: Use WorkManager with ExistingPeriodicWorkPolicy.KEEP
- FR-3.1.8: Fetch device list from TRMNL API
- FR-3.1.9: Record timestamp with each battery reading
- FR-3.1.10: Handle API errors gracefully (retry with exponential backoff)

**Database Schema** (BatteryHistoryEntity):
- deviceId: Int
- batteryPercentage: Double
- batteryVoltage: Double?
- recordedAt: Instant
- deviceName: String

#### 3.3.2 Manual Battery Recording
**Purpose**: Allow users to manually record battery levels at any time

**Requirements**:
- FR-3.2.1: Button on Device Detail Screen to trigger manual recording
- FR-3.2.2: Disable button if battery already recorded today
- FR-3.2.3: Show success message with checkmark when already logged
- FR-3.2.4: Explain that automatic weekly recordings happen when enabled
- FR-3.2.5: Use Material 3 OutlinedButton with battery icon
- FR-3.2.6: Fetch current battery level from API
- FR-3.2.7: Store in same database as automatic collection
- FR-3.2.8: Show confirmation snackbar on success

#### 3.3.3 Battery Tracking Opt-Out
**Purpose**: Allow users to disable automatic battery collection

**Requirements**:
- FR-3.3.1: Setting toggle in Settings screen (default: enabled)
- FR-3.3.2: Preference stored in UserPreferences (isBatteryTrackingEnabled)
- FR-3.3.3: BatteryCollectionWorker checks preference before collecting
- FR-3.3.4: Settings persisted in DataStore across app restarts
- FR-3.3.5: Device Detail Screen shows appropriate empty state when disabled
- FR-3.3.6: Manual recording button still available when tracking disabled

---

### 3.4 Low Battery Notifications

#### 3.4.1 Notification Configuration
**Purpose**: Alert users when device batteries fall below threshold

**Requirements**:
- FR-4.1.1: Master toggle to enable/disable low battery notifications (default: disabled)
- FR-4.1.2: Threshold slider (5% to 50%) with 1% increments
- FR-4.1.3: Visual slider with calculated steps for precise control
- FR-4.1.4: Display current threshold percentage next to slider
- FR-4.1.5: Settings stored in UserPreferences
- FR-4.1.6: Expandable section in Settings screen (collapses when master toggle disabled)
- FR-4.1.7: Runtime permission handling for Android 13+ (POST_NOTIFICATIONS)
- FR-4.1.8: Permission denied dialog with link to system settings
- FR-4.1.9: Debug test button for immediate notification testing (debug builds only)

#### 3.4.2 Low Battery Worker
**Purpose**: Weekly background check for devices below battery threshold

**Requirements**:
- FR-4.2.1: Run LowBatteryNotificationWorker weekly
- FR-4.2.2: Check all devices against configured threshold
- FR-4.2.3: Send aggregated notification for multiple devices
- FR-4.2.4: Respect user preference (isLowBatteryNotificationEnabled)
- FR-4.2.5: Require POST_NOTIFICATIONS permission (Android 13+)
- FR-4.2.6: Use dedicated notification channel "Low Battery Alerts"
- FR-4.2.7: Notification includes:
  - Count of affected devices
  - Device names and battery percentages
  - Tap to open app
  - Auto-cancel enabled
  - BigTextStyle for expandable content
- FR-4.2.8: Handle API errors gracefully

**Notification Channel**:
- ID: `low_battery_channel`
- Name: "Low Battery Alerts"
- Importance: DEFAULT
- Description: "Notifications when device batteries are low"

---

### 3.5 Content Feed (RSS)

#### 3.5.1 Announcements Feed
**Purpose**: Keep users informed of TRMNL product announcements

**Requirements**:
- FR-5.1.1: Fetch announcements from https://usetrmnl.com/feeds/announcements.xml
- FR-5.1.2: Parse RSS feed using RSS-Parser 6.0.8
- FR-5.1.3: Store in ContentDatabase (AnnouncementEntity)
- FR-5.1.4: Offline-first pattern with reactive Flow API
- FR-5.1.5: Display in carousel on home screen (latest 3 unread)
- FR-5.1.6: Dedicated AnnouncementsScreen with full list
- FR-5.1.7: Background sync via AnnouncementSyncWorker every 2 days
- FR-5.1.8: Filter chips: All / Unread / Read
- FR-5.1.9: Date grouping: Today, Yesterday, This Week, Older
- FR-5.1.10: Sticky headers for date categories
- FR-5.1.11: Pull-to-refresh for manual sync
- FR-5.1.12: FAB to "Mark All Read" (shown only when unread exist)
- FR-5.1.13: Click announcement to open in Chrome Custom Tabs and mark as read
- FR-5.1.14: Swipe-to-toggle read/unread status (bidirectional)
- FR-5.1.15: Unread count badge in TopAppBar
- FR-5.1.16: Read status preservation during background sync
- FR-5.1.17: Circular dot indicator for unread announcements
- FR-5.1.18: Loading and empty states for each filter view
- FR-5.1.19: Back button in TopAppBar
- FR-5.1.20: Material 3 design throughout

**Database Schema** (AnnouncementEntity):
- id: String (primary key)
- title: String
- summary: String?
- link: String
- publishedDate: Instant
- isRead: Boolean
- fetchedAt: Instant

**Announcement Authentication Banner**:
- FR-5.1.21: Dismissible banner at top of announcements list
- FR-5.1.22: Explains TRMNL account requirement for viewing full details
- FR-5.1.23: Remember dismissed state in UserPreferences
- FR-5.1.24: Material 3 design with info icon and close button

#### 3.5.2 Blog Posts Feed
**Purpose**: Provide access to TRMNL blog content within the app

**Requirements**:
- FR-5.2.1: Fetch blog posts from https://usetrmnl.com/feeds/posts.xml
- FR-5.2.2: Parse RSS/Atom feed with author extraction and category parsing
- FR-5.2.3: Store in ContentDatabase (BlogPostEntity)
- FR-5.2.4: Extract all images from RSS feed content (not just first)
- FR-5.2.5: Display in carousel on home screen (latest 3 unread)
- FR-5.2.6: Dedicated BlogPostsScreen with full list
- FR-5.2.7: Background sync via BlogPostSyncWorker every 2 days
- FR-5.2.8: Category filter dropdown with "All" option and dynamic categories
- FR-5.2.9: Featured image display (180dp height, crop scaling) using Coil
- FR-5.2.10: Image carousel for posts with multiple images:
  - Auto-rotate through images every 3 seconds
  - Smooth crossfade animation
  - Image counter indicator (e.g., "2/5")
  - Fallback to single image or no image gracefully
- FR-5.2.11: BlogPostCard components with:
  - Featured image with loading indicator
  - Title (2 line max) with bold styling
  - Summary (4 line max, sanitized HTML to plain text, 300 char max)
  - Author name and relative date
  - Unread indicator (blue dot)
  - Favorite toggle button (heart icon)
  - Card tap opens in Chrome Custom Tabs
- FR-5.2.12: Auto-mark as read when clicked
- FR-5.2.13: Toggle favorite functionality (persisted across syncs)
- FR-5.2.14: FAB to "Mark All Read" (shown only when unread exist)
- FR-5.2.15: Loading, error, and empty states with retry
- FR-5.2.16: TopAppBar with dynamic title showing selected category
- FR-5.2.17: Material You theming throughout
- FR-5.2.18: OnConflictStrategy.IGNORE to preserve user state (favorites, read status)

**Database Schema** (BlogPostEntity):
- id: String (primary key)
- title: String
- summary: String
- link: String
- authorName: String?
- category: String?
- publishedDate: Instant
- featuredImageUrl: String?
- imageUrls: List<String> (JSON-encoded)
- isRead: Boolean
- readingProgressPercent: Int
- lastReadAt: Instant?
- fetchedAt: Instant
- isFavorite: Boolean

**HTML Sanitization**:
- Remove all HTML tags and entities
- Limit to 300 characters maximum
- Normalize whitespace for readability
- Try content field first, fall back to description

#### 3.5.3 Content Hub Screen
**Purpose**: Unified navigation for announcements and blog posts

**Requirements**:
- FR-5.3.1: Bottom navigation with two tabs: "Announcements" and "Blog Posts"
- FR-5.3.2: Material 3 NavigationBar with icons and labels
- FR-5.3.3: Tab selection state management with rememberRetained
- FR-5.3.4: Announcements tab embeds AnnouncementsScreen
- FR-5.3.5: Blog Posts tab embeds BlogPostsScreen
- FR-5.3.6: TopAppBar title dynamically updates based on selected tab
- FR-5.3.7: TopAppBar shows tab-specific content:
  - Announcements: unread count badge
  - Blog Posts: selected category and filter dropdown
- FR-5.3.8: Embedded screens hide their own TopAppBars (isEmbedded parameter)
- FR-5.3.9: Smooth crossfade animation (300ms) when switching tabs
- FR-5.3.10: Bottom navigation icons:
  - Announcements: campaign icon (megaphone)
  - Blog Posts: newspaper icon
- FR-5.3.11: NavigableCircuitContent for embedded screens
- FR-5.3.12: Full content area utilizes Scaffold innerPadding

#### 3.5.4 RSS Feed Content Settings
**Purpose**: Granular control over RSS feed syncing and notifications

**Requirements**:
- FR-5.4.1: Single toggle to enable/disable both announcements and blog posts
- FR-5.4.2: Preference: isRssFeedContentEnabled (default: true)
- FR-5.4.3: Automatic migration from legacy announcements_enabled key
- FR-5.4.4: Controls both AnnouncementSyncWorker and BlogPostSyncWorker
- FR-5.4.5: WelcomeScreen respects preference (hides "What's New" when disabled)
- FR-5.4.6: Expandable notification toggle section:
  - Disabled by default (privacy-first)
  - Only shown when main RSS feed toggle is enabled
  - Separate preference: isRssFeedContentNotificationEnabled
  - Runtime permission check for Android 13+ (POST_NOTIFICATIONS)
  - Permission denied dialog with settings link
- FR-5.4.7: Unified notification channel "RSS Feed Updates" for both content types
- FR-5.4.8: Notifications use TRMNL glyph icon for branding
- FR-5.4.9: BigTextStyle support for expandable content
- FR-5.4.10: Auto-cancel enabled (dismiss on tap)
- FR-5.4.11: WorkerScheduler methods: scheduleBlogPostSync, cancelBlogPostSync

**Background Sync Optimization**:
- FR-5.4.12: Sync interval: every 2 days (not every 4 hours)
- FR-5.4.13: Device constraints: idle, charging, network connection
- FR-5.4.14: Uses REPLACE policy for existing users (updates schedule)
- FR-5.4.15: Reduces API calls while ensuring updates during optimal conditions

#### 3.5.5 Chrome Custom Tabs Integration
**Purpose**: In-app browser for announcements and blog posts

**Requirements**:
- FR-5.5.1: BrowserUtils utility with theme-aware Custom Tabs configuration
- FR-5.5.2: Use Material 3 primary color for toolbar
- FR-5.5.3: Use surface color for secondary elements
- FR-5.5.4: Share button enabled
- FR-5.5.5: URL bar visible for user transparency
- FR-5.5.6: Automatically mark content as read when opened
- FR-5.5.7: FLAG_ACTIVITY_NEW_TASK for Application context compatibility
- FR-5.5.8: Fallback to default browser if Custom Tabs unavailable
- FR-5.5.9: INTERNET permission required in AndroidManifest.xml

---

### 3.6 Settings & Preferences

#### 3.6.1 Settings Screen
**Purpose**: Central location for all app configuration

**Requirements**:
- FR-6.1.1: TRMNL News Updates section:
  - Icon: Campaign/announcement icon
  - Enable/disable RSS feed content (announcements + blog posts)
  - Enable/disable RSS feed content notifications (expandable)
- FR-6.1.2: Security section:
  - Icon: Fingerprint icon (error color when device lock unavailable)
  - Enable/disable biometric authentication
  - Single toggle (no separate PIN/biometric configuration)
- FR-6.1.3: Battery History section:
  - Icon: Chart/data icon
  - Enable/disable battery tracking (default: enabled)
- FR-6.1.4: Low Battery Alerts section:
  - Icon: Battery alert icon
  - Master toggle to enable/disable notifications
  - Threshold slider (5% to 50%, expandable when enabled)
- FR-6.1.5: Development section (debug builds only):
  - Icon: Android icon
  - Link to Development Tools screen
- FR-6.1.6: App Information section:
  - Current app version (from BuildConfig)
  - Build type (Debug/Release)
  - "Report Issues" link to GitHub repository
- FR-6.1.7: Material 3 design with dividers using outlineVariant color
- FR-6.1.8: Icons on section headers (not individual items)
- FR-6.1.9: All preferences persisted in DataStore
- FR-6.1.10: Modular UI: separate files for each section

**UI Organization**:
- Main SettingsScreen.kt (350 lines)
- RssFeedContentSection.kt
- SecuritySection.kt
- BatteryTrackingSection.kt
- LowBatteryNotificationSection.kt
- DevelopmentSection.kt
- AppInformationSection.kt

#### 3.6.2 User Preferences Repository
**Purpose**: Manage all user settings and preferences

**Requirements**:
- FR-6.2.1: DataStore-backed repository using Proto DataStore
- FR-6.2.2: Reactive Flow API for preference changes
- FR-6.2.3: UserPreferences data class with all settings:
  - isBatteryTrackingEnabled: Boolean (default: true)
  - isLowBatteryNotificationEnabled: Boolean (default: false)
  - lowBatteryThreshold: Int (default: 20)
  - isRssFeedContentEnabled: Boolean (default: true)
  - isRssFeedContentNotificationEnabled: Boolean (default: false)
  - isAnnouncementAuthBannerDismissed: Boolean (default: false)
  - isAuthenticationEnabled: Boolean (default: false)
- FR-6.2.4: Type-safe getters and setters for each preference
- FR-6.2.5: Default values for all preferences
- FR-6.2.6: Handle migration from legacy preference keys

---

### 3.7 User Account Management

#### 3.7.1 User Account Screen
**Purpose**: Display user information and account actions

**Requirements**:
- FR-7.1.1: Display user email address
- FR-7.1.2: Show TRMNL plan type
- FR-7.1.3: Display account creation date
- FR-7.1.4: "Logout" button with text and icon (error color)
- FR-7.1.5: User profile card with TRMNL brand colors:
  - Light mode: #FFEDE9 container, #3D1410 on-container
  - Dark mode: #4A1F15 container, #FFB4A8 on-container
- FR-7.1.6: Background TRMNL logo watermark (semi-transparent, 600dp, 70% opacity cards)
- FR-7.1.7: Logout clears API token from DataStore
- FR-7.1.8: Logout navigates back to Welcome Screen
- FR-7.1.9: Material 3 design with theme-aware colors
- FR-7.1.10: Accessible from Settings screen (account button in app bar)

**User API**:
- Endpoint: GET /user
- Response: UserResponse { data: User }
- User fields: email, planType, createdAt

---

### 3.8 Development Tools (Debug Only)

#### 3.8.1 Development Screen
**Purpose**: Comprehensive testing tools for notifications and workers

**Requirements**:
- FR-8.1.1: Gated by BuildConfig.DEBUG (not accessible in release builds)
- FR-8.1.2: Accessible via Settings → Development Tools
- FR-8.1.3: Notification testing with mock data:
  - Low battery notifications (1-5 devices slider, 5-50% threshold slider)
  - Blog post notifications (1-10 posts slider)
  - Announcement notifications (1-10 announcements slider)
  - Instant testing without API calls or scheduled intervals
- FR-8.1.4: Worker triggers with real data:
  - Manual one-time LowBatteryNotificationWorker execution
  - Manual one-time BlogPostSyncWorker execution
  - Manual one-time AnnouncementSyncWorker execution
  - Uses real TRMNL API and RSS feed data
- FR-8.1.5: Permission management:
  - Visual permission status display (Android 13+)
  - Color-coded cards: Green (granted) / Red (denied)
  - Quick permission request button
  - Direct link to system notification settings
- FR-8.1.6: Circuit UDF architecture with Metro DI
- FR-8.1.7: Material 3 UI with sliders and buttons
- FR-8.1.8: Documented in docs/TESTING_NOTIFICATIONS.md

#### 3.8.2 Development Configuration
**Purpose**: Feature flags for testing RSS feed notifications

**Requirements**:
- FR-8.2.1: AppDevConfig object with boolean flags:
  - ENABLE_ANNOUNCEMENT_NOTIFICATION (force announcement notifications)
  - ENABLE_BLOG_NOTIFICATION (force blog post notifications)
  - VERBOSE_NOTIFICATION_LOGGING (detailed notification flow debugging)
- FR-8.2.2: Bypasses user preference for notification toggles
- FR-8.2.3: Still respects POST_NOTIFICATIONS permission
- FR-8.2.4: Workers check dev flags alongside user preferences
- FR-8.2.5: Comprehensive documentation with production warnings

---

## 4. User Interface & Design

### 4.1 Design System

#### 4.1.1 Material You / Material 3 Guidelines
**All screens and UI components MUST be Material You compatible**

**Requirements**:
- UID-1.1.1: Use androidx.compose.material3.* components exclusively
- UID-1.1.2: Never use hardcoded colors (e.g., Color(0xFF4CAF50), Color.Red)
- UID-1.1.3: Always use MaterialTheme.colorScheme.* for colors:
  - primary, onPrimary - Main brand colors
  - primaryContainer, onPrimaryContainer - Filled components
  - secondary, tertiary - Accent colors
  - error, onError - Error states
  - surface, onSurface - Backgrounds
  - surfaceVariant, onSurfaceVariant - Alternative surfaces
  - outlineVariant - Dividers and borders
- UID-1.1.4: Support dynamic color (wallpaper-based theming on Android 12+)
- UID-1.1.5: All colors work in both light and dark themes
- UID-1.1.6: Test color contrast in both theme modes

#### 4.1.2 Typography
**Requirements**:
- UID-1.2.1: Use MaterialTheme.typography.* for all text
- UID-1.2.2: EB Garamond font for screen titles (TrmnlTitle component)
- UID-1.2.3: Available typography styles:
  - displayLarge/Medium/Small
  - headlineLarge/Medium/Small
  - titleLarge/Medium/Small
  - bodyLarge/Medium/Small
  - labelLarge/Medium/Small
- UID-1.2.4: Consistent font weights: Bold for unread, SemiBold for read titles

#### 4.1.3 Spacing & Layout
**Requirements**:
- UID-1.3.1: Material spacing system (4dp, 8dp, 12dp, 16dp)
- UID-1.3.2: Edge-to-edge display with transparent system bars
- UID-1.3.3: Use Modifier.padding(innerPadding) with Scaffold
- UID-1.3.4: Status and navigation bars configured in XML themes
- UID-1.3.5: MainActivity enables edge-to-edge via enableEdgeToEdge()

#### 4.1.4 Elevation & Shadows
**Requirements**:
- UID-1.4.1: Use Material elevation tokens (1.dp for subtle hierarchy)
- UID-1.4.2: Avoid excessive elevation (max 4.dp)
- UID-1.4.3: Animated elevation on card press (2dp → 4dp)

#### 4.1.5 Animations & Interactions
**Requirements**:
- UID-1.5.1: Smooth animations following Material 3 motion principles
- UID-1.5.2: Spring-based animations for natural, responsive feel
- UID-1.5.3: Card press animations:
  - Scale to 97-98% with medium bouncy damping
  - Elevation animation for depth perception
  - Ripple effects via interaction states
- UID-1.5.4: List animations:
  - Slide-in using animateItem() modifier
  - Crossfade for tab switching (300ms)
  - FAB appearance with slide, fade, and scale
- UID-1.5.5: Carousel animations:
  - Page alpha animation (92% scale for non-current pages)
  - Page indicators with size animation (8dp → 10dp for selected)
  - Smooth transitions between pages
- UID-1.5.6: Haptic feedback on content card clicks and buttons

### 4.2 Accessibility

#### 4.2.1 Semantic Descriptions
**Requirements**:
- ACC-2.1.1: Comprehensive semantic descriptions for all interactive elements
- ACC-2.1.2: Content descriptions for screen readers
- ACC-2.1.3: Semantic wrappers for decorative elements (unread badges)

#### 4.2.2 Touch Targets
**Requirements**:
- ACC-2.2.1: Minimum touch target size: 48dp x 48dp
- ACC-2.2.2: Adequate spacing between interactive elements
- ACC-2.2.3: Unread badges: 12dp with semantic wrapper

#### 4.2.3 Color Contrast
**Requirements**:
- ACC-2.3.1: Proper semantic color tokens (surfaceContainerLow, outlineVariant)
- ACC-2.3.2: No manual alpha values (e.g., .copy(alpha = 0.7f))
- ACC-2.3.3: Sufficient contrast ratios in both light and dark themes

### 4.3 Composable Previews

#### 4.3.1 Preview Coverage
**Requirements**:
- PRV-3.1.1: All screens have comprehensive Compose previews
- PRV-3.1.2: Use @PreviewLightDark annotation for light and dark modes
- PRV-3.1.3: Preview functions cover:
  - Loading states
  - Empty states (all filter variations)
  - Individual items (read/unread, with/without images)
  - Filter chips and controls
  - Full screen layouts
  - Embedded mode (for ContentHubScreen)
- PRV-3.1.4: Sample data entities with realistic content
- PRV-3.1.5: All previews wrapped in TrmnlBuddyAppTheme

**Screens with Previews**:
- TrmnlDevicesScreen (multiple component previews)
- UserAccountScreen
- DeviceTokenScreen
- AccessTokenScreen
- AnnouncementsScreen (9 previews)
- BlogPostsScreen (9 previews)
- ContentHubScreen (5 previews)
- SettingsScreen (component previews)

---

## 5. Data Management

### 5.1 API Integration

#### 5.1.1 TRMNL API Client
**Purpose**: HTTP client for TRMNL REST API

**Requirements**:
- API-1.1.1: Base URL: https://usetrmnl.com/api
- API-1.1.2: Authentication: Bearer token in Authorization header
- API-1.1.3: Response handling using EitherNet's ApiResult<T, E>
- API-1.1.4: Handle all result types:
  - ApiResult.Success - Successful response
  - ApiResult.Failure.HttpFailure - HTTP errors (4xx, 5xx)
  - ApiResult.Failure.NetworkFailure - Network issues
  - ApiResult.Failure.ApiFailure - API-specific errors
  - ApiResult.Failure.UnknownFailure - Unexpected errors
- API-1.1.5: Dynamic user agent format:
  - `TrmnlAndroidBuddy/Version (Android APILevel; DeviceModel) OkHttp/Version`
  - Includes app version from BuildConfig.VERSION_NAME
  - Includes Android API level and device model
  - Includes OkHttp version
- API-1.1.6: Retrofit 3.0.0 with OkHttp 5.1.0
- API-1.1.7: kotlinx.serialization for JSON parsing

#### 5.1.2 Available Endpoints

**GET /user**
- Returns: UserResponse { data: User }
- Auth: Required (Bearer token)
- Fields: email, planType, createdAt

**GET /devices**
- Returns: DevicesResponse { data: List<Device> }
- Auth: Required (Bearer token)
- Device fields: id, name, friendlyId, macAddress, batteryVoltage, rssi, percentCharged, wifiStrength

**GET /devices/{id}**
- Returns: DeviceResponse { data: Device }
- Auth: Required (Bearer token)

**GET /devices/{id}/display/current**
- Returns: Display { imageUrl, refreshRate }
- Auth: Required (Device token)
- Used for preview image and refresh rate

### 5.2 Database Architecture

#### 5.2.1 TrmnlDatabase (Room)
**Purpose**: App-level data storage for battery history

**Schema**:
```kotlin
@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Int,
    val batteryPercentage: Double,
    val batteryVoltage: Double?,
    val recordedAt: Instant,
    val deviceName: String
)
```

**DAO Operations**:
- Insert battery reading
- Get history for device
- Get latest reading
- Delete old readings

#### 5.2.2 ContentDatabase (Room)
**Purpose**: RSS feed content storage

**Schemas**:
```kotlin
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String?,
    val link: String,
    val publishedDate: Instant,
    val isRead: Boolean,
    val fetchedAt: Instant
)

@Entity(tableName = "blog_posts")
data class BlogPostEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val link: String,
    val authorName: String?,
    val category: String?,
    val publishedDate: Instant,
    val featuredImageUrl: String?,
    val imageUrls: List<String>, // JSON-encoded
    val isRead: Boolean,
    val readingProgressPercent: Int,
    val lastReadAt: Instant?,
    val fetchedAt: Instant,
    val isFavorite: Boolean
)
```

**Type Converters**:
- Instant ↔ Long (milliseconds since epoch)
- List<String> ↔ JSON string (for image URLs)

**Migration**:
- MIGRATION_1_2: Added blog_posts table

### 5.3 DataStore Preferences

#### 5.3.1 User Preferences
**Storage**: Proto DataStore (type-safe)

**Fields**:
- isBatteryTrackingEnabled: Boolean (default: true)
- isLowBatteryNotificationEnabled: Boolean (default: false)
- lowBatteryThreshold: Int (default: 20)
- isRssFeedContentEnabled: Boolean (default: true)
- isRssFeedContentNotificationEnabled: Boolean (default: false)
- isAnnouncementAuthBannerDismissed: Boolean (default: false)
- isAuthenticationEnabled: Boolean (default: false)

#### 5.3.2 API Token Storage
**Storage**: Encrypted DataStore

**Fields**:
- userApiKey: String (TRMNL user API key)

**Security**:
- Encrypted at rest
- Never logged or displayed after entry
- Cleared on logout

#### 5.3.3 Device Token Storage
**Storage**: Encrypted DataStore (separate from user token)

**Fields**:
- Map<Int, String> (deviceId → device token)

**Security**:
- Isolated from user API key
- Per-device encryption
- Never displayed after entry

---

## 6. Background Processing

### 6.1 WorkManager Configuration

**Requirements**:
- BG-1.1: All background tasks use WorkManager
- BG-1.2: CoroutineWorker for suspend functions
- BG-1.3: Metro DI integration with @AssistedFactory
- BG-1.4: Timber logging for all workers
- BG-1.5: Exponential backoff on failures
- BG-1.6: Device constraints: network, battery, idle (where applicable)

### 6.2 Worker Details

#### 6.2.1 BatteryCollectionWorker
- **Schedule**: Every 7 days
- **Constraints**: Network required
- **Policy**: ExistingPeriodicWorkPolicy.KEEP
- **Respects**: isBatteryTrackingEnabled preference
- **Actions**:
  1. Fetch device list from API
  2. Record battery percentage and voltage
  3. Store in TrmnlDatabase
  4. Log success/failure

#### 6.2.2 LowBatteryNotificationWorker
- **Schedule**: Weekly
- **Constraints**: Network required, battery not low
- **Respects**: isLowBatteryNotificationEnabled preference
- **Actions**:
  1. Fetch device list from API
  2. Check devices against threshold
  3. Send aggregated notification if needed
  4. Use notification channel "Low Battery Alerts"

#### 6.2.3 AnnouncementSyncWorker
- **Schedule**: Every 2 days
- **Constraints**: Network required, device idle, charging
- **Policy**: REPLACE (updates existing users)
- **Respects**: isRssFeedContentEnabled preference
- **Actions**:
  1. Fetch RSS feed from URL
  2. Parse with RSS-Parser
  3. Preserve read status of existing items
  4. Store in ContentDatabase
  5. Send notification if new unread items (when enabled)

#### 6.2.4 BlogPostSyncWorker
- **Schedule**: Every 2 days
- **Constraints**: Network required, device idle, charging
- **Policy**: REPLACE (updates existing users)
- **Respects**: isRssFeedContentEnabled preference
- **Actions**:
  1. Fetch RSS/Atom feed from URL
  2. Parse content, extract images, sanitize HTML
  3. Use OnConflictStrategy.IGNORE to preserve user state
  4. Store in ContentDatabase
  5. Send notification if new unread posts (when enabled)

---

## 7. Security & Privacy

### 7.1 Data Security

**Requirements**:
- SEC-1.1: API tokens stored in encrypted DataStore
- SEC-1.2: Device tokens isolated from user token
- SEC-1.3: No sensitive data logged or displayed in plain text
- SEC-1.4: ProGuard rules prevent R8 obfuscation of DI classes
- SEC-1.5: Network traffic uses HTTPS only
- SEC-1.6: No PII in crash reports or analytics

### 7.2 Privacy Features

**Requirements**:
- PRI-2.1: Device IDs obfuscated (first + last 2 chars)
- PRI-2.2: MAC addresses obfuscated (first + last segment)
- PRI-2.3: Privacy toggle enabled by default
- PRI-2.4: Biometric authentication optional
- PRI-2.5: All tracking features opt-out (battery, notifications)
- PRI-2.6: No third-party analytics or tracking

### 7.3 Permissions

**Runtime Permissions**:
- POST_NOTIFICATIONS (Android 13+) - For low battery and RSS feed notifications
- Handled with permission request dialogs
- Permission denied dialogs link to system settings

**Manifest Permissions**:
- INTERNET - Required for API calls and Chrome Custom Tabs
- No storage permissions (uses scoped storage API)

---

## 8. Testing & Quality

### 8.1 Testing Requirements

**Unit Tests**:
- TEST-1.1: All repositories have unit tests
- TEST-1.2: All API services tested with MockWebServer
- TEST-1.3: Use AssertK for all assertions (never JUnit assertions)
- TEST-1.4: Robolectric for Android framework dependencies
- TEST-1.5: Coverage for success cases, error cases, edge cases

**Instrumentation Tests**:
- TEST-1.6: Circuit testing with FakeNavigator
- TEST-1.7: Presenter testing with Presenter.test() helpers

**Testing Tools**:
- AssertK 0.28.1 (assertions)
- MockWebServer (API testing)
- kotlinx-coroutines-test (coroutine testing)
- circuit-test (Circuit testing)
- Robolectric 4.15 (Android framework)

### 8.2 Code Quality

**Linting & Formatting**:
- QUAL-2.1: Kotlinter plugin (ktlint wrapper)
- QUAL-2.2: Run `./gradlew formatKotlin` before commit
- QUAL-2.3: Run `./gradlew lintKotlin` to check formatting
- QUAL-2.4: Follow official Kotlin style guide

**Build Validation**:
- QUAL-2.5: Run `./gradlew test` before commit
- QUAL-2.6: Run `./gradlew assembleDebug` to verify build

### 8.3 Documentation

**Required**:
- DOC-3.1: Keep CHANGELOG.md updated (Keep a Changelog format)
- DOC-3.2: Semantic Versioning (MAJOR.MINOR.PATCH)
- DOC-3.3: Add changes to [Unreleased] section
- DOC-3.4: Avoid duplicate section headers in CHANGELOG
- DOC-3.5: Inline code comments for complex logic
- DOC-3.6: README.md with screenshots and feature list

---

## 9. Release Management

### 9.1 Version Strategy

**Versioning**:
- REL-1.1: Semantic Versioning (MAJOR.MINOR.PATCH)
- REL-1.2: MAJOR: Breaking changes or major new features
- REL-1.3: MINOR: New features, backward compatible (default increment)
- REL-1.4: PATCH: Bug fixes, backward compatible (only for hotfixes)
- REL-1.5: Version in app/build.gradle.kts (versionCode + versionName)

**Current Version**: 2.1.0 (versionCode: 16)

### 9.2 Release Process

**Steps**:
1. Create release branch: `release/X.Y.Z`
2. Update versionCode and versionName in app/build.gradle.kts
3. Update CHANGELOG.md (move [Unreleased] to [X.Y.Z])
4. Commit: "chore: Prepare release X.Y.Z"
5. Create PR from release branch to main
6. Merge PR after review
7. Create and push tag: `X.Y.Z` (not `vX.Y.Z`)
8. Create GitHub Release with changelog

**GitHub Actions**:
- android-release.yml: Builds signed APK and AAB
- Attaches artifacts to GitHub releases
- Uses production keystore from secrets

### 9.3 Distribution

**Channels**:
- Google Play Store (AAB format)
- GitHub Releases (APK + AAB)
- Direct APK sideloading

**Build Artifacts**:
- trmnl-android-buddy-vX.Y.Z.apk (direct installation)
- trmnl-android-buddy-vX.Y.Z.aab (Google Play)

---

## 10. Future Considerations

### 10.1 Potential Enhancements

**Feature Ideas** (not committed):
- Multi-device comparison view
- Custom battery health alerts per device
- Export battery history to CSV
- Widget for home screen device status
- Tasker/automation integration
- Device grouping and tagging
- Custom refresh intervals per device
- Push notifications for device events
- Offline mode with cached data
- Multi-language support

### 10.2 Technical Debt

**Known Areas for Improvement**:
- TrmnlDevicesScreen refactoring (1,303 lines → split into modules)
- Improved error handling for network failures
- Better offline support for content feed
- Reduce dependencies on legacy libraries

---

## 11. Appendices

### 11.1 External Links

- **TRMNL Website**: https://usetrmnl.com
- **TRMNL API Docs**: https://usetrmnl.com/api
- **TRMNL Account**: https://usetrmnl.com/account
- **TRMNL Help**: https://usetrmnl.com/help
- **TRMNL Devices**: https://usetrmnl.com/devices/
- **GitHub Repository**: https://github.com/hossain-khan/trmnl-android-buddy
- **Google Play**: https://play.google.com/store/apps/details?id=ink.trmnl.android.buddy

### 11.2 Related Documents

- **README.md**: Project overview and setup
- **CHANGELOG.md**: Complete version history
- **ARCHITECTURE_ANALYSIS.md**: Code quality analysis
- **GOOGLE_PLAY_LISTING.md**: Play Store metadata
- **RELEASE_SETUP.md**: Keystore and release configuration
- **TESTING_NOTIFICATIONS.md**: Notification testing guide
- **.github/copilot-instructions.md**: Development guidelines

### 11.3 Tech Stack References

- **Circuit**: https://slackhq.github.io/circuit/
- **Metro**: https://zacsweers.github.io/metro/
- **EitherNet**: https://github.com/slackhq/EitherNet
- **AssertK**: https://github.com/assertk-org/assertk
- **Material 3**: https://m3.material.io/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose

---

**Document Version**: 1.0  
**Generated**: October 26, 2025  
**Next Review**: With each major release (X.0.0)
