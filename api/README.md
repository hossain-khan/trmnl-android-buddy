# TRMNL API Module

This module provides API integration with the TRMNL server for the Android Buddy app.

## Overview

The `api` module is responsible for all network communication with the TRMNL server located at [https://usetrmnl.com](https://usetrmnl.com). It provides a clean, type-safe interface for interacting with TRMNL's REST API.

## API Documentation

Full API documentation is available at: [https://usetrmnl.com/api-docs/index.html](https://usetrmnl.com/api-docs/index.html)

## Architecture

This module follows a clean architecture approach:

- **Models**: Data classes representing API requests and responses (kotlinx.serialization)
- **Services**: Retrofit interfaces defining API endpoints
- **Client**: Configured HTTP client with proper error handling
- **Repository**: Data layer abstraction for consuming modules

## Technology Stack

- **Retrofit**: Type-safe HTTP client
- **OkHttp**: HTTP client for making network requests
- **Kotlinx Serialization**: JSON serialization/deserialization
- **Kotlin Coroutines**: Asynchronous programming
- **EitherNet**: Type-safe API result handling (by Slack)

## Usage

Add the module dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":api"))
}
```

### Example Usage

#### Basic API Client Setup

```kotlin
// Create API client with debug logging enabled
val trmnlApi = TrmnlApiClient.create(
    apiKey = "user_abc123",
    isDebug = true
)
```

#### Using EitherNet's ApiResult (Recommended)

EitherNet provides type-safe API result handling with sealed types:

```kotlin
// Direct API usage
lifecycleScope.launch {
    when (val result = trmnlApi.getDevices("Bearer user_abc123")) {
        is ApiResult.Success -> {
            val devices = result.value.data
            println("Found ${devices.size} devices:")
            devices.forEach { device ->
                println("  ${device.name}: ${device.percentCharged}% battery")
            }
        }
        is ApiResult.Failure.HttpFailure -> {
            println("HTTP Error: ${result.code}")
        }
        is ApiResult.Failure.NetworkFailure -> {
            println("Network Error: ${result.error}")
        }
        is ApiResult.Failure.ApiFailure -> {
            println("API Error: ${result.error}")
        }
        is ApiResult.Failure.UnknownFailure -> {
            println("Unknown Error: ${result.error}")
        }
    }
}
```

#### Using the Device Repository

The repository provides a cleaner API with proper error handling:

```kotlin
// Create repository
val repository = TrmnlDeviceRepository(
    apiService = trmnlApi,
    apiKey = "user_abc123"
)

// Fetch all devices
lifecycleScope.launch {
    when (val result = repository.getDevices()) {
        is ApiResult.Success -> {
            val devices = result.value
            println("Found ${devices.size} devices")
            devices.forEach { device ->
                println("  ${device.name}: ${device.getBatteryStatus()} (${device.percentCharged}%)")
            }
        }
        is ApiResult.Failure.HttpFailure -> {
            when (result.code) {
                401 -> println("Unauthorized - check your API key")
                else -> println("HTTP Error: ${result.code}")
            }
        }
        is ApiResult.Failure -> {
            println("Request failed")
        }
    }
}

// Fetch a specific device
lifecycleScope.launch {
    when (val result = repository.getDevice(12822)) {
        is ApiResult.Success -> {
            val device = result.value
            println("Device: ${device.name}")
            println("Battery: ${device.getBatteryStatus()} (${device.percentCharged}%)")
            println("WiFi: ${device.getWifiStatus()} (${device.wifiStrength}%)")
        }
        is ApiResult.Failure.HttpFailure -> {
            when (result.code) {
                404 -> println("Device not found")
                401 -> println("Unauthorized")
                else -> println("HTTP Error: ${result.code}")
            }
        }
        is ApiResult.Failure -> {
            println("Request failed")
        }
    }
}

