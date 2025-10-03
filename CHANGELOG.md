# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[unreleased]: https://github.com/hossain-khan/trmnl-android-buddy/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hossain-khan/trmnl-android-buddy/releases/tag/v1.0.0
