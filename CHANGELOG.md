# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Content Module**: Created new `:content` module for RSS feed management (#140, #141)
  - Added RSS-Parser 6.0.8 dependency for parsing Atom/RSS feeds
  - Added Chrome Custom Tabs 1.8.0 dependency for better in-app browser experience
  - **Database Layer**:
    - Created `AnnouncementEntity` Room entity with fields: id, title, summary, link, publishedDate, isRead, fetchedAt
    - Implemented `AnnouncementDao` with queries for fetching, inserting, updating, and marking announcements as read
    - Built `ContentDatabase` with Room type converters for `Instant` serialization
    - Added support for tracking unread announcements and badge counts
  - **Repository Layer**:
    - Implemented `AnnouncementRepository` with offline-first pattern using RSS-Parser and Room
    - Fetches announcements from https://usetrmnl.com/feeds/announcements.xml
    - Provides reactive Flow API for UI updates when data changes
    - Supports read tracking and batch operations
    - Added `markAsUnread()` method for bidirectional read state management
    - Read status preservation during refresh using Flow-based state lookup
  - **Dependency Injection**:
    - Integrated content module with app's Metro DI system
    - Added `ContentDatabase`, `AnnouncementDao`, and `AnnouncementRepository` providers in `AppBindings`
    - RssParser instantiated directly in repository (workaround for Metro compiler limitation)
  - Foundation complete for Issue #141 (Announcements Feed) and Issue #142 (Blog Posts Feed)
