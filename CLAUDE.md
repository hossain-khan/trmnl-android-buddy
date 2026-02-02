# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**TRMNL Android Buddy** is an Android companion app for managing [TRMNL](https://trmnl.com) e-ink display devices. The app uses modern Android architecture patterns with Circuit (Compose-driven UDF), Metro (DI), and Material You design.

## Tech Stack

- **Language**: Kotlin 2.2.20
- **Architecture**: [Circuit](https://github.com/slackhq/circuit) - Compose-driven unidirectional data flow
- **Dependency Injection**: [Metro](https://zacsweers.github.io/metro/) with KSP code generation
- **UI**: Jetpack Compose with Material Design 3 / Material You (dynamic color)
- **Networking**: Retrofit 3.0.0 + OkHttp 5.1.0 + kotlinx.serialization + [EitherNet](https://github.com/slackhq/EitherNet) 2.0.0
- **Build**: Gradle 9.1.0 with Kotlin DSL, Version Catalog (`gradle/libs.versions.toml`)
- **Code Quality**: Kotlinter (ktlint wrapper)
- **Testing**: JUnit, AssertK, MockWebServer, Circuit Test, Turbine, Robolectric

## Module Structure

```
trmnl-android-buddy/
├── app/              # Main Android application with UI screens, data layer, DI setup
├── api/              # TRMNL API client (Retrofit service, repositories, models)
└── content/          # RSS content feed integration module
```

## Common Commands

### Before Every Commit
```bash
# 1. Format Kotlin code (auto-fixes style issues)
./gradlew formatKotlin

# 2. Run all tests
./gradlew test

# 3. Ensure clean build
./gradlew assembleDebug
```

### Testing Commands
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :api:test
./gradlew :app:test

# Run single test class
./gradlew :api:test --tests TrmnlApiServiceTest

# Run with coverage report (Kover)
./gradlew koverHtmlReport
```

### Code Quality
```bash
# Check formatting (doesn't modify files)
./gradlew lintKotlin

# Format all Kotlin files
./gradlew formatKotlin

# Format specific module
./gradlew :app:formatKotlin
```

### Build Commands
```bash
# Clean build
./gradlew clean build

# Debug APK
./gradlew assembleDebug

# Release APK (uses keystore config)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Architecture Patterns

### Circuit UDF (Unidirectional Data Flow)

Circuit screens consist of two parts:
1. **Presenter** - Handles business logic and state management
2. **UI (Content)** - Renders the UI based on state

Both use `@CircuitInject` annotation for Metro integration:

```kotlin
// Presenter
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomePresenter(): HomeScreen.State {
    // Business logic, state management
    // Events flow up, state flows down
}

// UI
@CircuitInject(HomeScreen::class, AppScope::class)
@Composable
fun HomeContent(state: HomeScreen.State, modifier: Modifier = Modifier) {
    // UI composition using state
}
```

### Metro Dependency Injection

- Use `@ContributesBinding` for interface implementations
- Use `@Inject` constructor injection (preferred over field injection)
- Scopes: `@ApplicationContext`, `@ActivityKey`, `@WorkerKey`
- Circuit integration: `@CircuitInject` generates factories automatically

### Repository Pattern

Data layer uses repositories to abstract data sources. API calls return `ApiResult<T, E>` from EitherNet:

```kotlin
when (val result = apiService.getDevices(token)) {
    is ApiResult.Success -> // Handle success data
    is ApiResult.Failure.HttpFailure -> // HTTP errors (4xx, 5xx)
    is ApiResult.Failure.NetworkFailure -> // Network issues
    is ApiResult.Failure.ApiFailure -> // API-specific errors
    is ApiResult.Failure.UnknownFailure -> // Unexpected errors
}
```

## Material You / Material 3 Guidelines

**CRITICAL**: All UI components must be Material You compatible.

### Rules

1. **Use Material 3 Components**: `androidx.compose.material3.*` (NOT `material` or `material2`)
2. **NEVER hardcode colors**: Always use `MaterialTheme.colorScheme.*`
   - `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`
   - `secondary`, `tertiary`, `error`, `surface`, `surfaceVariant`
3. **Dynamic Color**: App supports Android 12+ wallpaper-based theming
4. **Typography**: Use `MaterialTheme.typography.*` for all text
5. **Edge-to-Edge**: Use `Modifier.padding(innerPadding)` with `Scaffold`

### Example - Good Practice ✅
```kotlin
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
```

### Example - Bad Practice ❌
```kotlin
// NEVER DO THIS
Card(colors = CardDefaults.cardColors(containerColor = Color.Blue)) {
    Text(text = "Hello", color = Color.White)
}
```

## Testing Guidelines

### Test Assertions
- **ALWAYS use AssertK** for all assertions (never JUnit assertions)
- Import: `import assertk.assertThat` and `import assertk.assertions.*`

```kotlin
// ✅ Good - AssertK
assertThat(result).isEqualTo("expected")
assertThat(list).hasSize(3)

// ❌ Bad - JUnit
assertEquals("expected", result)
assertEquals(3, list.size)
```

### Test Doubles
- **Prefer fakes** over mocks (lightweight, working implementations)
- **Use mocks** only when necessary (require mocking frameworks)
- **MockWebServer** for testing Retrofit services
- **FakeNavigator** from `circuit-test` for Circuit screens

### Required Tests
- Unit tests for all API services and repositories
- Cover success cases, error cases, and edge cases
- Use `runTest` for coroutine testing
- Use `Presenter.test()` helpers from Circuit Test library

## TRMNL API Integration

### Authentication
- **Account API**: `Authorization: Bearer user_xxxxxx` (user account token)
- **Device API**: `Access-Token: abc-123` (device-specific token)

### Base URL
`https://trmnl.com/api`

### Key Endpoints
- `GET /devices` - List all user devices
- `GET /devices/{id}` - Get device details
- `GET /display/current` - Get device display (requires device token)
- `GET /me` - Get user info
- `GET https://trmnl.com/recipes.json` - List recipes (public, no auth)

See `api/src/main/java/ink/trmnl/android/buddy/api/TrmnlApiService.kt` for full API documentation.

## Changelog Maintenance

**REQUIRED**: Always update `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format.

### Process
1. Add changes to `[Unreleased]` section
2. Use appropriate category: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`
3. **CRITICAL**: Check if section header already exists before adding duplicate headers
4. Use [Semantic Versioning](https://semver.org/) (MAJOR.MINOR.PATCH)

### Example
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

## Release Process

**IMPORTANT**: `main` branch is protected. All changes via pull requests.

1. Create release branch: `git checkout -b release/X.Y.Z`
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`
3. Move `[Unreleased]` changes in `CHANGELOG.md` to `[X.Y.Z] - YYYY-MM-DD`
4. Commit: `git commit -m "chore: Prepare release X.Y.Z"`
5. Create PR from `release/X.Y.Z` to `main`
6. After merge, create tag: `git tag -a X.Y.Z -m "Release X.Y.Z"`
7. Push tag: `git push origin X.Y.Z`
8. Create GitHub Release with changelog

**Version Numbering**: Default to MINOR increment (e.g., 1.2.0 → 1.3.0). Use PATCH only for hotfixes.

## Adding New Features

### New Circuit Screen
1. Create screen state interface in `ui/<feature>/<Feature>Screen.kt`
2. Create presenter in `ui/<feature>/<Feature>Presenter.kt` with `@CircuitInject`
3. Create UI in `ui/<feature>/<Feature>Content.kt` with `@CircuitInject`
4. Both presenter and UI must use same `Screen` class and `AppScope`
5. Write tests using `circuit-test` library with `Presenter.test()` and `FakeNavigator`

### New API Endpoint
1. Add method to `api/src/main/java/.../TrmnlApiService.kt`
2. Create/update models in `api/src/main/java/.../models/`
3. Add repository method (if needed)
4. Write unit tests with MockWebServer in `api/src/test/`
5. Use AssertK for all assertions

### New Compose Component
1. Use Material 3 components only
2. Follow Material You color guidelines (no hardcoded colors)
3. Add `@PreviewLightDark` annotation for Compose previews
4. Test in both light and dark themes

## Dependencies

All versions in `gradle/libs.versions.toml`. Major dependencies:
- Kotlin: 2.2.20
- Circuit: 0.30.0
- Metro: 0.6.8
- Compose BOM: 2025.09.01
- Retrofit: 3.0.0
- EitherNet: 2.0.0
- AssertK: 0.28.1

When updating dependencies, check official release pages and test thoroughly.

## Common Patterns

### Error Handling with EitherNet
```kotlin
suspend fun fetchDevices(): Result<List<Device>> {
    return when (val result = apiService.getDevices(token)) {
        is ApiResult.Success -> Result.success(result.value.data)
        is ApiResult.Failure -> Result.failure(Exception(result.toString()))
    }
}
```

### Metro Constructor Injection
```kotlin
class DeviceRepository @Inject constructor(
    private val apiService: TrmnlApiService,
    private val userPrefs: UserPreferencesRepository,
) {
    // Implementation
}
```

### Circuit State Management
```kotlin
@CircuitInject(MyScreen::class, AppScope::class)
@Composable
fun MyPresenter(): MyScreen.State {
    var isLoading by remember { mutableStateOf(false) }

    return MyScreen.State(
        isLoading = isLoading,
        eventSink = { event ->
            when (event) {
                is MyScreen.Event.ButtonClicked -> {
                    // Handle event
                }
            }
        }
    )
}
```

## Important Notes

- Never create summary markdown files (FEATURE_SUMMARY.md, etc.)
- Keep documentation in README.md, CHANGELOG.md, or inline comments
- Always run `formatKotlin` and `test` before commits
- Use EitherNet's `ApiResult` (not custom Result types)
- Write comprehensive tests for new API endpoints
- Follow existing code structure and patterns
- Avoid PII in code examples or tests

## Resources

- [Circuit Documentation](https://slackhq.github.io/circuit/)
- [Circuit Testing Guide](https://slackhq.github.io/circuit/testing/)
- [Metro Documentation](https://zacsweers.github.io/metro/)
- [EitherNet Repository](https://github.com/slackhq/EitherNet)
- [AssertK Documentation](https://github.com/assertk-org/assertk)
- [Material 3 Compose](https://developer.android.com/jetpack/compose/designsystems/material3)
- [TRMNL API Documentation](https://trmnl.com/api-docs/index.html)
