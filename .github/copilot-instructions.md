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

### Material You / Material 3 Guidelines

**All screens and UI components MUST be Material You compatible:**

1. **Use Material 3 Components**:
   - Use `androidx.compose.material3.*` components (NOT `material` or `material2`)
   - Prefer Material 3 equivalents: `Button`, `Card`, `TextField`, `TopAppBar`, etc.
   - Use `ListItem` for list entries with proper leading/trailing content

2. **Theme-Aware Colors**:
   - **NEVER use hardcoded colors** (e.g., `Color(0xFF4CAF50)`, `Color.Red`)
   - Always use `MaterialTheme.colorScheme.*` for colors:
     - `primary`, `onPrimary` - Main brand colors
     - `primaryContainer`, `onPrimaryContainer` - Filled components
     - `secondary`, `tertiary` - Accent colors
     - `error`, `onError` - Error states
     - `surface`, `onSurface` - Backgrounds
     - `surfaceVariant`, `onSurfaceVariant` - Alternative surfaces
   - For status indicators (battery, WiFi), use semantic color scheme tokens with alpha modifiers
   
3. **Dynamic Color Support**:
   - The app uses `dynamicColor = true` for Android 12+ wallpaper-based theming
   - All colors must work in both light and dark themes
   - Test color contrast in both theme modes

4. **Edge-to-Edge Display**:
   - Use `Modifier.padding(innerPadding)` with `Scaffold` to respect system bars
   - Status and navigation bars are transparent (configured in XML themes)
   - MainActivity already enables edge-to-edge via `enableEdgeToEdge()`

5. **Typography**:
   - Use `MaterialTheme.typography.*` for all text
   - Available styles: `displayLarge/Medium/Small`, `headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small`

**Example - Good Practice**:
```kotlin
@Composable
fun GoodExample() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            text = "Hello",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
```