- **Announcement Carousel**: Implemented carousel UI component on home screen (#141, Phase 1)
  - Created `AnnouncementCarousel` composable with HorizontalPager
  - Auto-rotation every 5 seconds with pause on manual interaction
  - "View All" button in header to navigate to full announcements screen
  - Displays latest 3 announcements with title, summary, and publish date
  - Shows unread badge for new announcements
  - Page indicators for visual feedback
  - Click to open announcement in Chrome Custom Tabs
  - Manual swipe gestures supported alongside auto-rotation
  - Loading and empty states with Material 3 design
- **Chrome Custom Tabs Integration**: Added in-app browser for announcements (#141, Phase 1)
  - Created `BrowserUtils` utility with theme-aware Custom Tabs configuration
  - Uses Material 3 primary color for toolbar, surface color for secondary elements
  - Share button enabled, URL bar visible for user transparency
  - Automatically marks announcements as read when opened
- **Background Sync**: Implemented WorkManager periodic sync for announcements (#141, Phase 1)
  - Created `AnnouncementSyncWorker` running every 4 hours
  - Network-required constraint to avoid unnecessary battery drain
  - Metro DI integration with @AssistedFactory pattern
  - Auto-scheduled on app startup with KEEP policy to avoid duplicates
  - Timber logging for sync operations and error tracking
- **Manual Refresh**: Added pull-to-refresh functionality (#141, Phase 1)
  - Refresh button syncs both devices and announcements in parallel
  - Shows loading indicators during sync
  - Displays error messages via Snackbar
  - Initial data fetch on first app launch (checks if database empty)
- **Dedicated Announcements Screen**: Full-screen view for all announcements (#141, Phase 2)
  - Created `AnnouncementsScreen` with Circuit architecture (Screen/State/Event/Presenter)
  - **Filtering**: Filter chips for All/Unread/Read views
    - Added `getUnreadAnnouncements()` and `getReadAnnouncements()` repository methods
    - Added `getRead()` DAO query for read-only announcements
    - Reactive filtering with Flow-based updates
  - **Date Grouping**: Announcements grouped by date categories
    - Today, Yesterday, This Week, Older sections
    - Sticky headers for each category using LazyColumn
    - Relative date formatting ("2 days ago")
  - **Interactive Features**:
    - Pull-to-refresh for manual sync
    - FAB to mark all as read (only shown when unread exist)
    - Click announcement to open in Chrome Custom Tabs and mark as read
    - **Swipe-to-Toggle**: Swipe left or right on announcements to toggle read/unread status
      - Visual feedback with eye icons (visibility/visibility_off)
      - Bidirectional swipe support (both directions work)
      - Instant state update without dismissing the item
    - **Unread Count Badge**: Shows circular badge with count in TopAppBar
      - Only visible when unread announcements exist
      - Real-time updates using Flow-based reactive state
    - **Read Status Preservation**: Read status preserved during background sync
      - Existing read/unread states maintained when refreshing feed
      - Prevents previously read announcements from appearing as unread
    - Circular dot indicator for unread announcements (Material 3 design)
  - **Navigation**: ViewAllAnnouncementsClicked event from carousel
  - Material 3 design with proper theming throughout
  - Loading and empty states for each filter view
  - Back button in TopAppBar for easy navigation
  - Displays latest 3 announcements with title, summary, and relative date ("2 days ago")
  - Unread badges with green indicator on unread announcements
  - Page indicators showing current position with circular dots
  - Fade animation between pages for smooth transitions
  - Loading state with CircularProgressIndicator
  - Empty state when no announcements available
  - Integrated into `TrmnlDevicesScreen` above device list
  - **Chrome Custom Tabs Integration**: Clicking announcements opens links in in-app browser
    - Created `BrowserUtils` utility with theme-aware Custom Tabs
    - Uses Material 3 primary and surface colors for toolbar
    - Falls back to default browser if Custom Tabs unavailable
  - **Manual Refresh**: Pull-to-refresh now refreshes both devices and announcements
    - Shows loading indicators for both operations
    - Displays error messages if announcement sync fails
  - **Background Sync**: Automatic announcement refresh every 4 hours
    - Created `AnnouncementSyncWorker` with WorkManager
    - Uses exponential backoff on failures
    - Requires network connection
    - Scheduled on app startup
  - **Initial Fetch**: Announcements automatically fetched on first app launch
    - Checks if database is empty and fetches from RSS feed
    - Runs in background without blocking UI
- **Announcements Toggle**: Added setting to completely disable announcements feature
  - New "Announcements" section in Settings screen with ON/OFF toggle
  - Default: Enabled (announcements shown and synced)
  - When disabled:
    - Announcement carousel hidden from home screen (TrmnlDevicesScreen)
    - Background sync job cancelled (no network usage)
    - Announcements screen still accessible via direct navigation
  - Preference persisted in DataStore
  - Added `isAnnouncementsEnabled` to UserPreferences (defaults to true)
  - WorkerScheduler now includes `scheduleAnnouncementSync()` and `cancelAnnouncementSync()` methods
  - TrmnlDevicesScreen reactively hides/shows carousel based on preference
- **Blog Posts Module**: Implemented database and repository layer for TRMNL blog posts (#142, Phase 1 & 2)
  - **Database Layer**:
    - Created `BlogPostEntity` Room entity with rich metadata fields
      - Core fields: id, title, summary, link, authorName, category, publishedDate
      - Featured image support: `featuredImageUrl` for post thumbnails
      - Reading tracking: `isRead`, `readingProgressPercent`, `lastReadAt`
      - User preferences: `isFavorite` for bookmarking posts
      - Cache metadata: `fetchedAt` for cache invalidation
    - Implemented `BlogPostDao` with comprehensive query methods
      - Basic queries: `getAll()`, `getLatest(limit)`, `getByCategory()`
      - User-centric queries: `getFavorites()`, `getUnread()`, `getRecentlyRead()`
      - Search: `searchPosts(query)` for full-text search across title, summary, and author
      - Utility queries: `getUnreadCount()`, `getCategories()`
      - State management: `markAsRead()`, `updateReadingProgress()`, `toggleFavorite()`
      - Cache cleanup: `deleteOlderThan(threshold)` for old post removal
    - Updated `ContentDatabase` to version 2 with database migration
      - Added `BlogPostEntity` to entities array
      - Created `MIGRATION_1_2` to add blog_posts table
      - Added `blogPostDao()` abstract method for DAO access
  - **Repository Layer**:
    - Implemented `BlogPostRepository` following offline-first pattern
      - Fetches blog posts from https://usetrmnl.com/feeds/posts.xml (Atom format)
      - Parses author, category, and publication metadata from feed
      - Extracts featured images from HTML content using regex
      - Generates summaries from post content (first 200 chars, HTML stripped)
      - Preserves user preferences during refresh (read status, favorites, reading progress)
      - Provides reactive Flow API for all queries
    - Repository methods mirror DAO capabilities:
      - `getLatestPosts()`, `getAllPosts()`, `getPostsByCategory()`
      - `getFavoritePosts()`, `getUnreadPosts()`, `getRecentlyReadPosts()`
      - `searchPosts()`, `getUnreadCount()`, `getCategories()`
      - `refreshPosts()` for manual/background sync
      - `markAsRead()`, `updateReadingProgress()`, `toggleFavorite()`
  - **Dependency Injection**:
    - Added `BlogPostDao` and `BlogPostRepository` providers in `AppBindings`
    - Database migration integrated into ContentDatabase builder
  - **Testing**:
    - Created `BlogPostDaoTest` with 13 test cases covering all DAO operations
    - Created `BlogPostRepositoryTest` with 11 test cases for repository logic
    - Uses `FakeBlogPostDao` for testing without Room dependencies
    - All tests use AssertK for assertions (per project guidelines)
    - Tests cover CRUD operations, filtering, search, and state management
- **Combined Content Feed UI**: Implemented unified carousel for announcements and blog posts (#142, Phase 5)
  - **ContentItem Model**: Created sealed class to represent both announcements and blog posts
    - `ContentItem.Announcement` wrapper for `AnnouncementEntity`
    - `ContentItem.BlogPost` wrapper for `BlogPostEntity`
    - Unified interface for title, summary, link, publishedDate, isRead
    - Type-specific properties (authorName, category, featuredImageUrl for blog posts)
  - **ContentCarousel Component**: Renamed and enhanced carousel for combined content display
    - Changed title from "Announcements" to "Announcements & Blog Posts"
    - Shows top 3 items from both announcements and blog posts, sorted by publishedDate DESC
    - Added `ContentTypeChip` component with Material 3 color scheme:
      - "Announcement" chip uses primaryContainer/onPrimaryContainer
      - "Blog" chip uses secondaryContainer/onSecondaryContainer
    - Maintains auto-rotation every 5 seconds, manual swipe, page indicators, and unread badges
    - Click behavior opens content in Chrome Custom Tabs and marks as read (type-aware)
  - **TrmnlDevicesScreen Updates**: Integrated combined content feed
    - Added `BlogPostRepository` to presenter constructor
    - Updated State to use `content: List<ContentItem>` instead of `announcements`
    - Combines latest announcements (limit 10) and blog posts (limit 10)
    - Sorts combined list by publishedDate DESC and takes top 3 for display
    - Fetches both content types on initial load and refresh
    - Click handler marks content as read based on type (announcement or blog post)
    - Updated all preview functions to work with new ContentItem model
  - **Reactive Updates**: Content automatically refreshes when either repository updates
    - Uses `Flow.combine()` to merge announcements and blog posts streams
    - Real-time sorting and filtering when database changes
    - Preserves user preferences (read status, favorites) during refresh
  - All UI components follow Material 3 design guidelines
  - Compatible with existing announcements toggle setting

### Changed
- **Logging**: Migrated from `android.util.Log` to Timber library for better logging
  - Added Timber 5.0.1 dependency
  - Initialized Timber in `TrmnlBuddyApp` with `DebugTree` for debug builds
  - Migrated all 7 files using `Log` calls to use Timber's fluent API
  - Removed TAG constants as Timber automatically tags logs with the class name
  - Updated logging patterns to use Timber's format string syntax for better performance

## [1.7.0] - 2025-10-23

### Added
- **DevicePreviewScreen Refresh**: Added refresh button to preview screen to reload current display image
  - New refresh button in top app bar alongside download button
  - Calls `/display/current` API to fetch latest device screen image
  - Shows loading indicator while refreshing
  - Displays success/error messages via snackbar
  - Handles HTTP 500 (server error) and 429 (rate limit) with user-friendly messages
  - Updates preview image URL when refresh succeeds

### Changed
- **User Agent**: Implemented dynamic user agent for API requests following industry best practices
  - Format: `TrmnlAndroidBuddy/Version (Android APILevel; DeviceModel) OkHttp/Version`
  - Includes app version from BuildConfig.VERSION_NAME
  - Includes Android API level and device model
  - Includes OkHttp version for better server-side debugging
  - Replaces hardcoded "TrmnlAndroidBuddy/1.0" with dynamic version information

## [1.6.0] - 2025-10-23

### Changed
- **Dependency Updates**: Upgraded key dependencies to stable releases
  - Vico chart library upgraded from 2.0.0-alpha.28 to 2.2.1 (stable)
  - Room database upgraded from 2.7.0-alpha13 to 2.8.3 (stable)
  - Migrated battery history chart to new Vico 2.x API

### Added
- **Privacy Toggle Feedback**: Added snackbar messages when privacy toggle is used in TrmnlDevicesScreen
  - Shows "Personal information hidden for privacy" when privacy is enabled
  - Shows "Personal information now visible" when privacy is disabled
  - Provides clear feedback about PII visibility state changes

### Fixed
- **DeviceTokenScreen UX**: Added clickable link to TRMNL devices page in Device API Key setup
  - Users can now click "device settings" text to navigate directly to https://usetrmnl.com/devices/
  - Improves user experience for finding device API keys during setup
  - Uses Material 3 LinkAnnotation with theme-aware styling

## [1.5.0] - 2025-10-23

### Fixed
- **Critical Crash Fix**: Fixed `ClassCastException` crash on app startup in release builds
  - Added ProGuard rules to prevent R8 from obfuscating Metro DI classes
  - Keeps `TrmnlBuddyApp`, `AppGraph`, and `ComposeAppComponentFactory` classes
  - Prevents dependency injection failures in production builds
  - Resolves crashes reported on Google Pixel 10 Pro (Android 16) and Samsung Galaxy S23 (Android 15)

### Added
- **Low Battery Notification Settings**: New settings section for configuring low battery alerts
  - Master toggle to enable/disable low battery notifications
  - Animated slider to set alert threshold (5% to 50%) with calculated steps for 1% increments
  - Weekly background worker checks battery levels across all devices
  - Aggregated notifications when multiple devices meet the threshold
  - Notification channel for low battery alerts with proper Android O+ support
  - Runtime permission handling for Android 13+ using Accompanist Permissions library (0.37.3)
  - Tappable notifications that open the app via PendingIntent
  - WorkerScheduler interface for testable worker management
  - Debug test button for immediate notification testing (debug builds only)
  - Comprehensive unit tests for settings and preferences
  - Testing documentation in `docs/TESTING_LOW_BATTERY_NOTIFICATIONS.md`
- **Download Display Image Feature**: Added ability to save device preview images to Pictures directory
  - Download button in DevicePreviewScreen top app bar
  - Uses Material 3 image icon with loading indicator during download
  - Downloads via Coil's image cache to avoid re-downloading
  - Saves to device Pictures directory using MediaStore API (Android 10+)
  - No runtime permissions required (uses scoped storage API)
  - Snackbar notification confirms successful save or reports errors
  - Downloaded files named with device name and timestamp (e.g., `Device_Name_20251023_120530.png`)
  - Filename sanitization removes special characters for safe file naming
- **Settings icon in Device Detail screen app bar**
  - Icon displays in top app bar alongside back button
  - Shows "Settings" icon (checkmark) when device token is configured
  - Shows "Configure" icon when device token is not set
  - Icon color changes based on configuration status (primary for configured, onSurfaceVariant for unconfigured)
  - Tapping the icon navigates to Device API Key configuration screen
  - Maintains consistent UX with device list screen settings behavior

## [1.4.0] - 2025-10-22

### Changed
- Moved the image refresh time indicator to the top-right corner of the device preview

### Fixed
- Disabled color inversion for dark mode

## [1.3.0] - 2025-10-22

### Changed
- **Improved Button UX with Text Labels**: Converted icon-only buttons to text buttons with icons for better accessibility and discoverability
  - Settings screen: Account button now displays "Account" text with icon
  - User Account screen: Logout button now displays "Logout" text with icon
  - Follows Material Design 3 guidelines (18dp icons in buttons, 8dp spacing)
  - Improves accessibility by making button actions explicit
  - Maintains theme-aware error color for logout action

## [1.2.0] - 2025-10-21

### Added
- **TRMNL "Works With" Badge on Welcome Screen**: Added official TRMNL branding badge
  - Theme-aware badge that automatically switches between light and dark variants
  - Positioned in bottom right corner of welcome screen (80dp size)
  - Includes official TRMNL brand assets (logos, glyphs, and badge SVGs)
  - Uses `isSystemInDarkTheme()` for seamless theme integration
- **EB Garamond Typography**: Implemented elegant EB Garamond font for branding
  - Applied to all screen titles across the app via `TrmnlTitle` component
  - Google Fonts downloadable font integration with automatic font downloading
  - Created reusable `TrmnlTitle` composable for consistent branding
  - Centralized font logic in single component for maintainability
  - Used on Welcome screen title and all TopAppBar titles (8 screens total)
- **Google Play Store Listing Documentation**: Created `docs/GOOGLE_PLAY_LISTING.md` with comprehensive Google Play Store content
  - App title and descriptions (short and full)
  - Feature sections and highlights
  - Privacy policy information
  - Screenshot captions and promotional text
  - What's new content for version 1.1.0
  - Metadata including category, tags, and content rating
  - Feature graphic ideas and target audience details

## [1.1.0] - 2025-10-21

### Added
- **AAB (Android App Bundle) Distribution**: Release workflow now builds both APK and AAB formats
  - APK for direct installation and sideloading
  - AAB for Google Play Store distribution with optimized downloads
  - Both formats automatically attached to GitHub releases
  - Separate artifacts with version naming (e.g., `trmnl-android-buddy-v1.1.0.apk` and `.aab`)

### Fixed
- **Release Keystore Path Resolution**: Fixed GitHub Actions workflow keystore path issue
  - Changed from module-relative to project-relative path resolution using `rootProject.file()`
  - Resolves workflow failure where keystore file was not found during automated builds
  - Ensures proper keystore location for CI/CD release signing

## [1.0.6] - 2025-10-20

### Added
- **App Information Display**: Version and build type now shown in Settings screen
  - Displays current app version from BuildConfig (e.g., "1.0.5")
  - Shows build type with capitalized first letter (e.g., "Debug" or "Release")
  - Organized in Material 3 card with consistent styling
- **GitHub Project Link**: Direct link to report issues and view project on GitHub
  - "Report Issues" item in App Information section
  - Opens GitHub repository in browser
  - Uses GitHub icon for easy recognition
- **Account Access from Settings**: Added account button to Settings screen app bar
  - Quick access to user account information from Settings
  - Maintains consistent navigation flow
- **Manual Battery Recording**: New button on Device Detail screen to manually record battery levels
  - Allows users to manually track battery health at any time
  - Button automatically disables if battery already recorded today (prevents duplicate same-day entries)
  - Shows success message with checkmark icon when battery is already logged for the day
  - Explains that automatic weekly recordings happen when user preference is enabled
  - Uses Material 3 OutlinedButton design with battery icon
- **Battery History Chart Enhancements**: Improved UX for the battery history chart on Device Detail screen
  - Data point dots: Added circular indicators (8dp) on each data point for better visibility
  - Horizontal scrolling: Chart width dynamically adjusts based on data points (50dp per point) to prevent date label truncation
  - Material 3 theming: Point indicators use primary color for consistency with dynamic theming
- **GitHub Actions Release Workflows**: Automated release build and signing system
  - `android-release.yml` - Builds and publishes release APKs on main branch pushes and GitHub releases
  - `test-keystore-apk-signing.yml` - Manual workflow to validate keystore and APK signing
  - `test-keystore.yml` - Comprehensive keystore diagnostics and troubleshooting workflow
- **CI/CD Release Signing**: Environment variable-based keystore configuration for automated builds
  - Supports `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, and `KEY_ALIAS` environment variables
  - Falls back to debug keystore for local development
  - Automatic APK attachment to GitHub releases
- **Enhanced Keystore Documentation**: Comprehensive guide for production keystore setup
  - Instructions for creating production release keystore
  - Base64 encoding instructions for GitHub Actions
  - Workflow usage documentation
- **Settings Screen**: New screen for app settings accessible from devices list
  - Battery history tracking toggle to opt-out of periodic data collection
  - Material 3 design with Switch component for easy toggling
  - Navigation from TRMNL Devices screen via settings icon in top app bar
- **Battery Tracking Opt-Out**: Users can now disable automatic battery history collection
  - New `isBatteryTrackingEnabled` preference (default: enabled)
  - BatteryCollectionWorker checks preference before collecting data
  - Settings are persisted in DataStore and survive app restarts
- **Device Detail Screen Enhancement**: Shows when battery tracking is disabled
  - Empty state with friendly message when tracking is turned off
  - Directs users to Settings to re-enable tracking
  - Distinguishes between "no data yet" and "tracking disabled" states
- **Battery History Tracking**: Automatic weekly collection of battery data for health monitoring
  - Background worker collects battery levels for all devices every 7 days
  - Room database stores historical battery data with timestamps
  - Battery data includes percentage charged and voltage (when available)
- **Device Detail Screen**: New screen showing comprehensive device information
  - Current battery and WiFi status with progress indicators
  - Historical battery chart using Vico library
  - Visual battery trajectory over time with date labels
  - Material 3 design with smooth animations
  - Navigation from device list by tapping any device card
- **Battery Trajectory Visualization**: Line chart displaying battery drain over time
  - Interactive chart with date-based x-axis (MM/dd format)
  - Battery percentage on y-axis (0-100%)
  - Automatic data point plotting from historical readings
  - Empty state with friendly message when no data available
- **Prediction Disclaimer**: User-friendly note about battery trajectory accuracy
  - Explains that predictions are based on historical data
  - Notes that actual results may vary by usage and conditions
- Room database integration with Metro DI
  - `TrmnlDatabase` with `BatteryHistoryEntity` and `BatteryHistoryDao`
  - `BatteryHistoryRepository` for data operations
  - Database singleton pattern with application context
- Vico chart library (2.0.0-alpha.28) for Material 3 compatible charting

### Changed
- **Account Navigation**: Moved account button from TRMNL Devices screen to Settings screen
  - Improves UI organization by grouping account access with app settings
  - Reduces clutter in main devices screen top app bar
- Release builds now use separate signing configuration instead of debug keystore
  - Local builds: Falls back to debug keystore when production keystore not configured
  - GitHub Actions: Uses production keystore from repository secrets
- Updated `app/build.gradle.kts` with dual signing configuration (debug and release)
- TRMNL Devices screen: Added settings icon to top app bar (between privacy and account icons)
- Battery collection worker now respects user preference for tracking opt-out
- UserPreferences data class includes `isBatteryTrackingEnabled` field
- Updated WorkManager initialization to schedule weekly battery collection
  - Periodic work runs every 7 days with network connectivity requirement
  - Uses `ExistingPeriodicWorkPolicy.KEEP` to avoid duplicate workers

## [1.0.5] - 2025-10-08

### Added
- Device preview refresh rate indicator on device list screen
  - Semi-transparent overlay with rounded corners displayed on top of preview images
  - Shows refresh rate with refresh icon (e.g., "5m", "300s", "1h")
  - Human-readable format: seconds (< 60s), minutes (< 60m), hours (≥ 60m)
  - Overlay positioned at top-left corner with Material 3 surface styling
  - **Clickable indicator** - Tap to see user-friendly explanation via snackbar
    - Example: "This device checks for new screen content every 5 minutes"
    - Helps users understand what the refresh rate means
- New refresh icon drawable for refresh rate indicator
- `FormattingUtils.kt` utility file for common formatting functions
  - `formatRefreshRate()` - Converts seconds to short format (5m, 300s, 1h)
  - `formatRefreshRateExplanation()` - Generates full sentences for user-friendly explanations
- `DeviceIndicatorUtils.kt` utility file for device status UI helpers
  - `getBatteryColor()` - Returns theme-aware battery color based on charge level
  - `getWifiColor()` - Returns theme-aware WiFi color based on signal strength
  - `getBatteryIcon()` - Returns appropriate battery icon based on charge percentage
  - `getWifiIcon()` - Returns appropriate WiFi icon based on signal strength
- Comprehensive unit tests for `FormattingUtils` with 8 test cases

### Changed
- Device preview data structure now includes refresh rate information
  - Added `DevicePreviewInfo` data class to store both image URL and refresh rate
  - Updated presenter to fetch and store refresh rate from Display API
  - Improved preview loading to utilize complete Display API response
- Refactored formatting functions to `ink.trmnl.android.buddy.util` package for better code organization
- Refactored device indicator helper functions to `ink.trmnl.android.buddy.ui.utils` package for reusability

## [1.0.4] - 2025-10-04

### Changed
- Refactored following screens for better code modularity, organization, maintainability and follow Compose best practices
  - TrmnlDevicesScreen
  - UserAccountScreen
  - DeviceTokenScreen
  - AccessTokenScreen
- Improved Circuit state retention across configuration changes

### Added
- Composable previews with light and dark mode previews using `@PreviewLightDark` annotation for following screens
  - TrmnlDevicesScreen
  - UserAccountScreen
  - DeviceTokenScreen
  - AccessTokenScreen

## [1.0.3] - 2025-10-03

### Added
- "Welcome back!" message on Welcome screen for returning users
  - Displays with emoji_people icon after 800ms delay
  - Smooth 1-second fade-in animation
  - Fixed-height container prevents layout shifts
- TRMNL brand orange colors for user profile card
  - Light mode: `#FFEDE9` container, `#3D1410` on-container
  - Dark mode: `#4A1F15` container, `#FFB4A8` on-container
  - Automatic theme adaptation based on system preferences
- Background logo watermark on User Account screen
  - Semi-transparent TRMNL logo (600dp) positioned on right edge
  - 70% opacity cards allow subtle logo visibility
  - Professional branded appearance without interfering with content readability
- "Reset Token" button on unauthorized (401) error state
  - Appears when API token is invalid or expired
  - Clears stored API token from preferences
  - Navigates user back to Access Token screen to re-enter credentials

### Changed
- Welcome screen: Changed "Get Started" button from filled to outlined style
- User Account screen: Information cards now use 70% opacity for watermark visibility
- Updated Device API Key format guide to reflect actual token format
  - Updated format description: "20+ character hexadecimal string (e.g., 1a2b3c4d5e6f7g8h9i0j...)"
  - Updated placeholder text to show generic hexadecimal pattern
  - Added validation for minimum token length of 20 characters
- Moved `redactApiKey` function to `PrivacyUtils` for better code organization

### Fixed
- Welcome screen now scrollable in landscape mode
- Access Token screen now scrollable in landscape mode
- Both screens properly handle keyboard visibility without cutting off content

## [1.0.2] - 2025-10-03

### Added
- Privacy toggle button in TRMNL Devices screen top app bar
  - Allows users to show/hide device IDs and MAC addresses
  - Uses password_2 icons to indicate privacy state (enabled/disabled)
  - Privacy is enabled by default (device IDs and MAC addresses are obfuscated)
  - Toggle state is preserved during screen navigation

## [1.0.1] - 2025-10-03

### Added
- Visual icons for device list card labels to improve information scannability
  - Device ID: Tag icon for quick identification
  - MAC Address: Barcode icon for hardware identification
  - Battery Level: Dynamic battery icon that changes based on charge level (8 distinct levels)
  - WiFi Signal: Dynamic WiFi icon that changes based on signal strength (4 distinct levels)
  - All icons consistently sized at 16dp with 4dp spacing for visual harmony

### Security
- Extended device ID obfuscation to Device API Key management screen for consistent PII protection

## [1.0.0] - 2025-10-03

### Added
- **Device Management**: View and monitor all TRMNL e-ink display devices
- **Device Status Monitoring**: Real-time display of battery level, WiFi signal strength, and device health
- **Animated Progress Indicators**: Smooth animations for battery and WiFi signal strength on first load
- **Device Preview**: View current screen display for devices with configured device tokens
- **Device Token Configuration**: Store and manage device-level tokens for accessing display content
- **User API Key Setup**: Guided onboarding with clickable links to account settings and help documentation
- **Privacy-First Design**: Obfuscated display of sensitive information (Device IDs and MAC addresses)
  - Device IDs show only first character and last 2 characters (e.g., `A•••23`)
  - MAC addresses show only first and last segments (e.g., `AB:••:••:••:••:56`)
- **Material You Support**: Full Material Design 3 with dynamic color theming from Android 12+
- **Dark Mode**: Automatic theme switching based on system preferences
- **Edge-to-Edge Display**: Modern fullscreen layout with transparent system bars
- **Secure Storage**: Encrypted DataStore for API tokens and device tokens
- **User Account Management**: View account information and logout functionality
- **Navigation**: Seamless navigation between device list, device token setup, and user account screens

### Technical Features
- Built with Jetpack Compose and Material Design 3
- Circuit architecture for type-safe navigation and state management
- Metro dependency injection with KSP
- Coil for async image loading with e-ink display optimization
- EitherNet for type-safe API error handling
- Retrofit 3.0.0 + OkHttp 5.1.0 for network operations
- Kotlinx Coroutines for asynchronous operations
- DataStore Preferences for secure data persistence
- Debug keystore for consistent development signing

### Security
- API tokens stored securely in encrypted DataStore
- Device tokens isolated in separate encrypted storage
- Sensitive information (Device IDs, MAC addresses) obfuscated in UI
- Debug keystore for development (production releases require separate keystore)

[unreleased]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.7.0...HEAD
[1.7.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.6.0...1.7.0
[1.6.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.5.0...1.6.0
[1.5.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.4.0...1.5.0
[1.4.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.3.0...1.4.0
[1.3.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.2.0...1.3.0
[1.2.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.6...1.1.0
[1.0.6]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.5...1.0.6
[1.0.5]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.4...1.0.5
[1.0.4]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/hossain-khan/trmnl-android-buddy/releases/tag/1.0.0
