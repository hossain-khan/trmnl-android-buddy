# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[unreleased]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.4...HEAD
[1.0.4]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.3...1.0.4
[1.0.3]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.2...1.0.3
[1.0.2]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.1...1.0.2
[1.0.1]: https://github.com/hossain-khan/trmnl-android-buddy/compare/1.0.0...1.0.1
[1.0.0]: https://github.com/hossain-khan/trmnl-android-buddy/releases/tag/1.0.0
[1.0.0]: https://github.com/hossain-khan/trmnl-android-buddy/releases/tag/v1.0.0
