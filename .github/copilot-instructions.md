# GitHub Copilot Instructions for TRMNL Android Buddy

## Project Overview

**TRMNL Android Buddy** is an Android application for managing and monitoring [TRMNL](https://usetrmnl.com) e-ink display devices. The app allows users to view device status, battery levels, WiFi strength, and manage their TRMNL devices.

### Tech Stack

- **Language**: Kotlin 2.2.20
- **Architecture**: [Circuit](https://github.com/slackhq/circuit) - Compose-driven UDF architecture
- **Dependency Injection**: [Metro](https://zacsweers.github.io/metro/) with KSP
- **UI Framework**: Jetpack Compose with Material Design 3
- **Networking**: 
  - Retrofit 3.0.0 (HTTP client)
  - OkHttp 5.1.0 (HTTP engine)
  - kotlinx.serialization (JSON parsing)
  - [EitherNet](https://github.com/slackhq/EitherNet) 2.0.0 (Type-safe API results)
- **Async**: Kotlinx Coroutines 1.10.2
- **Build System**: Gradle 9.1.0 with Kotlin DSL
- **Code Quality**: Kotlinter (ktlint wrapper) for formatting and linting

### Project Structure

```
trmnl-android-buddy/
├── app/                          # Main Android application module
│   └── src/main/java/app/example/
│       ├── CircuitApp.kt         # Application entry point
│       ├── MainActivity.kt       # Single activity
│       ├── circuit/              # Circuit screens and presenters
│       ├── data/                 # Repositories and data sources
│       ├── di/                   # Metro dependency injection
│       └── ui/theme/             # Compose theme configuration
├── api/                          # TRMNL API integration module
│   └── src/
│       ├── main/java/ink/trmnl/android/buddy/api/
│       │   ├── TrmnlApiClient.kt       # API client factory
│       │   ├── TrmnlApiService.kt      # Retrofit interface
│       │   ├── TrmnlDeviceRepository.kt # Repository layer
│       │   └── models/                  # API models (Device, DevicesResponse, etc.)
│       └── test/                        # Unit tests with MockWebServer
└── gradle/
    └── libs.versions.toml        # Centralized dependency versions
```

## Coding Guidelines

### Architecture Patterns

1. **Circuit UDF (Unidirectional Data Flow)**:
   - Use `@CircuitInject` annotation for screens and presenters
   - Screens are composable functions that render UI
   - Presenters handle business logic and state management
   - Events flow up, state flows down

2. **Metro Dependency Injection**:
   - Use `@ContributesBinding` for interface implementations
   - Use `@Inject` constructor injection
   - Scopes: `@ApplicationContext`, `@ActivityKey`, `@WorkerKey`

3. **Repository Pattern**:
   - Data layer uses repositories to abstract data sources
   - API calls return `ApiResult<T, E>` from EitherNet (sealed type-safe results)
   - Handle all result types: `Success`, `HttpFailure`, `NetworkFailure`, `ApiFailure`, `UnknownFailure`

### Code Style

- **Kotlin Style Guide**: Follow [official Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- **Formatting**: Enforced by Kotlinter plugin (ktlint)
- **Naming**:
  - Classes: PascalCase
  - Functions/Properties: camelCase
  - Constants: SCREAMING_SNAKE_CASE
  - Composables: PascalCase (like classes)

### API Integration

When working with TRMNL API:

1. **Base URL**: `https://usetrmnl.com/api`
2. **Authentication**: Bearer token in `Authorization` header
3. **Response Handling**: Use EitherNet's `ApiResult<T, E>`
   ```kotlin
   when (val result = apiService.getDevices(token)) {
       is ApiResult.Success -> // Handle success
       is ApiResult.Failure.HttpFailure -> // Handle HTTP errors (4xx, 5xx)
       is ApiResult.Failure.NetworkFailure -> // Handle network issues
       is ApiResult.Failure.ApiFailure -> // Handle API-specific errors
       is ApiResult.Failure.UnknownFailure -> // Handle unexpected errors
   }
   ```
4. **Models**: Use `@Serializable` with kotlinx.serialization
5. **Testing**: Write unit tests with MockWebServer for all API endpoints

### Testing Guidelines

- **Unit Tests**: Required for all API services and repositories
- **MockWebServer**: Use for testing Retrofit services
- **Coroutine Testing**: Use `kotlinx-coroutines-test` with `runTest`
- **Test Coverage**: Aim for success cases, error cases, and edge cases

## Development Workflow

### Before Committing

**IMPORTANT**: Always run these commands before making a commit:

```bash
# 1. Format Kotlin code (auto-fixes style issues)
./gradlew formatKotlin

# 2. Run all tests (ensures nothing is broken)
./gradlew test
```

If either command fails, fix the issues before committing.

### Common Gradle Tasks

```bash
# Build the project
./gradlew build

# Run specific module tests
./gradlew :api:test
./gradlew :app:test

# Check code formatting (doesn't modify files)
./gradlew lintKotlin

# Clean build
./gradlew clean build

# Assemble debug APK
./gradlew assembleDebug

# Run app on connected device
./gradlew installDebug
```

### Kotlinter Commands

```bash
# Format all Kotlin files
./gradlew formatKotlin

# Check formatting without changing files
./gradlew lintKotlin

# Format specific module
./gradlew :api:formatKotlin
./gradlew :app:formatKotlin
```

## API Endpoints (TRMNL)

### Available Endpoints

1. **GET /devices** - List all user devices
   - Returns: `DevicesResponse` with `List<Device>`
   - Auth: Required (Bearer token)

2. **GET /devices/{id}** - Get single device
   - Returns: `DeviceResponse` with `Device`
   - Auth: Required (Bearer token)

### Device Model

```kotlin
data class Device(
    val id: Int,
    val name: String,
    val friendlyId: String,
    val macAddress: String,
    val batteryVoltage: Double?,      // Nullable
    val rssi: Int?,                   // WiFi signal in dBm, nullable
    val percentCharged: Double,       // 0.0 to 100.0
    val wifiStrength: Double          // 0.0 to 100.0
)
```

## Dependencies Management

All dependency versions are centralized in `gradle/libs.versions.toml`:

- **Major Dependencies**:
  - Kotlin: 2.2.20
  - Circuit: 0.30.0
  - Metro: 0.6.8
  - Compose BOM: 2025.09.01
  - Retrofit: 3.0.0
  - OkHttp: 5.1.0
  - EitherNet: 2.0.0

When suggesting dependency updates:
1. Check the official release page (links in `libs.versions.toml`)
2. Consider breaking changes
3. Test thoroughly after updates

## Common Patterns

### Creating a new Circuit Screen

```kotlin
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomePresenter(): HomeScreen.State {
    // Presenter logic
}

@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomeContent(state: HomeScreen.State, modifier: Modifier = Modifier) {
    // UI composition
}
```

### Adding a new API endpoint

1. Add method to `TrmnlApiService.kt`
2. Create/update models in `api/models/`
3. Add repository method in `TrmnlDeviceRepository.kt`
4. Write unit tests in `TrmnlApiServiceTest.kt`

### Error Handling

```kotlin
suspend fun fetchDevices(): Result<List<Device>> {
    return when (val result = apiService.getDevices(token)) {
        is ApiResult.Success -> Result.success(result.value.data)
        is ApiResult.Failure -> Result.failure(
            Exception(result.toString())
        )
    }
}
```

## Resources

- [Circuit Documentation](https://slackhq.github.io/circuit/)
- [Metro Documentation](https://zacsweers.github.io/metro/)
- [EitherNet Repository](https://github.com/slackhq/EitherNet)
- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [TRMNL API Documentation](https://usetrmnl.com/api)

## Notes for AI Assistants

- Always suggest running `formatKotlin` and `test` before commits
- Use EitherNet's `ApiResult` for API responses, not custom Result types
- Prefer constructor injection over field injection
- Write comprehensive unit tests for new API endpoints
- Follow the existing code structure and patterns
- Don't use PII (Personally Identifiable Information) in code examples or tests
