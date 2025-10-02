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

## Usage

Add the module dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":api"))
}
```

### Example Usage

```kotlin
// Create API client
val trmnlApi = TrmnlApiClient.create()

// Make API calls
val devices = trmnlApi.getDevices()
```

## Features

- Type-safe API calls
- Automatic JSON serialization
- Error handling
- Coroutine support for async operations
- ProGuard rules for release builds

## Module Structure

```
api/
├── src/
│   ├── main/
│   │   ├── java/ink/trmnl/android/buddy/api/
│   │   │   ├── models/      # API request/response models
│   │   │   ├── services/    # Retrofit API interfaces
│   │   │   └── client/      # HTTP client configuration
│   │   └── AndroidManifest.xml
│   └── test/
│       └── java/            # Unit tests
├── build.gradle.kts
├── proguard-rules.pro
└── README.md
```

## Testing

The module includes unit tests for API models and integration tests with MockWebServer.

```bash
./gradlew :api:test
```

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