// Get devices with low battery
lifecycleScope.launch {
    when (val result = repository.getDevicesWithLowBattery()) {
        is ApiResult.Success -> {
            val lowBatteryDevices = result.value
            if (lowBatteryDevices.isEmpty()) {
                println("All devices have sufficient battery!")
            } else {
                println("${lowBatteryDevices.size} device(s) need charging:")
                lowBatteryDevices.forEach { device ->
                    println("  ${device.name}: ${device.percentCharged}%")
                }
            }
        }
        is ApiResult.Failure -> {
            println("Failed to check battery levels")
        }
    }
}
```

#### EitherNet Result Types

EitherNet provides these sealed result types for comprehensive error handling:

- **`ApiResult.Success<T>`** - Successful response with data (`result.value`)
- **`ApiResult.Failure.NetworkFailure`** - Network connectivity issues (`result.error`)
- **`ApiResult.Failure.HttpFailure`** - HTTP errors like 401, 404, 500 (`result.code`)
- **`ApiResult.Failure.ApiFailure`** - API-specific errors with decoded error body (`result.error`)
- **`ApiResult.Failure.UnknownFailure`** - Unexpected errors (`result.error`)

## Features

- Type-safe API calls with Retrofit
- Automatic JSON serialization with kotlinx.serialization
- **EitherNet for sealed type-safe error handling**
- Repository pattern for clean architecture
- Coroutine support for async operations
- ProGuard rules for release builds
- Device health monitoring (battery, WiFi)
- Built-in authentication with Bearer tokens
- Comprehensive error handling (Network, HTTP, API, Unknown)

## Implemented Endpoints

### Devices API
- ✅ `GET /devices` - List all user devices
- ✅ `GET /devices/{id}` - Get specific device details

### Display API (Device API)
- ✅ `GET /display/current` - Get current display content for a device

### Users API
- ✅ `GET /me` - Get authenticated user information

### Coming Soon
- 🔜 Playlists API (`/playlists/items`)
- 🔜 Plugin Settings API (`/plugin_settings`)
- 🔜 Models & Palettes API (`/models`, `/palettes`)

## Module Structure

```
api/
├── src/
│   ├── main/
│   │   ├── java/ink/trmnl/android/buddy/api/
│   │   │   ├── TrmnlApiClient.kt      # HTTP client factory
│   │   │   ├── TrmnlApiService.kt     # Retrofit API interface
│   │   │   ├── TrmnlDeviceRepository.kt # Repository layer
│   │   │   └── models/                # API request/response models
│   │   │       ├── ApiError.kt
│   │   │       ├── Device.kt
│   │   │       ├── DeviceResponse.kt
│   │   │       ├── DevicesResponse.kt
│   │   │       ├── Display.kt
│   │   │       ├── User.kt
│   │   │       └── UserResponse.kt
│   │   └── AndroidManifest.xml
│   └── test/
│       └── java/ink/trmnl/android/buddy/api/
│           ├── TrmnlDeviceApiTest.kt  # Device API tests
│           ├── TrmnlDisplayApiTest.kt # Display API tests
│           └── TrmnlUserApiTest.kt    # User API tests
├── resources/
│   ├── README.md                      # API resources documentation
│   └── trmnl-open-api.yaml           # OpenAPI specification
├── build.gradle.kts
├── proguard-rules.pro
├── consumer-rules.pro
└── README.md
```

## Testing

The module includes comprehensive unit tests using MockWebServer for API integration testing.

Run tests with:
```bash
./gradlew :api:test
```

Test files:
- `TrmnlDeviceApiTest.kt` - Tests for Devices API endpoints
- `TrmnlDisplayApiTest.kt` - Tests for Display API endpoints  
- `TrmnlUserApiTest.kt` - Tests for User API endpoints

## ProGuard

ProGuard rules are included to ensure proper obfuscation while preserving necessary classes for serialization and reflection.

## Contributing

When adding new API endpoints:

1. Define the data models in `models/`
2. Add the endpoint to the appropriate service interface
3. Update this README with usage examples
4. Add unit tests for new functionality

## License

See the root project LICENSE file.
