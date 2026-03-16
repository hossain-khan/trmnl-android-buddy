# Testing Guide

This guide is designed for developers working on the **TRMNL Android Buddy** Android app. It documents the testing stack, conventions, and practical patterns for writing reliable unit tests across all modules. Whether you're adding a new presenter, repository, or WorkManager worker, this guide shows you which tools to use, how to structure your tests, and which fake implementations are available to use.

> **Note:** All file paths in this document are relative to the repository root. Links to source files work when viewing this document on GitHub.

## Table of Contents

1. [Testing Stack](#testing-stack)
2. [Project Test Structure](#project-test-structure)
3. [Assertions: AssertK](#assertions-assertk)
4. [Presenter Testing with Circuit](#presenter-testing-with-circuit)
5. [Repository Testing with Fakes](#repository-testing-with-fakes)
6. [Worker Testing with MockWebServer](#worker-testing-with-mockwebserver)
7. [Flow Testing with Turbine](#flow-testing-with-turbine)
8. [Test Doubles: Fakes vs Mocks](#test-doubles-fakes-vs-mocks)
9. [Available Fake Implementations](#available-fake-implementations)
10. [Code Coverage with Kover](#code-coverage-with-kover)
11. [Running Tests](#running-tests)

---

## Testing Stack

| Tool | Purpose | Documentation |
|------|---------|---------------|
| **JUnit 4** | Test runner and lifecycle | Standard |
| **Robolectric** | Android framework emulation for unit tests | [docs](http://robolectric.org/) |
| **AssertK** | Kotlin-native fluent assertions | [docs](https://github.com/assertk-org/assertk) |
| **Circuit Test** | Presenter testing with `FakeNavigator` | [docs](https://slackhq.github.io/circuit/testing/) |
| **Turbine** | Flow assertion library | [docs](https://github.com/cashapp/turbine) |
| **MockWebServer** | Simulate HTTP API responses | [docs](https://square.github.io/okhttp/5.x/mockwebserver/) |
| **Kotlinx Coroutines Test** | Coroutine and `runTest` support | [docs](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) |
| **AndroidX WorkManager Testing** | `TestListenableWorkerBuilder` for workers | [docs](https://developer.android.com/reference/androidx/work/testing/package-summary) |
| **Kover** | Kotlin code coverage reporting | [docs](https://github.com/Kotlin/kotlinx-kover) |

---

## Project Test Structure

```
app/src/test/java/ink/trmnl/android/buddy/
├── content/
│   └── db/
│       ├── FakeAnnouncementDao.kt         # Fake DAO for announcement data
│       └── FakeBlogPostDao.kt             # Fake DAO for blog post data
├── data/
│   ├── BookmarkRepositoryTest.kt          # Tests for bookmark repository
│   ├── FakeBookmarkRepository.kt          # Fake bookmark repository
│   ├── PlaylistItemsRepositoryTest.kt     # Tests for playlist items repository
│   ├── RecipesRepositoryTest.kt           # Tests for recipes repository
│   ├── battery/
│   │   └── BatteryHistoryAnalyzerTest.kt  # Tests for battery analytics
│   ├── database/
│   │   ├── BatteryHistoryRepositoryTest.kt # Tests for battery history DAO/repo
│   │   ├── BookmarkedRecipeDaoTest.kt      # Tests for bookmarked recipe DAO
│   │   ├── FakeBatteryHistoryDao.kt        # Fake battery history DAO
│   │   └── FakeBookmarkedRecipeDao.kt      # Fake bookmarked recipe DAO
│   └── preferences/
│       ├── DeviceTokenRepositoryTest.kt    # Tests for device token repo
│       └── UserPreferencesRepositoryTest.kt # Tests for user preferences repo
├── dev/
│   └── DevelopmentPresenterTest.kt         # Tests for dev tools presenter
├── fakes/
│   ├── FakeBatteryHistoryRepository.kt     # Fake battery history repository
│   ├── FakeDeviceTokenRepository.kt        # Fake device token repository
│   ├── FakePlaylistItemsRepository.kt      # Fake playlist items repository
│   ├── FakeTrmnlApiService.kt              # Fake TRMNL API service
│   └── FakeUserPreferencesRepository.kt   # Fake user preferences repository
├── security/
│   └── FakeBiometricAuthHelper.kt          # Fake biometric auth helper
├── ui/
│   ├── accesstoken/
│   │   └── AccessTokenScreenTest.kt       # Tests for access token screen
│   ├── announcements/
│   │   └── AnnouncementsScreenTest.kt     # Tests for announcements screen
│   ├── auth/
│   │   └── AuthenticationScreenTest.kt    # Tests for authentication screen
│   ├── blogposts/
│   │   └── BlogPostsScreenTest.kt         # Tests for blog posts screen
│   ├── bookmarkedrecipes/
│   │   └── BookmarkedRecipesPresenterTest.kt # Tests for bookmarked recipes
│   ├── contenthub/
│   │   └── ContentHubScreenTest.kt        # Tests for content hub screen
│   ├── devicecatalog/
│   │   ├── ColorCapabilityTest.kt         # Tests for device color capabilities
│   │   └── DeviceCatalogPresenterTest.kt  # Tests for device catalog presenter
│   ├── devicedetail/
│   │   └── DeviceDetailScreenTest.kt      # Tests for device detail presenter
│   ├── devicepreview/
│   │   └── DevicePreviewScreenTest.kt     # Tests for device preview screen
│   ├── devices/
│   │   └── TrmnlDevicesScreenTest.kt      # Tests for main devices screen
│   ├── devicetoken/
│   │   └── DeviceTokenScreenTest.kt       # Tests for device token screen
│   ├── playlistitems/
│   │   └── PlaylistItemsPresenterTest.kt  # Tests for playlist items presenter
│   ├── recipescatalog/
│   │   └── RecipesCatalogPresenterTest.kt # Tests for recipes catalog presenter
│   ├── settings/
│   │   └── SettingsScreenTest.kt          # Tests for settings screen
│   ├── user/
│   │   └── UserAccountScreenTest.kt       # Tests for user account screen
│   ├── utils/
│   │   ├── ColorUtilsTest.kt              # Tests for color utilities
│   │   └── DeviceIndicatorUtilsTest.kt    # Tests for device indicator utils
│   └── welcome/
│       └── WelcomeScreenTest.kt           # Tests for welcome/onboarding screen
├── util/
│   ├── FormattingUtilsTest.kt             # Tests for formatting utilities
│   ├── GravatarUtilsTest.kt               # Tests for Gravatar utilities
│   ├── ImageDownloadUtilsTest.kt          # Tests for image download utils
│   └── PrivacyUtilsTest.kt               # Tests for privacy utilities
└── work/
    ├── AnnouncementSyncWorkerTest.kt      # Tests for announcement sync worker
    ├── BatteryCollectionWorkerTest.kt     # Tests for battery collection worker
    ├── BlogPostSyncWorkerTest.kt          # Tests for blog post sync worker
    ├── LowBatteryNotificationWorkerTest.kt # Tests for low battery notif worker
    ├── TestBatteryCollectionWorkerFactory.kt # Factory for worker tests
    ├── WorkManagerObserverTest.kt         # Tests for WorkManager observer
    └── WorkerSchedulerTest.kt             # Tests for worker scheduling
```

---

## Assertions: AssertK

**Always use AssertK for all assertions.** Never use JUnit assertions (`assertEquals`, `assertTrue`, `assertNotNull`, etc.).

AssertK provides a fluent, Kotlin-native API with better error messages and type safety.

### Imports

```kotlin
import assertk.assertThat
import assertk.assertions.*
```

### Common Assertions

```kotlin
// Equality
assertThat(actual).isEqualTo(expected)
assertThat(actual).isNotEqualTo(unexpected)

// Nullability
assertThat(value).isNull()
assertThat(value).isNotNull()

// Boolean
assertThat(flag).isTrue()
assertThat(flag).isFalse()

// Collections
assertThat(list).hasSize(3)
assertThat(list).isEmpty()
assertThat(list).isNotEmpty()
assertThat(list).contains("item")
assertThat(list).containsAll("a", "b")

// Types (use for sealed class checks)
assertThat(screen).isInstanceOf(DeviceDetailScreen::class)

// Numeric
assertThat(value).isGreaterThan(0)
assertThat(value).isLessThan(100)
assertThat(value).isCloseTo(expected, within = 0.001)
```

### Bad Pattern (Never Do This)

```kotlin
// ❌ NEVER use JUnit assertions
assertEquals("expected", actual)
assertTrue(flag)
assertNotNull(value)
assertEquals(3, list.size)
```

---

## Presenter Testing with Circuit

Circuit presenters are the core of the app's business logic. The `circuit-test` library provides the `test` extension function and `FakeNavigator` for comprehensive testing.

### Test Class Setup

Presenter tests using Android-specific APIs require Robolectric:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyPresenterTest {
    // tests here
}
```

Pure presenter tests (no Android context) don't need Robolectric:

```kotlin
class MyPresenterTest {
    // tests here
}
```

### Basic Presenter Test Pattern

```kotlin
@Test
fun `presenter loads data on initial composition`() = runTest {
    // 1. Create FakeNavigator for the screen
    val navigator = FakeNavigator(MyScreen)

    // 2. Create fake dependencies
    val fakeRepo = FakeMyRepository()

    // 3. Instantiate the presenter directly
    val presenter = MyPresenter(navigator, fakeRepo)

    // 4. Use presenter.test {} to observe emitted states
    presenter.test {
        // Poll states until desired state is reached
        var state: MyScreen.State
        do {
            state = awaitItem()
        } while (state.isLoading)

        // Assert final state
        assertThat(state.items).hasSize(2)
        assertThat(state.errorMessage).isNull()

        cancelAndIgnoreRemainingEvents()
    }
}
```

### Navigation Testing

```kotlin
@Test
fun `navigates to detail screen on item clicked`() = runTest {
    val navigator = FakeNavigator(MyScreen)
    val presenter = MyPresenter(navigator, fakeRepo)

    presenter.test {
        var loadedState: MyScreen.State
        do {
            loadedState = awaitItem()
        } while (loadedState.items.isEmpty())

        // Trigger navigation event
        loadedState.eventSink(MyScreen.Event.ItemClicked(item))

        // Assert navigation occurred
        val nextScreen = navigator.awaitNextScreen()
        assertThat(nextScreen).isInstanceOf(DetailScreen::class)

        cancelAndIgnoreRemainingEvents()
    }
}

// For root navigation resets (e.g., after login/logout):
@Test
fun `resets navigation root on logout`() = runTest {
    // ...
    state.eventSink(MyScreen.Event.Logout)
    val reset = navigator.awaitResetRoot()
    assertThat(reset.newRoot).isEqualTo(AccessTokenScreen)
    // ...
}
```

### Error State Testing

```kotlin
@Test
fun `presenter handles network failure`() = runTest {
    val navigator = FakeNavigator(MyScreen)
    val presenter = createPresenter(
        response = ApiResult.networkFailure(IOException("Network error"))
    )

    presenter.test {
        var errorState: MyScreen.State
        do {
            errorState = awaitItem()
        } while (errorState.errorMessage == null)

        assertThat(errorState.errorMessage)
            .isEqualTo("Network error. Please check your connection.")
        cancelAndIgnoreRemainingEvents()
    }
}
```

### Full TrmnlDevicesPresenter Test Example

See [`app/src/test/java/ink/trmnl/android/buddy/ui/devices/TrmnlDevicesScreenTest.kt`](../app/src/test/java/ink/trmnl/android/buddy/ui/devices/TrmnlDevicesScreenTest.kt) for a complete example covering:
- Initial device loading
- Empty state handling
- HTTP error responses (401, 404)
- Network failure handling
- Missing API token
- Navigation events (settings, device detail, content hub, token reset)
- Multiple device display

---

## Repository Testing with Fakes

Repository tests use fake DAO implementations to test the repository logic without a real Room database.

### Pattern

```kotlin
class MyRepositoryTest {
    private lateinit var repository: MyRepository
    private lateinit var fakeDao: FakeMyDao

    @Before
    fun setup() {
        fakeDao = FakeMyDao()
        repository = MyRepositoryImpl(fakeDao)
    }

    @Test
    fun `stores and retrieves data correctly`() = runTest {
        // When
        repository.save(item)

        // Then
        val result = repository.getAll().first()
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(item)
    }
}
```

### BatteryHistoryRepository Example

See [`app/src/test/java/ink/trmnl/android/buddy/data/database/BatteryHistoryRepositoryTest.kt`](../app/src/test/java/ink/trmnl/android/buddy/data/database/BatteryHistoryRepositoryTest.kt) for a complete example covering:
- Recording readings
- Querying by device ID
- Latest reading retrieval
- Time-range queries
- History deletion

---

## Worker Testing with MockWebServer

WorkManager workers that make API calls are tested using `TestListenableWorkerBuilder` (from `androidx.work:work-testing`) combined with MockWebServer for HTTP simulation.

### Setup Pattern

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyWorkerTest {
    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TrmnlApiService

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(ApiResultConverterFactory)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ApiResultCallAdapterFactory)
            .build()

        apiService = retrofit.create(TrmnlApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun createWorker(): MyWorker =
        TestListenableWorkerBuilder<MyWorker>(context)
            .setWorkerFactory(TestMyWorkerFactory(apiService, ...))
            .build()
}
```

### Testing Worker Results

```kotlin
@Test
fun `successful data collection returns success result`() = runTest {
    // Enqueue mock HTTP response
    mockWebServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"data": [...]}""")
    )

    val worker = createWorker()
    val result = worker.doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class)
}

@Test
fun `401 unauthorized returns failure`() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(401))

    val worker = createWorker()
    val result = worker.doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class)
}

@Test
fun `network error returns retry result`() = runTest {
    mockWebServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

    val worker = createWorker()
    val result = worker.doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class)
}
```

### Custom WorkerFactory for Testing

Workers with constructor dependencies require a custom `WorkerFactory`:

```kotlin
private class TestMyWorkerFactory(
    private val apiService: TrmnlApiService,
    private val fakeRepo: FakeMyRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker = MyWorker(appContext, workerParameters, apiService, fakeRepo)
}
```

See [`app/src/test/java/ink/trmnl/android/buddy/work/BatteryCollectionWorkerTest.kt`](../app/src/test/java/ink/trmnl/android/buddy/work/BatteryCollectionWorkerTest.kt) for a complete example.

---

## Flow Testing with Turbine

For testing `Flow`-based APIs (repositories, preferences), use Turbine from the `app.cash.turbine` library.

```kotlin
import app.cash.turbine.test

@Test
fun `flow emits updated value after change`() = runTest {
    val repository = MyRepository()

    repository.dataFlow.test {
        // Assert initial emission
        assertThat(awaitItem()).isEmpty()

        // Trigger change
        repository.save(item)

        // Assert new emission
        assertThat(awaitItem()).hasSize(1)

        cancelAndIgnoreRemainingEvents()
    }
}
```

---

## Test Doubles: Fakes vs Mocks

**This project prefers fakes over mocks.** Fakes are working implementations that behave correctly but use in-memory storage instead of real persistence.

| Approach | When to Use |
|----------|-------------|
| **Fake** (preferred) | Repository interfaces, DAO interfaces, API services, helper classes |
| **Mock** (avoid) | Only when creating a fake is prohibitively complex |

### Why Fakes?

- **Correctness**: Fakes behave like real implementations
- **Simplicity**: No mocking framework setup or verification syntax
- **Stability**: Not fragile to internal implementation changes
- **Kotlin-native**: Written in idiomatic Kotlin with no annotation magic

### Writing a Fake

A good fake:
1. Implements the interface under test
2. Uses in-memory data structures (`MutableList`, `MutableStateFlow`, etc.)
3. Exposes test-visible properties for assertions
4. Supports failure simulation via constructor parameters

```kotlin
class FakeMyRepository(
    initialData: List<MyEntity> = emptyList(),
    private val shouldThrowOnSave: Boolean = false,
) : MyRepository {
    private val dataFlow = MutableStateFlow(initialData.toList())

    /** Test-visible: verify save was called with correct data */
    val savedItems: List<MyEntity> get() = dataFlow.value

    override suspend fun save(entity: MyEntity) {
        if (shouldThrowOnSave) throw Exception("Test error")
        dataFlow.update { current -> current + entity }
    }

    override fun getAll(): Flow<List<MyEntity>> = dataFlow
}
```

---

## Available Fake Implementations

Located in `app/src/test/java/ink/trmnl/android/buddy/fakes/`:

### `FakeUserPreferencesRepository`

```kotlin
FakeUserPreferencesRepository(
    initialPreferences = UserPreferences(apiToken = "test_token"),
    shouldThrowOnSave = false,
)
```

**Test-visible properties**: `batteryTrackingEnabled`, `lowBatteryNotificationEnabled`, `lowBatteryThreshold`, `wasCleared`, `savedToken`, `onboardingCompleted`, `securityEnabled`

### `FakeBatteryHistoryRepository`

```kotlin
FakeBatteryHistoryRepository(
    initialHistory = listOf(batteryEntity1, batteryEntity2),
    shouldThrowOnRecord = false,
)
```

**Test-visible properties**: `recordedReadings`

**Test helper methods**: `clear()`

### `FakeDeviceTokenRepository`

```kotlin
FakeDeviceTokenRepository(
    hasToken = true,           // Pre-populate with "ABC-123" → "test-token"
    initialTokens = mapOf("DEF-456" to "other-token"),
    shouldThrowOnSave = false,
    shouldThrowOnClear = false,
)
```

**Test-visible properties**: `wasCleared`

**Test helper methods**: `getAllTokens()`

### `FakePlaylistItemsRepository`

```kotlin
FakePlaylistItemsRepository(
    initialResult = Result.success(listOf(item1, item2)),
)
```

**Test-visible properties**: `getPlaylistItemsCallCount`, `getPlaylistItemsForDeviceCallCount`, `lastDeviceId`, `lastForceRefresh`, `wasCacheCleared`, `cacheStaleCheckCount`, `isCacheStaleResult`, `updateVisibilityCalls`

**Test helper methods**: `setResult()`, `setUpdateVisibilityResult()`, `setUpdateVisibilityError()`, `resetUpdateVisibilityCalls()`

### `FakeTrmnlApiService`

```kotlin
FakeTrmnlApiService(
    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
    userInfoResponse = ApiResult.success(UserResponse(...)),
)
```

Configurable responses for all API endpoints; useful for testing repositories and presenters that depend on the API service directly.

### `FakeBiometricAuthHelper`

```kotlin
FakeBiometricAuthHelper(isAvailable = true)
```

Simulates biometric hardware availability.

### Fake DAOs

Located in `app/src/test/java/ink/trmnl/android/buddy/data/database/` and `app/src/test/java/ink/trmnl/android/buddy/content/db/`:

- `FakeBatteryHistoryDao` — In-memory battery readings with sorted query support
- `FakeBookmarkedRecipeDao` — In-memory bookmarks with Flow support
- `FakeAnnouncementDao` — In-memory announcements with read state tracking
- `FakeBlogPostDao` — In-memory blog posts with read state tracking

---

## Code Coverage with Kover

The project uses [Kover](https://github.com/Kotlin/kotlinx-kover) for Kotlin code coverage.

### Configuration

Kover is applied at the root `build.gradle.kts` level and merges coverage from all modules:

```kotlin
// build.gradle.kts (root)
plugins {
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
    kover(project(":api"))
    kover(project(":content"))
}
```

### Generating Reports Locally

```bash
# Generate HTML coverage report (opens in browser)
./gradlew koverHtmlReport

# Generate XML coverage report (for CI/CD tools)
./gradlew koverXmlReport
```

HTML report location: `<project-root>/build/reports/kover/html/index.html`

### CI/CD Integration

Coverage reports are automatically:
1. Generated in CI via `./gradlew koverXmlReport`
2. Uploaded to [Codecov](https://codecov.io) via the `codecov/codecov-action` GitHub Action

See [`.github/workflows/android.yml`](../.github/workflows/android.yml) for the full CI configuration.

---

## Running Tests

### All Tests

```bash
./gradlew test
```

### Module-Specific Tests

```bash
# App module only
./gradlew :app:test

# API module only
./gradlew :api:test

# Content module only
./gradlew :content:test
```

### Single Test Class

```bash
./gradlew :app:test --tests ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreenTest
```

### Single Test Method

```bash
./gradlew :app:test --tests "ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreenTest.presenter loads devices on initial composition"
```

### With Coverage Report

```bash
./gradlew testDebugUnitTest koverHtmlReport
```

### Before Every Commit

```bash
# 1. Format code
./gradlew formatKotlin

# 2. Run tests
./gradlew test

# 3. Verify build
./gradlew assembleDebug
```
