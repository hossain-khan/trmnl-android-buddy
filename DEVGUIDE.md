# Developer Guide

**TRMNL Android Buddy** is an Android application for managing and monitoring [TRMNL](https://usetrmnl.com) e-ink display devices. This guide provides comprehensive technical documentation for developers working on the project.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Tech Stack](#tech-stack)
- [Development Setup](#development-setup)
- [Code Organization](#code-organization)
- [Design Patterns](#design-patterns)
- [Testing](#testing)
- [Build and Deployment](#build-and-deployment)
- [Common Tasks](#common-tasks)
- [Troubleshooting](#troubleshooting)

## Architecture Overview

TRMNL Android Buddy follows a modern Android architecture combining several cutting-edge patterns and libraries:

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Circuit Screens & Presenters             │   │
│  │  (Compose UI + Unidirectional Data Flow)         │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                         │
│  ┌─────────────────────────────────────────────────┐   │
│  │         Repositories & Data Sources              │   │
│  │  (UserPreferences, DeviceToken, API)             │   │
│  └─────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                      Data Layer                          │
│  ┌──────────────┐              ┌──────────────────┐    │
│  │  DataStore   │              │   TRMNL API      │    │
│  │ Preferences  │              │   (Retrofit)     │    │
│  └──────────────┘              └──────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Components

1. **Circuit (UI Architecture)**
   - Compose-driven Unidirectional Data Flow (UDF) pattern
   - Screens are split into Presenter (logic) and UI (rendering)
   - Type-safe navigation with BackStack
   - Built-in support for overlays, shared elements, and gesture navigation

2. **Metro (Dependency Injection)**
   - Compile-time DI with KSP code generation
   - Zero runtime overhead
   - Type-safe and refactor-friendly
   - Scoped dependencies (AppScope, ActivityKey, WorkerKey)

3. **Repository Pattern**
   - Abstracts data sources (API, DataStore)
   - Single source of truth for data access
   - Clean separation between data and presentation layers

4. **Material You (Material Design 3)**
   - Dynamic color theming from wallpaper (Android 12+)
   - Light/dark mode support
   - Edge-to-edge display

## Project Structure

```
trmnl-android-buddy/
├── api/                                    # API integration module
│   ├── src/
│   │   ├── main/java/ink/trmnl/android/buddy/api/
│   │   │   ├── TrmnlApiClient.kt          # HTTP client factory
│   │   │   ├── TrmnlApiService.kt         # Retrofit API interface
│   │   │   ├── TrmnlDeviceRepository.kt   # Repository layer
│   │   │   └── models/                     # API models
│   │   │       ├── ApiError.kt
│   │   │       ├── Device.kt
│   │   │       ├── DeviceResponse.kt
│   │   │       ├── DevicesResponse.kt
│   │   │       ├── Display.kt
│   │   │       ├── User.kt
│   │   │       └── UserResponse.kt
│   │   └── test/java/ink/trmnl/android/buddy/api/
│   │       ├── TrmnlDeviceApiTest.kt      # Device API tests
│   │       ├── TrmnlDisplayApiTest.kt     # Display API tests
│   │       └── TrmnlUserApiTest.kt        # User API tests
│   ├── resources/
│   │   ├── README.md                       # API resources docs
│   │   └── trmnl-open-api.yaml            # OpenAPI spec
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── README.md                           # API module docs
│
├── app/                                    # Main application module
│   ├── src/
│   │   ├── main/java/ink/trmnl/android/buddy/
│   │   │   ├── MainActivity.kt            # Single activity
│   │   │   ├── TrmnlBuddyApp.kt          # Application class
│   │   │   │
│   │   │   ├── data/                      # Data layer
│   │   │   │   └── preferences/           # DataStore repositories
│   │   │   │       ├── DeviceTokenRepository.kt
│   │   │   │       ├── UserPreferences.kt
│   │   │   │       └── UserPreferencesRepository.kt
│   │   │   │
│   │   │   ├── di/                        # Dependency injection
│   │   │   │   ├── AppGraph.kt           # Metro DI graph
│   │   │   │   ├── AppBindings.kt        # App-level bindings
│   │   │   │   ├── AppWorkerFactory.kt   # WorkManager factory
│   │   │   │   ├── CircuitProviders.kt   # Circuit providers
│   │   │   │   ├── ComposeAppComponentFactory.kt
│   │   │   │   ├── ActivityKey.kt        # DI keys
│   │   │   │   ├── ApplicationContext.kt
│   │   │   │   └── WorkerKey.kt
│   │   │   │
│   │   │   ├── ui/                        # UI layer (Circuit screens)
│   │   │   │   ├── accesstoken/           # API token setup
│   │   │   │   │   └── AccessTokenScreen.kt
│   │   │   │   ├── devicepreview/         # Device preview
│   │   │   │   │   └── DevicePreviewScreen.kt
│   │   │   │   ├── devices/               # Device list
│   │   │   │   │   └── TrmnlDevicesScreen.kt
│   │   │   │   ├── devicetoken/           # Device token setup
│   │   │   │   │   └── DeviceTokenScreen.kt
│   │   │   │   ├── user/                  # User account
│   │   │   │   │   └── UserAccountScreen.kt
│   │   │   │   ├── welcome/               # Welcome screen
│   │   │   │   │   └── WelcomeScreen.kt
│   │   │   │   ├── sharedelements/        # Shared element transitions
│   │   │   │   ├── theme/                 # Material 3 theme
│   │   │   │   ├── utils/                 # UI utilities
│   │   │   │   └── README.md             # UI screens docs
│   │   │   │
│   │   │   ├── util/                      # Utility classes
│   │   │   │   ├── DeviceIndicatorUtils.kt
│   │   │   │   ├── FormattingUtils.kt
│   │   │   │   └── PrivacyUtils.kt
│   │   │   │
│   │   │   └── work/                      # Background workers
│   │   │       └── SampleWorker.kt
│   │   │
│   │   └── test/                          # Unit tests
│   │
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── README.md (if exists)
│
├── gradle/
│   ├── libs.versions.toml                 # Centralized dependencies
│   └── wrapper/
│
├── keystore/
│   ├── debug.keystore                     # Debug signing key
│   └── README.md
│
├── project-resources/
│   └── screenshots/                        # App screenshots
│
├── .github/
│   ├── copilot-instructions.md            # GitHub Copilot config
│   └── workflows/
│       └── android.yml                     # CI/CD workflow
│
├── build.gradle.kts                        # Root build script
├── settings.gradle.kts                     # Project settings
├── gradle.properties                       # Gradle properties
├── CHANGELOG.md                            # Version history
├── README.md                               # Project overview
├── LICENSE
└── DEVGUIDE.md                            # This file
```

## Tech Stack

### Core Technologies

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **Kotlin** | 2.2.20 | Primary language | [kotlinlang.org](https://kotlinlang.org) |
| **Gradle** | 9.1.0 | Build system | [gradle.org](https://gradle.org) |
| **Android Gradle Plugin** | 8.12.1 | Android build | [developer.android.com](https://developer.android.com/build/releases/gradle-plugin) |

### UI & Architecture

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **Jetpack Compose** | BOM 2025.09.01 | Declarative UI | [developer.android.com/compose](https://developer.android.com/compose) |
| **Material 3** | (via BOM) | Design system | [m3.material.io](https://m3.material.io) |
| **Circuit** | 0.30.0 | UI architecture (UDF) | [slackhq.github.io/circuit](https://slackhq.github.io/circuit) |
| **Metro** | 0.6.8 | Dependency Injection | [zacsweers.github.io/metro](https://zacsweers.github.io/metro) |

### Networking

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **Retrofit** | 3.0.0 | HTTP client | [square.github.io/retrofit](https://square.github.io/retrofit) |
| **OkHttp** | 5.1.0 | HTTP engine | [square.github.io/okhttp](https://square.github.io/okhttp) |
| **kotlinx.serialization** | 1.9.0 | JSON parsing | [github.com/Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) |
| **EitherNet** | 2.0.0 | Type-safe API results | [github.com/slackhq/EitherNet](https://github.com/slackhq/EitherNet) |

### Async & Data

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **Kotlin Coroutines** | 1.10.2 | Async programming | [kotlinlang.org/docs/coroutines](https://kotlinlang.org/docs/coroutines-overview.html) |
| **DataStore** | 1.1.2 | Preferences storage | [developer.android.com/datastore](https://developer.android.com/topic/libraries/architecture/datastore) |
| **WorkManager** | 2.10.5 | Background tasks | [developer.android.com/workmanager](https://developer.android.com/topic/libraries/architecture/workmanager) |

### Image Loading

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **Coil** | 3.3.0 | Image loading | [coil-kt.github.io/coil](https://coil-kt.github.io/coil/) |

### Testing

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **JUnit** | 4.13.2 | Test framework | [junit.org](https://junit.org/junit4/) |
| **AssertK** | 0.28.1 | Fluent assertions | [github.com/assertk-org/assertk](https://github.com/assertk-org/assertk) |
| **MockWebServer** | (via OkHttp) | HTTP mocking | [github.com/square/okhttp](https://github.com/square/okhttp/tree/master/mockwebserver) |
| **Circuit Test** | 0.30.0 | Circuit testing | [slackhq.github.io/circuit/testing](https://slackhq.github.io/circuit/testing/) |

### Code Quality

| Technology | Version | Purpose | Documentation |
|------------|---------|---------|---------------|
| **Kotlinter** | 5.2.0 | Linter/Formatter | [github.com/jeremymailen/kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle) |
| **ktlint** | (via Kotlinter) | Kotlin style guide | [pinterest.github.io/ktlint](https://pinterest.github.io/ktlint/) |

## Development Setup

### Prerequisites

- **JDK 17** or higher
- **Android Studio Ladybug (2025.1.1)** or newer
- **Android SDK** (API 28+, compile SDK 36)
- **Git**

### Initial Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/hossain-khan/trmnl-android-buddy.git
   cd trmnl-android-buddy
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Configure API Key (Optional)**
   
   Create `local.properties` in the project root:
   ```properties
   SERVICE_API_KEY=your_trmnl_api_key_here
   ```
   
   > **Note**: The app can run without this key. Users will be prompted to enter their API token on first launch.

4. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run tests**
   ```bash
   ./gradlew test
   ```

6. **Run the app**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio, or:
   ```bash
   ./gradlew installDebug
   ```

## Code Organization

### Circuit Architecture Pattern

Circuit follows a Unidirectional Data Flow (UDF) pattern. Each screen consists of:

#### 1. Screen Definition
```kotlin
@Parcelize
data object WelcomeScreen : Screen
```

#### 2. State & Events
```kotlin
data class State(
    val isLoading: Boolean = false,
    val error: String? = null,
    val eventSink: (Event) -> Unit
) : CircuitUiState

sealed interface Event : CircuitUiEvent {
    data object GetStartedClicked : Event
    data object ErrorDismissed : Event
}
```

#### 3. Presenter (Business Logic)
```kotlin
@CircuitInject(WelcomeScreen::class, AppScope::class)
@Composable
fun WelcomePresenter(): WelcomeScreen.State {
    var isLoading by remember { mutableStateOf(false) }
    
    return State(
        isLoading = isLoading,
        eventSink = { event ->
            when (event) {
                is Event.GetStartedClicked -> { /* handle event */ }
            }
        }
    )
}
```

#### 4. UI Composable (Rendering)
```kotlin
@CircuitInject(WelcomeScreen::class, AppScope::class)
@Composable
fun WelcomeContent(
    state: WelcomeScreen.State,
    modifier: Modifier = Modifier
) {
    // Render UI based on state
}
```

### Metro Dependency Injection

Metro uses compile-time code generation for zero-runtime overhead DI.

#### Providing Dependencies

**In AppGraph.kt:**
```kotlin
@DependencyGraph(scope = AppScope::class)
@SingleIn(AppScope::class)
interface AppGraph {
    val circuit: Circuit
    
    @Provides
    fun providesSomething(/* params */): SomeType {
        return SomeTypeImpl()
    }
}
```

**In AppBindings.kt:**
```kotlin
@ContributesTo(AppScope::class)
interface AppBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideTrmnlApiService(
        @ApplicationContext context: Context
    ): TrmnlApiService {
        return TrmnlApiClient.create(isDebug = BuildConfig.DEBUG)
    }
}
```

#### Injecting Dependencies

**Constructor Injection:**
```kotlin
@CircuitInject(DevicesScreen::class, AppScope::class)
@AssistedFactory
interface Factory {
    fun create(navigator: Navigator): DevicesPresenter
}

@Inject
class DevicesPresenter(
    private val apiService: TrmnlApiService,
    private val prefsRepo: UserPreferencesRepository,
    @Assisted private val navigator: Navigator
)
```

### Repository Pattern

Repositories abstract data sources and provide a clean API for the presentation layer.

**Example: UserPreferencesRepository**
```kotlin
@Inject
class UserPreferencesRepository(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = 
        context.createDataStore(name = "user_preferences")
    
    val apiToken: Flow<String?> = dataStore.data
        .map { it[API_TOKEN_KEY] }
    
    suspend fun saveApiToken(token: String) {
        dataStore.edit { it[API_TOKEN_KEY] = token }
    }
}
```

### API Integration with EitherNet

EitherNet provides type-safe API error handling with sealed result types.

**API Service Definition:**
```kotlin
interface TrmnlApiService {
    @GET("devices")
    suspend fun getDevices(
        @Header("Authorization") authToken: String
    ): ApiResult<DevicesResponse, ApiError>
}
```

**Handling Results:**
```kotlin
when (val result = apiService.getDevices("Bearer $token")) {
    is ApiResult.Success -> {
        // Handle success: result.value
    }
    is ApiResult.Failure.HttpFailure -> {
        // HTTP errors (4xx, 5xx): result.code, result.error
    }
    is ApiResult.Failure.NetworkFailure -> {
        // Network issues: result.error
    }
    is ApiResult.Failure.ApiFailure -> {
        // API-specific errors: result.error
    }
    is ApiResult.Failure.UnknownFailure -> {
        // Unexpected errors: result.error
    }
}
```

### Material 3 Guidelines

**Theme-Aware Colors** (ALWAYS use MaterialTheme.colorScheme):
```kotlin
// ✅ GOOD
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
)

// ❌ BAD - Never hardcode colors
Card(colors = CardDefaults.cardColors(containerColor = Color.Blue))
```

**Material 3 Components:**
```kotlin
// Use Material 3 components from androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextField
```

## Design Patterns

### 1. Unidirectional Data Flow (UDF)

- **State flows down**: UI receives immutable state from Presenter
- **Events flow up**: UI sends events to Presenter via `eventSink`
- **Single source of truth**: Presenter owns the state

### 2. Repository Pattern

- **Single responsibility**: Each repository handles one data domain
- **Abstraction**: Hides implementation details (API, DataStore)
- **Testability**: Easy to mock for testing

### 3. Dependency Injection

- **Constructor injection**: All dependencies injected via constructor
- **Scoped dependencies**: `@SingleIn(AppScope::class)` for singletons
- **Type-safe**: Compile-time verification

### 4. Separation of Concerns

- **Presentation Layer**: Screens, Presenters, UI Composables
- **Domain Layer**: Repositories, Business Logic
- **Data Layer**: API clients, DataStore, Models

## Testing

### Unit Testing

**Location**: `api/src/test/` and `app/src/test/`

**Test Structure**:
```kotlin
class TrmnlDeviceApiTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TrmnlApiService
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        apiService = TrmnlApiClient.create(
            baseUrl = mockWebServer.url("/").toString()
        )
    }
    
    @Test
    fun `getDevices returns success`() = runTest {
        // Arrange
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"devices": []}""")
        )
        
        // Act
        val result = apiService.getDevices("Bearer token")
        
        // Assert
        assertThat(result).isInstanceOf<ApiResult.Success<*>>()
    }
}
```

### Testing Guidelines

1. **Use AssertK for assertions** (NOT JUnit assertions):
   ```kotlin
   // ✅ GOOD
   import assertk.assertThat
   import assertk.assertions.*
   assertThat(result).isEqualTo("expected")
   
   // ❌ BAD
   assertEquals("expected", result)
   ```

2. **Test Doubles**:
   - **Prefer Fakes**: Lightweight, working implementations (e.g., `FakeNavigator`)
   - **Avoid Mocks**: Unless necessary (require mocking frameworks)

3. **MockWebServer for API Tests**: Simulates HTTP responses

4. **Coroutine Testing**: Use `kotlinx-coroutines-test` with `runTest`

5. **Circuit Testing**: Use `circuit-test` library with `Presenter.test()`

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :api:test
./gradlew :app:test

# Run with coverage
./gradlew test jacocoTestReport
```

## Build and Deployment

### Build Variants

- **Debug**: Development builds with logging enabled
- **Release**: Production builds with ProGuard/R8 minification

### Building APKs

```bash
# Debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Code Formatting and Linting

**IMPORTANT**: Always run before committing:

```bash
# Format Kotlin code (auto-fixes)
./gradlew formatKotlin

# Check formatting (no changes)
./gradlew lintKotlin

# Format specific module
./gradlew :api:formatKotlin
./gradlew :app:formatKotlin
```

### ProGuard/R8

ProGuard rules are configured in:
- `app/proguard-rules.pro` - App-specific rules
- `api/proguard-rules.pro` - API module rules
- `api/consumer-rules.pro` - Rules for API consumers

### Signing Configuration

Debug builds use `keystore/debug.keystore` for consistent signing across development environments. See `keystore/README.md` for details.

## Common Tasks

### Adding a New Screen

1. **Create Screen definition**:
   ```kotlin
   @Parcelize
   data object NewScreen : Screen
   ```

2. **Define State & Events**:
   ```kotlin
   data class State(
       val data: Data? = null,
       val eventSink: (Event) -> Unit
   ) : CircuitUiState
   
   sealed interface Event : CircuitUiEvent {
       data object SomeAction : Event
   }
   ```

3. **Implement Presenter**:
   ```kotlin
   @CircuitInject(NewScreen::class, AppScope::class)
   @Composable
   fun NewPresenter(): NewScreen.State {
       // Business logic
   }
   ```

4. **Implement UI**:
   ```kotlin
   @CircuitInject(NewScreen::class, AppScope::class)
   @Composable
   fun NewContent(state: NewScreen.State, modifier: Modifier = Modifier) {
       // UI rendering
   }
   ```

5. **Navigate to screen**:
   ```kotlin
   navigator.goTo(NewScreen)
   ```

### Adding a New API Endpoint

1. **Define data models** in `api/src/main/java/ink/trmnl/android/buddy/api/models/`:
   ```kotlin
   @Serializable
   data class NewResponse(val data: String)
   ```

2. **Add endpoint** to `TrmnlApiService.kt`:
   ```kotlin
   @GET("endpoint")
   suspend fun getNewData(
       @Header("Authorization") authToken: String
   ): ApiResult<NewResponse, ApiError>
   ```

3. **Add repository method** (optional) in `TrmnlDeviceRepository.kt`:
   ```kotlin
   suspend fun fetchNewData(token: String): ApiResult<NewResponse, ApiError> {
       return apiService.getNewData("Bearer $token")
   }
   ```

4. **Write unit tests** in `api/src/test/`:
   ```kotlin
   @Test
   fun `getNewData returns success`() = runTest {
       // Test implementation
   }
   ```

5. **Update API README** with usage examples

### Adding Dependencies

1. **Update `gradle/libs.versions.toml`**:
   ```toml
   [versions]
   newLib = "1.0.0"
   
   [libraries]
   new-library = { group = "com.example", name = "library", version.ref = "newLib" }
   ```

2. **Add to module's `build.gradle.kts`**:
   ```kotlin
   dependencies {
       implementation(libs.new.library)
   }
   ```

3. **Sync Gradle** and verify build

### Updating CHANGELOG.md

Follow [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format:

```markdown
## [Unreleased]

### Added
- New feature description

### Changed
- Changes to existing functionality

### Fixed
- Bug fix description
```

## Troubleshooting

### Common Issues

#### 1. Gradle Sync Failures

**Problem**: "Plugin [id: '...'] was not found"

**Solution**:
```bash
./gradlew --stop
./gradlew clean
./gradlew build --refresh-dependencies
```

#### 2. KSP Code Generation Issues

**Problem**: "Cannot find Metro generated code"

**Solution**:
```bash
./gradlew clean
./gradlew :app:kspDebugKotlin
```

#### 3. Compose Preview Not Showing

**Problem**: Preview not rendering

**Solution**:
- Ensure `@Preview` or `@PreviewLightDark` annotation
- Check for `@Composable` annotation
- Rebuild project

#### 4. Test Failures

**Problem**: MockWebServer tests failing

**Solution**:
- Check mock response format matches expected JSON
- Verify kotlinx.serialization models
- Ensure `@Before` setup is called

#### 5. Linting Errors

**Problem**: ktlint formatting errors

**Solution**:
```bash
./gradlew formatKotlin
```

### Debug Logging

Enable debug logging in `TrmnlApiClient`:
```kotlin
val isDebug = BuildConfig.DEBUG  // or force true
TrmnlApiClient.create(isDebug = true)
```

### Getting Help

- **Circuit Documentation**: https://slackhq.github.io/circuit/
- **Metro Documentation**: https://zacsweers.github.io/metro/
- **Material 3 Guidelines**: https://m3.material.io/
- **Compose Documentation**: https://developer.android.com/compose
- **Project Issues**: https://github.com/hossain-khan/trmnl-android-buddy/issues

---

**Last Updated**: 2025-10-05
**Document Version**: 1.0.0
