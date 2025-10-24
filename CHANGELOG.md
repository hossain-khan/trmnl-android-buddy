# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Material 3 Design Enhancements**: Improved UI animations and interactions across ContentHubScreen, BlogPostsScreen, and AnnouncementsScreen
  - **ContentHubScreen**: Added smooth crossfade animation (300ms) when switching between tabs for better visual continuity
  - **BlogPostsScreen**: 
    - Added card press animation with spring physics (scale to 97% with medium bouncy damping) for tactile feedback
    - Enhanced card elevation animation (2dp → 4dp on press) for depth perception
    - Added slide-in animations for list items using `animateItem()` modifier
    - Improved FAB appearance with combined slide, fade, and scale animations
  - **AnnouncementsScreen**:
    - Added list item press animation (scale to 98% with medium bouncy damping) with default Material 3 ripple effect for better interaction feedback
    - Improved list item animations with `animateItem()` for smooth reordering and filtering
    - Improved FAB appearance with combined slide, fade, and scale animations
  - All animations follow Material 3 motion design principles with purposeful, smooth transitions
  - Used spring-based animations for natural, responsive feel
  - Enhanced user delight through micro-interactions and polished UI feedback
  - Animations are consistent across both light and dark themes

### Added
  - Added Extended FloatingActionButton with "Mark All Read" text and icon
  - Shows only when there are unread blog posts
  - Added `markAllAsRead()` method to BlogPostDao and BlogPostRepository
  - Added `MarkAllAsRead` event to BlogPostsScreen
  - Added `unreadCount` to BlogPostsScreen state to track unread posts
  - Consistent with Announcements screen FAB functionality

- **Announcement Authentication Banner**: Informational banner explaining TRMNL account requirement
  - Dismissible banner shown at top of announcements list
  - Informs users that announcements require authentication to view full details on web
  - Banner remembers dismissed state in user preferences (never shows again after dismissal)
  - Material 3 design with info icon and close button
  - Added `isAnnouncementAuthBannerDismissed` preference to UserPreferences
  - Added `setAnnouncementAuthBannerDismissed()` method to UserPreferencesRepository

- **UI/UX Refinements for Content Feed**: Complete Material Design 3 and accessibility overhaul
  - **Accessibility Improvements** (Phase 1):
    - Added comprehensive semantic descriptions to all interactive elements
    - Fixed touch target sizes (unread badge now 12dp with semantic wrapper)
    - Replaced manual color alpha with proper semantic color tokens (surfaceContainerLow, outlineVariant, etc.)
    - Added content descriptions for screen readers on carousel, cards, buttons, and indicators
  - **Material Design 3 Compliance** (Phase 2):
    - Applied consistent Material spacing system (4dp, 8dp, 12dp, 16dp)
    - Replaced all manual `.copy(alpha = 0.7f)` with semantic color scheme tokens
    - Upgraded chip implementation from AssistChip to proper semantic Surface with custom styling
    - Used Material elevation tokens (1.dp instead of 2.dp for subtle hierarchy)
    - Added proper ripple effects via interaction states
  - **Performance Optimizations** (Phase 3):
    - Added lifecycle awareness to carousel auto-rotation (pauses when app is backgrounded)
    - Pauses carousel rotation on user interaction (tap/swipe) for 2 seconds
    - Optimized image loading with remember{} for expensive calculations
    - Used DisposableEffect for proper lifecycle observer cleanup
  - **UX Polish** (Phase 4):
    - Implemented shimmer loading skeleton instead of simple spinner
    - Added haptic feedback on content card clicks and "View All" button
    - Animated card elevation and background color on press (150ms tween)
    - Enhanced page indicators with size animation (8dp → 10dp for selected)
    - Improved carousel transitions with scale + alpha animation (92% scale for non-current pages)
    - Added subtle font weight changes (Bold for unread, SemiBold for read titles)
    - Used tertiary color for blog post categories for better visual hierarchy
    - Increased summary max lines from 2 to 3 for better content preview

