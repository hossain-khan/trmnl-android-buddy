package ink.trmnl.android.buddy.ui.devices

import android.content.Context
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UserResponse
import ink.trmnl.android.buddy.content.models.ContentItem
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.content.repository.ContentFeedRepository
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devicedetail.DeviceDetailScreen
import ink.trmnl.android.buddy.ui.devicetoken.DeviceTokenScreen
import ink.trmnl.android.buddy.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.time.Instant

/**
 * Unit tests for TrmnlDevicesScreenPresenter.
 *
 * Tests cover:
 * - Initial loading state and device fetch
 * - Empty state when no devices
 * - Error handling (401, 404, network errors, missing token)
 * - Retry functionality
 * - Pull-to-refresh
 * - Navigation events (detail, settings, token, preview, content hub)
 * - User interactions (privacy toggle, content clicks, battery alerts)
 * - Edge cases (multiple devices, missing data)
 */
@RunWith(RobolectricTestRunner::class)
class TrmnlDevicesScreenTest {
    @Test
    fun `presenter loads devices on initial composition`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(2)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait until we get devices loaded (skip initial and loading states)
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty() && loadedState.errorMessage == null)

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.devices).hasSize(2)
                assertThat(loadedState.errorMessage).isNull()
                assertThat(loadedState.isUnauthorized).isFalse()
            }
        }

    @Test
    fun `presenter shows empty state when no devices`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = emptyList())),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.devices).isEmpty()
                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.errorMessage).isNull()
            }
        }

    @Test
    fun `presenter handles 401 unauthorized error`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val apiService =
                FakeApiService(
                    devicesResponse =
                        ApiResult.httpFailure(
                            401,
                            ApiError("Unauthorized"),
                        ),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for error state
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("Unauthorized. Please check your API token.")
                assertThat(errorState.isUnauthorized).isTrue()
                assertThat(errorState.devices).isEmpty()
            }
        }

    @Test
    fun `presenter handles 404 not found error`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val apiService =
                FakeApiService(
                    devicesResponse =
                        ApiResult.httpFailure(
                            404,
                            ApiError("Not Found"),
                        ),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for error state
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("API endpoint not found.")
                assertThat(errorState.isUnauthorized).isFalse()
            }
        }

    @Test
    fun `presenter handles network failure`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val apiService =
                FakeApiService(
                    devicesResponse =
                        ApiResult.networkFailure(
                            IOException("Network error"),
                        ),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for error state
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("Network error. Please check your connection.")
                assertThat(errorState.isUnauthorized).isFalse()
            }
        }

    @Test
    fun `presenter handles missing API token`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = emptyList())),
                )
            val userPrefsRepository =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = null),
                )
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for error state
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.isLoading).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("API token not found. Please configure your token.")
                assertThat(errorState.isUnauthorized).isFalse()
            }
        }

    @Test
    fun `presenter handles refresh event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(1)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                assertThat(loadedState.devices).hasSize(1)

                // Trigger refresh
                loadedState.eventSink(TrmnlDevicesScreen.Event.Refresh)

                // Should see loading state
                var refreshingState: TrmnlDevicesScreen.State
                do {
                    refreshingState = awaitItem()
                } while (!refreshingState.isLoading && refreshingState.isContentLoading)

                assertThat(refreshingState.isLoading).isTrue()
                assertThat(refreshingState.isContentLoading).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to settings on SettingsClicked event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(1)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger settings navigation
                loadedState.eventSink(TrmnlDevicesScreen.Event.SettingsClicked)

                // Verify navigation
                assertThat(navigator.awaitNextScreen()).isEqualTo(SettingsScreen)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to device detail on DeviceClicked event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1, "Test Device")
            val devices = listOf(device)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger device click
                loadedState.eventSink(TrmnlDevicesScreen.Event.DeviceClicked(device))

                // Verify navigation to device detail
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isNotNull()
                assertThat(nextScreen).isInstanceOf(DeviceDetailScreen::class)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to device token screen on DeviceSettingsClicked event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1, "Test Device")
            val devices = listOf(device)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger device settings click
                loadedState.eventSink(TrmnlDevicesScreen.Event.DeviceSettingsClicked(device))

                // Verify navigation to device token screen
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isNotNull()
                assertThat(nextScreen).isInstanceOf(DeviceTokenScreen::class)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to content hub on ViewAllContentClicked event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(1)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger view all content click
                loadedState.eventSink(TrmnlDevicesScreen.Event.ViewAllContentClicked)

                // Verify navigation to content hub
                assertThat(navigator.awaitNextScreen()).isEqualTo(ContentHubScreen)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter toggles privacy on TogglePrivacy event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(1)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                val initialPrivacy = loadedState.isPrivacyEnabled
                assertThat(initialPrivacy).isTrue()

                // Toggle privacy
                loadedState.eventSink(TrmnlDevicesScreen.Event.TogglePrivacy)

                // Wait for privacy toggle
                var toggledState: TrmnlDevicesScreen.State
                do {
                    toggledState = awaitItem()
                } while (toggledState.isPrivacyEnabled == initialPrivacy)

                assertThat(toggledState.isPrivacyEnabled).isFalse()
                assertThat(toggledState.snackbarMessage).isEqualTo("Device ID and MAC address now visible")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter resets token and navigates to access token screen on ResetToken event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(1)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger reset token
                loadedState.eventSink(TrmnlDevicesScreen.Event.ResetToken)

                // Verify navigation to access token screen (resetRoot)
                assertThat(navigator.awaitResetRoot()).isEqualTo(AccessTokenScreen)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows snackbar on BatteryAlertClicked event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1, "Test Device", percentCharged = 15.0)
            val devices = listOf(device)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger battery alert
                loadedState.eventSink(
                    TrmnlDevicesScreen.Event.BatteryAlertClicked(
                        device = device,
                        thresholdPercent = 20,
                    ),
                )

                // Wait for snackbar message
                var snackbarState: TrmnlDevicesScreen.State
                do {
                    snackbarState = awaitItem()
                } while (snackbarState.snackbarMessage == null)

                assertThat(snackbarState.snackbarMessage).isEqualTo(
                    "Battery level (15%) is below your threshold of 20%. Consider charging soon.",
                )

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter marks content as read on ContentItemClicked event`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = createTestDevices(1)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                val contentItem =
                    ContentItem.Announcement(
                        id = "1",
                        title = "Test Announcement",
                        summary = "Test summary",
                        link = "https://example.com",
                        publishedDate = Instant.now(),
                        isRead = false,
                    )

                // Trigger content item click
                loadedState.eventSink(TrmnlDevicesScreen.Event.ContentItemClicked(contentItem))

                // Verify announcement was marked as read
                assertThat(announcementRepository.markedAsReadIds).hasSize(1)
                assertThat(announcementRepository.markedAsReadIds).isEqualTo(listOf("1"))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles multiple devices with various battery and wifi levels`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices =
                listOf(
                    createTestDevice(1, "High Battery", percentCharged = 90.0, wifiStrength = 80.0),
                    createTestDevice(2, "Medium Battery", percentCharged = 50.0, wifiStrength = 60.0),
                    createTestDevice(3, "Low Battery", percentCharged = 15.0, wifiStrength = 30.0),
                )
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait until we get all devices loaded
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                assertThat(loadedState.devices).hasSize(3)
                assertThat(loadedState.devices[0].name).isEqualTo("High Battery")
                assertThat(loadedState.devices[0].percentCharged).isEqualTo(90.0)
                assertThat(loadedState.devices[1].percentCharged).isEqualTo(50.0)
                assertThat(loadedState.devices[2].percentCharged).isEqualTo(15.0)
            }
        }

    @Test
    fun `presenter handles device with null battery voltage and rssi`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device =
                Device(
                    id = 1,
                    name = "Device With Null Data",
                    friendlyId = "NULL-001",
                    macAddress = "12:34:56:78:9A:BC",
                    batteryVoltage = null, // Null battery voltage
                    rssi = null, // Null RSSI
                    percentCharged = 50.0,
                    wifiStrength = 50.0,
                )
            val devices = listOf(device)
            val apiService =
                FakeApiService(
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )
            val userPrefsRepository = FakeUserPreferencesRepository()
            val deviceTokenRepository = FakeDeviceTokenRepository()
            val contentFeedRepository = ink.trmnl.android.buddy.content.repository.ContentFeedRepository(FakeAnnouncementDao(), FakeBlogPostDao())
            val announcementRepository = ink.trmnl.android.buddy.content.repository.AnnouncementRepository(fakeAnnouncementDao)
            val blogPostRepository = ink.trmnl.android.buddy.content.repository.BlogPostRepository(fakeBlogPostDao)
            val context = RuntimeEnvironment.getApplication()

            val presenter =
                TrmnlDevicesPresenter(
                    navigator = navigator,
                    context = context,
                    apiService = apiService,
                    userPreferencesRepository = userPrefsRepository,
                    deviceTokenRepository = deviceTokenRepository,
                    contentFeedRepository = contentFeedRepository,
                    announcementRepository = announcementRepository,
                    blogPostRepository = blogPostRepository,
                )

            // When/Then
            presenter.test {
                // Wait for loaded state
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                assertThat(loadedState.devices).hasSize(1)
                assertThat(loadedState.devices[0].batteryVoltage).isNull()
                assertThat(loadedState.devices[0].rssi).isNull()
                assertThat(loadedState.devices[0].percentCharged).isEqualTo(50.0)
            }
        }
}

// ========== Helper Functions ==========

/**
 * Create a list of test devices.
 */
private fun createTestDevices(count: Int): List<Device> = (1..count).map { createTestDevice(it, "Device $it") }

/**
 * Create a single test device.
 */
private fun createTestDevice(
    id: Int,
    name: String,
    percentCharged: Double = 75.0,
    wifiStrength: Double = 70.0,
): Device =
    Device(
        id = id,
        name = name,
        friendlyId = "ABC-$id",
        macAddress = "12:34:56:78:9A:B$id",
        batteryVoltage = 3.7,
        rssi = -50,
        percentCharged = percentCharged,
        wifiStrength = wifiStrength,
    )

// ========== Fake Implementations ==========

/**
 * Fake implementation of TrmnlApiService for testing.
 */
private class FakeApiService(
    private val devicesResponse: ApiResult<DevicesResponse, ApiError> =
        ApiResult.success(DevicesResponse(data = emptyList())),
    private val displayResponse: ApiResult<Display, ApiError> =
        ApiResult.success(
            Display(
                status = 200,
                refreshRate = 300,
                imageUrl = "https://example.com/image.png",
                filename = "image.png",
                renderedAt = "2024-01-01T00:00:00Z",
            ),
        ),
) : TrmnlApiService {
    override suspend fun getDevices(authorization: String): ApiResult<DevicesResponse, ApiError> = devicesResponse

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ): ApiResult<DeviceResponse, ApiError> = throw NotImplementedError("Not needed for this test")

    override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> = displayResponse

    override suspend fun userInfo(authorization: String): ApiResult<UserResponse, ApiError> =
        throw NotImplementedError("Not needed for this test")

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int?,
        perPage: Int?,
    ): ApiResult<RecipesResponse, ApiError> = throw NotImplementedError("Not needed for this test")

    override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> =
        throw NotImplementedError("Not needed for this test")

    override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> =
        throw NotImplementedError("Not needed for this test")
}

/**
 * Fake implementation of UserPreferencesRepository for testing.
 */
private class FakeUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences(apiToken = "test_token"),
) : UserPreferencesRepository {
    private val _userPreferencesFlow = MutableStateFlow(initialPreferences)

    override val userPreferencesFlow = _userPreferencesFlow

    override suspend fun saveApiToken(token: String) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = token)
    }

    override suspend fun clearApiToken() {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = null)
    }

    override suspend fun setOnboardingCompleted() {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isOnboardingCompleted = true)
    }

    override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isBatteryTrackingEnabled = enabled)
    }

    override suspend fun setLowBatteryNotificationEnabled(enabled: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isLowBatteryNotificationEnabled = enabled)
    }

    override suspend fun setLowBatteryThreshold(percent: Int) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(lowBatteryThresholdPercent = percent)
    }

    override suspend fun setRssFeedContentEnabled(enabled: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isRssFeedContentEnabled = enabled)
    }

    override suspend fun setRssFeedContentNotificationEnabled(enabled: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isRssFeedContentNotificationEnabled = enabled)
    }

    override suspend fun setAnnouncementAuthBannerDismissed(dismissed: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isAnnouncementAuthBannerDismissed = dismissed)
    }

    override suspend fun setSecurityEnabled(enabled: Boolean) {
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isSecurityEnabled = enabled)
    }

    override suspend fun clearAll() {
        _userPreferencesFlow.value = UserPreferences()
    }
}

/**
 * Fake implementation of DeviceTokenRepository for testing.
 */
private class FakeDeviceTokenRepository : DeviceTokenRepository {
    private val tokens = mutableMapOf<String, String>()

    override suspend fun saveDeviceToken(
        deviceFriendlyId: String,
        token: String,
    ) {
        tokens[deviceFriendlyId] = token
    }

    override suspend fun getDeviceToken(deviceFriendlyId: String): String? = tokens[deviceFriendlyId]

    override fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?> = flowOf(tokens[deviceFriendlyId])

    override suspend fun clearDeviceToken(deviceFriendlyId: String) {
        tokens.remove(deviceFriendlyId)
    }

    override suspend fun hasDeviceToken(deviceFriendlyId: String): Boolean = tokens.containsKey(deviceFriendlyId)

    override suspend fun clearAll() {
        tokens.clear()
    }
}