**Example - Bad Practice** ❌:
```kotlin
// DON'T DO THIS
Card(colors = CardDefaults.cardColors(containerColor = Color.Blue)) {
    Text(text = "Hello", color = Color.White)
}
```

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
- **Assertions**: Always use [AssertK](https://github.com/assertk-org/assertk) for all test assertions
  - **NEVER use JUnit assertions** (`assertEquals`, `assertTrue`, `assertNotNull`, etc.)
  - Use assertk's fluent API: `assertThat(actual).isEqualTo(expected)`
  - Common assertions: `isEqualTo()`, `isNotNull()`, `isTrue()`, `isFalse()`, `hasSize()`, `isEmpty()`, `isInstanceOf()`, `isCloseTo()`
  - Benefits: Kotlin-native, better error messages, type-safe, null-safe
  - Example:
    ```kotlin
    import assertk.assertThat
    import assertk.assertions.*
    
    @Test
    fun `test example`() {
        val result = someFunction()
        assertThat(result).isEqualTo("expected")
        assertThat(result).hasLength(8)
    }
    ```
- **Test Doubles**: Use fakes instead of mocks when possible
  - **Fakes** are preferred: lightweight, working implementations suitable for tests (e.g., in-memory database, `FakeNavigator`)
  - **Mocks** should be avoided unless necessary: require mocking frameworks and add complexity
  - See [Android Test Doubles Guide](https://developer.android.com/training/testing/fundamentals/test-doubles) for detailed explanations
- **MockWebServer**: Use for testing Retrofit services (simulates HTTP server responses)
- **Coroutine Testing**: Use `kotlinx-coroutines-test` with `runTest`
- **Circuit Testing**: Use `circuit-test` library with `FakeNavigator` and `Presenter.test()` helpers
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

### Changelog Maintenance

**REQUIRED**: Always update `CHANGELOG.md` when making changes following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) guidelines:

1. **Format**: Follow Keep a Changelog format
2. **Versioning**: Use [Semantic Versioning](https://semver.org/spec/v2.0.0.html) (MAJOR.MINOR.PATCH)
   - MAJOR: Incompatible API changes
   - MINOR: Add functionality in a backward compatible manner
   - PATCH: Backward compatible bug fixes
3. **Sections**: Use appropriate change types:
   - `Added` for new features
   - `Changed` for changes in existing functionality
   - `Deprecated` for soon-to-be removed features
   - `Removed` for now removed features
   - `Fixed` for any bug fixes
   - `Security` in case of vulnerabilities
4. **Unreleased Section**: Add all changes to `[Unreleased]` section first
5. **Release Process**: When releasing, move `[Unreleased]` changes to a new version section with date
6. **Format Example**:
   ```markdown
   ## [Unreleased]
   
   ### Added
   - New feature description
   
   ### Fixed
   - Bug fix description
   
   ## [1.0.1] - 2025-10-03
   
   ### Fixed
   - Previous bug fix
   ```
7. **Guidelines**:
   - Write for humans, not machines
   - Each version should have an entry
   - Group similar types of changes together
   - Use ISO 8601 date format (YYYY-MM-DD)
   - Link versions at bottom of file
   - Keep entries concise but descriptive
   - Don't dump git commit logs

**Example Workflow**:
```bash
# 1. Make code changes
# 2. Update CHANGELOG.md under [Unreleased] section
# 3. Format code
./gradlew formatKotlin
# 4. Run tests
./gradlew test
# 5. Commit with descriptive message
git commit -m "Add feature X

- Updated CHANGELOG.md with new feature"
```

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

### Release Process

**IMPORTANT**: The `main` branch is protected. All changes must be made via pull requests.

Follow this workflow for creating a new release:

1. **Create Release Branch**:
   ```bash
   git checkout main
   git pull
   git checkout -b release/X.Y.Z
   ```

2. **Update Version Numbers**:
   - Update `versionCode` and `versionName` in `app/build.gradle.kts`
   - Example: `versionCode = 4` and `versionName = "1.0.3"`

3. **Update CHANGELOG.md**:
   - Move all `[Unreleased]` changes to new version section `[X.Y.Z] - YYYY-MM-DD`
   - Add empty `[Unreleased]` section at top
   - Update version comparison links at bottom:
     ```markdown
     [unreleased]: https://github.com/hossain-khan/trmnl-android-buddy/compare/X.Y.Z...HEAD
     [X.Y.Z]: https://github.com/hossain-khan/trmnl-android-buddy/compare/X.Y.Z-1...X.Y.Z
     ```

4. **Commit and Push Release Branch**:
   ```bash
   git add app/build.gradle.kts CHANGELOG.md
   git commit -m "chore: Prepare release X.Y.Z"
   git push -u origin release/X.Y.Z
   ```

5. **Create Release Pull Request**:
   - Create PR from `release/X.Y.Z` to `main`
   - Title: "Release X.Y.Z"
   - Include changelog summary in PR description
   - Request review and merge

6. **Create and Push Tag** (after PR is merged):
   ```bash
   git checkout main
   git pull
   git tag -a X.Y.Z -m "Release X.Y.Z - Brief Description

   - Major change 1
   - Major change 2
   - Major change 3"
   git push origin X.Y.Z
   ```

7. **Create GitHub Release**:
   - Go to GitHub Releases page
   - Click "Draft a new release"
   - Select tag `X.Y.Z`
   - Title: "Release X.Y.Z"
   - Copy relevant section from CHANGELOG.md
   - Publish release

**Version Numbering** (Semantic Versioning):
- `MAJOR.MINOR.PATCH` (e.g., 1.0.3)
- MAJOR: Breaking changes or major new features
- MINOR: New features, backward compatible
- PATCH: Bug fixes, backward compatible

**Tag Format**: Use plain version number (e.g., `1.0.3`), not `v1.0.3`

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
  - AssertK: 0.28.1 (testing assertions)

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
- [Circuit Testing Guide](https://slackhq.github.io/circuit/testing/)
- [Metro Documentation](https://zacsweers.github.io/metro/)
- [EitherNet Repository](https://github.com/slackhq/EitherNet)
- [AssertK Documentation](https://github.com/assertk-org/assertk)
- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material 3 Design System](https://m3.material.io/)
- [Material 3 Compose Components](https://developer.android.com/jetpack/compose/designsystems/material3)
- [Android Dynamic Color Guide](https://developer.android.com/develop/ui/views/theming/dynamic-colors)
- [Android Test Doubles Guide](https://developer.android.com/training/testing/fundamentals/test-doubles)
- [TRMNL API Documentation](https://usetrmnl.com/api)

## Notes for AI Assistants

- Always suggest running `formatKotlin` and `test` before commits
- **Always update CHANGELOG.md** following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format
- Add changes to `[Unreleased]` section with appropriate category (Added, Changed, Deprecated, Removed, Fixed, Security)
- Use [Semantic Versioning](https://semver.org/) for version numbers (MAJOR.MINOR.PATCH)
- Use EitherNet's `ApiResult` for API responses, not custom Result types
- Prefer constructor injection over field injection
- Write comprehensive unit tests for new API endpoints
- Follow the existing code structure and patterns
- Don't use PII (Personally Identifiable Information) in code examples or tests
- **Do NOT create summary markdown files** (like `FEATURE_SUMMARY.md`, `SCREENS_IMPLEMENTATION.md`, etc.) for features or bug fixes
- Keep documentation in existing files like README.md, CHANGELOG.md, or inline code comments