- **Blog Post Repository Tests**: Comprehensive unit tests for RSS parsing and data transformation
  - Created `BlogPostRepositoryTest` with Robolectric 4.15 for Android framework support
  - 3 test methods covering: RSS parser content extraction, HTML sanitization, full refresh flow
  - Uses real Atom feed XML from `docs/usetrmnl.com-blog-posts.xml` as test data
  - MockWebServer for simulating HTTP responses
  - Validates that RSS parser correctly extracts `<content>` field from Atom feeds
  - Verifies HTML sanitization removes tags and limits to 300 characters
  - All tests passing with comprehensive debug output

- **Combined Content Feed Feature (#142) - Complete Implementation**:
  - **Phase 1-6 Complete**: Full blog posts integration with announcements
  - **Architecture**: Offline-first, reactive Flow-based data layer with Room database
  - **UI/UX**: Material You compliant throughout, supports dynamic theming and dark mode
  
- **Background Blog Post Sync**: Automatic periodic sync worker (#142, Phase 6)
  - Created `BlogPostSyncWorker` with CoroutineWorker and Metro DI integration
  - Periodic sync every 24 hours using WorkManager
  - Work constraints: requires network connectivity and battery not low
  - Smart notification system:
    - Tracks unread count before/after refresh to detect new posts
    - Shows notification only when new posts are available
    - Custom notification channel "Blog Post Updates" (Android O+)
    - Notification displays count (e.g., "3 new blog posts available")
    - Tapping notification opens MainActivity
    - Auto-dismiss on tap
  - Error handling with exponential backoff retry
  - Worker registered in AppWorkerFactory with AssistedFactory pattern
  - Scheduled in TrmnlBuddyApp.onCreate() with KEEP policy (prevents duplicates)
  - Timber logging for debugging
  - Added `getUnreadCount()` to BlogPostRepository (suspending function using Flow.first())

- **Compose Previews for Content Screens**: Added comprehensive preview coverage for better developer experience
  - `AnnouncementsScreen`: 9 preview functions covering loading, empty (all/unread filters), individual items (read/unread), filter chips, full screen, and embedded mode
  - `BlogPostsScreen`: 9 preview functions covering loading, error, empty, cards (with image/favorited/no image), full screen, filtered by category, and embedded mode
  - `ContentHubScreen`: 5 preview functions covering both tabs (announcements/blog posts), unread badges, category filtering, and navigation bar
  - All previews use `@PreviewLightDark` for both light and dark mode rendering
  - Sample data entities created with realistic content and varied states
  - Follows existing app pattern established in `TrmnlDevicesScreen`
  - All previews wrapped in `TrmnlBuddyAppTheme` for Material You theming


### Changed
- **Copilot Instructions**: Updated GitHub operations guidance for AI assistants
  - Added requirement to always use GitHub MCP tools instead of `gh` CLI
  - Specified MCP tools for PRs, issues, and search operations
  - Added fallback instructions if MCP tools are unavailable
  - Improved consistency and error handling for GitHub operations

- **Always-Visible Filter Bar in Announcements**: Filter chips now permanently pinned at the top
  - Moved filter chips outside the scrollable LazyColumn to ensure they're always visible
  - Filters are now fixed at the top in a separate Surface layer above the pull-to-refresh list
  - Date headers remain sticky within the scrollable area for category navigation
  - Pull-to-refresh functionality preserved and working correctly
  - Better UX: filters never scroll away, users can always change view without scrolling back to top
  - Follows Material Design 3 pattern of persistent filter controls for long lists

- **ContentHubScreen UI Refinement**: Eliminated nested TopAppBars for better space utilization
  - Embedded screens (AnnouncementsScreen, BlogPostsScreen) now hide their TopAppBars when displayed in ContentHubScreen
  - ContentHubScreen's single TopAppBar dynamically shows tab-specific content:
    - Announcements tab: displays unread count badge
    - Blog Posts tab: displays selected category and filter dropdown
  - Added `isEmbedded` parameter to both screen data classes (default: `false`)
  - Presenters automatically set `showTopBar = false` when embedded
  - Improved vertical space efficiency by removing duplicate toolbar areas
  - Full content area now utilizes padding from Scaffold's innerPadding directly
  - Bottom navigation icons updated for better semantic meaning:
    - Announcements: `campaign` icon (megaphone/broadcast symbol)
    - Blog Posts: `newspaper` icon (news/articles symbol)
  - Tests passing: 125 tasks
- **Blog Posts List UI Polish**:
  - Removed redundant category chip from list items (category already shown in TopAppBar filter)
  - Updated favorite icon to use new Material Design icons:
    - Unfavorited: `heart_plus` icon (add to favorites)
    - Favorited: `favorite_fill1` icon (filled heart)
  - Cleaner, more focused list item layout
  - **Summary Text Improvements**:
    - HTML content sanitized to plain text (removes all HTML tags and entities)
    - Summary limited to 300 characters maximum for cleaner display
    - UI updated to show 4 lines max (previously 3) with ellipsis for overflow
    - Normalized whitespace for better readability
    - **Tries content field first, falls back to description** for better summary extraction
    - **Existing posts automatically updated** with sanitized summaries on refresh
- **Blog Posts List Screen**: Full-featured blog posts viewer (#142, Phase 5)
  - Created `BlogPostsScreen` with Circuit architecture (Screen, State, Event, Presenter, Content)
  - List view of all blog posts from TRMNL RSS feed with pull-to-refresh
  - Category filter dropdown with "All" option and dynamic categories from posts
  - Featured image display when available using Coil (180dp height, crop scaling)
  - BlogPostCard component with:
    - Featured image with loading indicator
    - Title (2 line max) with bold styling
    - Summary (4 line max) with overflow ellipsis and sanitized plain text
    - Author name and relative date ("2 days ago")
    - Unread indicator (blue dot) for new posts
    - Favorite toggle button (heart icon with error color when active)
    - **Card tap opens blog post in Chrome Custom Tabs** with theme-aware colors
  - Click to open blog post in Chrome Custom Tabs with theme colors
  - Auto-mark as read when clicked
  - Toggle favorite functionality
  - Loading, error, and empty states with retry functionality
  - TopAppBar with dynamic title showing selected category
  - Replaced placeholder in ContentHubScreen with actual BlogPostsScreen
  - Integrated with existing BlogPostRepository (RSS parsing, offline-first)
  - Material You theming throughout (color schemes, typography, proper contrast)
  - Tests passing: 125 tasks
- **Content Hub Screen**: Unified navigation for announcements and blog posts (#142, Phase 4)
  - Created `ContentHubScreen` with Circuit architecture (Screen, Presenter, Content)
  - Bottom navigation with two tabs: "Announcements" and "Blog Posts"
  - Material 3 NavigationBar with proper icon and label styling
  - Tab selection state management with `rememberRetained`
  - Announcements tab embeds existing `AnnouncementsScreen` using `NavigableCircuitContent`
  - Blog Posts tab shows placeholder UI ("Coming soon in Phase 5")
  - TopAppBar title dynamically updates based on selected tab
  - Updated "View All" navigation from devices screen to open ContentHubScreen
  - Proper Material You theming with color schemes
  - Tests passing: 125 tasks

### Fixed
- **Announcement Summary Display**: Hide summary text when it's blank/empty
  - AnnouncementItem now checks if summary is not blank before displaying
  - Prevents showing empty summary text views in the announcements list
  - Improves UI cleanliness when announcements lack summary content

- **Chrome Custom Tabs Launch Fix**: Added FLAG_ACTIVITY_NEW_TASK to fix activity context issue
  - Fixed AndroidRuntimeException when opening blog posts/announcements
  - Error: "Calling startActivity() from outside of an Activity context requires the FLAG_ACTIVITY_NEW_TASK flag"
  - Root cause: Using Application context instead of Activity context to launch Custom Tabs
  - Solution: Added `FLAG_ACTIVITY_NEW_TASK` flag to CustomTabsIntent
  - Links now properly open in Chrome Custom Tabs without errors

- **INTERNET Permission Missing**: Added missing INTERNET permission to AndroidManifest.xml
  - Fixed critical bug where clicking blog posts and announcements didn't open browser
  - Chrome Custom Tabs requires INTERNET permission to launch URLs
  - Now blog post and announcement links properly open in Chrome Custom Tabs

- **Blog Post Summary Logging**: Added Timber logging to help diagnose RSS parsing issues
  - Replaced println statements with proper Timber.d() calls
  - Logs content/description lengths and summary lengths during parsing
  - Logs counter for updated summaries to track refresh behavior
  - Tests prove parsing works correctly; production issues may be due to old cached data

- **Blog Posts Favorite Persistence**: Fixed critical bug where favorite status was not persisted
  - Root cause: `BlogPostDao.insertAll()` was using `OnConflictStrategy.REPLACE` which completely replaced existing rows
  - This overwrote user state fields (`isFavorite`, `isRead`, `readingProgressPercent`) during refresh
  - Solution: Changed to `OnConflictStrategy.IGNORE` to prevent overwriting existing posts
  - Now only new posts are inserted, preserving all user interactions on existing posts
  - Favorite toggle now correctly persists across app restarts and background syncs

- **Content Carousel UI**: Restored horizontal auto-rotating carousel behavior (#142)
  - Fixed regression where carousel displayed 3 cards vertically instead of horizontal pager
  - Re-implemented `HorizontalPager` with auto-rotation every 5 seconds
  - Added page indicators showing current position in carousel
  - Added page alpha animation for smooth fade effect during transitions
  - Updated ContentItemCard to match AnnouncementCard layout style
  - Card now uses surface color with proper elevation instead of surfaceVariant
  - Post type chip (Announcement/Blog) displayed above title
  - Unread badge positioned in top-right corner
  - Improved spacing and typography (titleMedium for title, bodyMedium for summary)
  - Loading and empty states display in fixed height box (120dp)
  - All Material You colors preserved with proper theming support

### Added
- **Combined Content Carousel**: Updated devices screen with unified announcements and blog posts carousel (#142, Phase 3)
  - Replaced announcements-only carousel with combined content feed showing both announcements and blog posts
  - Added `ContentCarousel` composable with "Announcements & Blog Posts" header
  - Implemented `ContentItemCard` with Material 3 AssistChip for visual post type differentiation
  - Post type indicators: 🔔 Notification icon for announcements (primaryContainer), 📄 List icon for blog posts (secondaryContainer)
  - Displays unread indicator (8dp circle with primary color)
  - Shows title (max 2 lines), summary (max 2 lines), and metadata row with relative date
  - Blog posts include category badge when available
  - Integrated `ContentFeedRepository` in `TrmnlDevicesPresenter` for fetching latest 3 content items
  - Updated refresh logic to fetch both announcements and blog posts in parallel
  - Type-aware event handling for marking content as read based on content type (Announcement vs BlogPost)
  - Updated State, Event, and all composables to use `ContentItem` instead of `AnnouncementEntity`
  - Added `formatRelativeDate()` helper function for user-friendly time display
  - All preview data updated with sample announcements and blog posts
  - Tests passing: 125 tasks
- **Combined Content Feed Repository**: Unified content feed architecture for announcements and blog posts (#142, Phase 2)
  - Created `BlogPostRepository` for fetching, parsing, and managing blog posts from https://usetrmnl.com/feeds/posts.xml
  - Implemented `ContentItem` sealed class for type-safe representation of announcements and blog posts
  - Built `ContentFeedRepository` to combine both content types, sorted by published date
  - RSS-Parser integration with author extraction, category parsing, and featured image detection
  - Offline-first pattern with read status preservation during refresh
  - Added providers in `AppBindings` for `BlogPostRepository` and `ContentFeedRepository`
  - Support for favorites, reading progress tracking, and search functionality
- **Blog Posts Database Schema**: Extended ContentDatabase for combined announcements and blog posts feed (#142, Phase 1)
  - Created `BlogPostEntity` Room entity with rich metadata: id, title, summary, link, authorName, category, publishedDate, featuredImageUrl, isRead, readingProgressPercent, lastReadAt, fetchedAt, isFavorite
  - Implemented `BlogPostDao` with queries for filtering by category, favorites, unread status, search, and reading progress tracking
  - Migrated `ContentDatabase` from version 1 to version 2 with backward-compatible migration
  - Added `ContentDatabase.MIGRATION_1_2` to create blog_posts table
  - Updated `AppBindings` to provide `BlogPostDao` and include migration in Room builder
  - Foundation complete for unified content feed architecture (announcements + blog posts)
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
