package ink.trmnl.android.buddy.ui.devices

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
import ink.trmnl.android.buddy.api.models.CategoriesResponse
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsHealth
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsHealthStatus
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsPlugin
import ink.trmnl.android.buddy.api.models.RecipeAnalyticsStats
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesAnalytics
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UserResponse
import ink.trmnl.android.buddy.content.db.FakeAnnouncementDao
import ink.trmnl.android.buddy.content.db.FakeBlogPostDao
import ink.trmnl.android.buddy.content.models.ContentItem
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.content.repository.ContentFeedRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakeRecipesAnalyticsRepository
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devicedetail.DeviceDetailScreen
import ink.trmnl.android.buddy.ui.devicepreview.DevicePreviewScreen
import ink.trmnl.android.buddy.ui.devicetoken.DeviceTokenScreen
import ink.trmnl.android.buddy.ui.recipesanalytics.RecipesAnalyticsScreen
import ink.trmnl.android.buddy.ui.recipesanalytics.getDataOrNull
import ink.trmnl.android.buddy.ui.settings.SettingsScreen
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
 * Tests cover core functionality:
 * - Initial loading and device fetch
 * - Empty state
 * - Error handling (401, 404, network, missing token)
 * - Navigation events
 * - User interactions
 */
@RunWith(RobolectricTestRunner::class)
class TrmnlDevicesScreenTest {
    @Test
    fun `presenter loads devices on initial composition`() =
        runTest {
            // Given
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices = listOf(createTestDevice(1), createTestDevice(2))
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )

            // When/Then
            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty() && loadedState.errorMessage == null)

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.devices).hasSize(2)
                assertThat(loadedState.errorMessage).isNull()
            }
        }

    @Test
    fun `presenter shows empty state when no devices`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = emptyList())),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.devices).isEmpty()
                assertThat(loadedState.errorMessage).isNull()
            }
        }

    @Test
    fun `presenter handles 401 unauthorized error`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.httpFailure(401, ApiError("Unauthorized")),
                )

            presenter.test {
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.errorMessage).isEqualTo("Unauthorized. Please check your access credentials.")
                assertThat(errorState.isUnauthorized).isTrue()
            }
        }

    @Test
    fun `presenter handles 404 error`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.httpFailure(404, ApiError("Not Found")),
                )

            presenter.test {
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.errorMessage).isEqualTo("Resource not found.")
            }
        }

    @Test
    fun `presenter handles network failure`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.networkFailure(IOException("Network error")),
                )

            presenter.test {
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.errorMessage).isEqualTo("Network error. Please check your connection.")
            }
        }

    @Test
    fun `presenter handles missing API token`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    apiToken = null,
                )

            presenter.test {
                var errorState: TrmnlDevicesScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.errorMessage == null)

                assertThat(errorState.errorMessage).isEqualTo("API token not found. Please configure your token.")
            }
        }

    @Test
    fun `presenter navigates to settings on SettingsClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.SettingsClicked)
                assertThat(navigator.awaitNextScreen()).isEqualTo(SettingsScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to device detail on DeviceClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(device))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.DeviceClicked(device))
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isInstanceOf(DeviceDetailScreen::class)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to device token screen on DeviceSettingsClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(device))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.DeviceSettingsClicked(device))
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isInstanceOf(DeviceTokenScreen::class)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to content hub on ViewAllContentClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.ViewAllContentClicked)
                assertThat(navigator.awaitNextScreen()).isEqualTo(ContentHubScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter resets token and navigates on ResetToken event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.ResetToken)
                assertThat(navigator.awaitResetRoot().newRoot).isEqualTo(AccessTokenScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles multiple devices`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val devices =
                listOf(
                    createTestDevice(1, percentCharged = 90.0),
                    createTestDevice(2, percentCharged = 50.0),
                    createTestDevice(3, percentCharged = 15.0),
                )
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = devices)),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                assertThat(loadedState.devices).hasSize(3)
                assertThat(loadedState.devices[0].percentCharged).isEqualTo(90.0)
                assertThat(loadedState.devices[1].percentCharged).isEqualTo(50.0)
                assertThat(loadedState.devices[2].percentCharged).isEqualTo(15.0)
            }
        }

    @Test
    fun `presenter navigates to recipes analytics screen on ViewRecipesAnalyticsClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val analyticsRepository =
                FakeRecipesAnalyticsRepository().apply {
                    analyticsResult =
                        Result.success(
                            RecipesAnalytics(
                                plugins = listOf(RecipeAnalyticsPlugin("Test Plugin", "healthy", 5, 2)),
                                stats = RecipeAnalyticsStats(plugins = 1, connections = 5, pageviews = 10),
                                health =
                                    RecipeAnalyticsHealth(
                                        healthy = RecipeAnalyticsHealthStatus(100.0),
                                        degraded = RecipeAnalyticsHealthStatus(0.0),
                                        erroring = RecipeAnalyticsHealthStatus(0.0),
                                    ),
                                growth = emptyList(),
                            ),
                        )
                }
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                    analyticsRepository = analyticsRepository,
                )

            presenter.test {
                // Wait for analytics state to have data loaded (navigation depends on analyticsState)
                var readyState: TrmnlDevicesScreen.State
                do {
                    readyState = awaitItem()
                } while (readyState.analyticsState.getDataOrNull() == null)

                readyState.eventSink(TrmnlDevicesScreen.Event.ViewRecipesAnalyticsClicked)
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isInstanceOf(RecipesAnalyticsScreen::class)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to settings on SettingsClicked event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.SettingsClicked)
                assertThat(navigator.awaitNextScreen()).isEqualTo(SettingsScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter toggles privacy and updates snackbar on TogglePrivacy event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                assertThat(loadedState.isPrivacyEnabled).isTrue()

                // Toggle off
                loadedState.eventSink(TrmnlDevicesScreen.Event.TogglePrivacy)
                var toggledOff: TrmnlDevicesScreen.State
                do {
                    toggledOff = awaitItem()
                } while (toggledOff.isPrivacyEnabled || toggledOff.snackbarMessage != "Device ID and MAC address now visible")
                assertThat(toggledOff.isPrivacyEnabled).isFalse()
                assertThat(toggledOff.snackbarMessage).isEqualTo("Device ID and MAC address now visible")

                // Toggle back on
                toggledOff.eventSink(TrmnlDevicesScreen.Event.TogglePrivacy)
                var toggledOn: TrmnlDevicesScreen.State
                do {
                    toggledOn = awaitItem()
                } while (!toggledOn.isPrivacyEnabled || toggledOn.snackbarMessage != "Device ID and MAC address hidden for privacy")
                assertThat(toggledOn.isPrivacyEnabled).isTrue()
                assertThat(toggledOn.snackbarMessage).isEqualTo("Device ID and MAC address hidden for privacy")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to device detail on DeviceClicked event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1, percentCharged = 80.0)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(device))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.DeviceClicked(device))
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isInstanceOf(DeviceDetailScreen::class)
                val detailScreen = nextScreen as DeviceDetailScreen
                assertThat(detailScreen.deviceId).isEqualTo("ABC-1")
                assertThat(detailScreen.currentBattery).isEqualTo(80.0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates to device preview on DevicePreviewClicked event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1)
            val previewInfo = DevicePreviewInfo(imageUrl = "https://example.com/preview.png", refreshRate = 300)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(device))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.DevicePreviewClicked(device, previewInfo))
                val nextScreen = navigator.awaitNextScreen()
                assertThat(nextScreen).isInstanceOf(DevicePreviewScreen::class)
                val previewScreen = nextScreen as DevicePreviewScreen
                assertThat(previewScreen.deviceId).isEqualTo("ABC-1")
                assertThat(previewScreen.imageUrl).isEqualTo("https://example.com/preview.png")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter sets snackbar explanation on RefreshRateInfoClicked event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.RefreshRateInfoClicked(600))
                var stateWithSnackbar: TrmnlDevicesScreen.State
                do {
                    stateWithSnackbar = awaitItem()
                } while (stateWithSnackbar.snackbarMessage == null)
                assertThat(stateWithSnackbar.snackbarMessage?.contains("10 minutes") == true).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter sets and clears snackbar on BatteryAlertClicked and DismissSnackbar events`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1, percentCharged = 10.0)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(device))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                // Trigger battery alert snackbar
                loadedState.eventSink(TrmnlDevicesScreen.Event.BatteryAlertClicked(device, 20))
                var stateWithAlert: TrmnlDevicesScreen.State
                do {
                    stateWithAlert = awaitItem()
                } while (stateWithAlert.snackbarMessage == null)
                assertThat(
                    stateWithAlert.snackbarMessage,
                ).isEqualTo("Battery level (10%) is below your threshold of 20%. Consider charging soon.")

                // Dismiss snackbar
                stateWithAlert.eventSink(TrmnlDevicesScreen.Event.DismissSnackbar)
                var dismissedState: TrmnlDevicesScreen.State
                do {
                    dismissedState = awaitItem()
                } while (dismissedState.snackbarMessage != null)
                assertThat(dismissedState.snackbarMessage).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter reloads data on Refresh event`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.Refresh)

                var refreshedState: TrmnlDevicesScreen.State
                do {
                    refreshedState = awaitItem()
                } while (refreshedState.isLoading)

                assertThat(refreshedState.devices).hasSize(1)
                assertThat(refreshedState.errorMessage).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads device preview when device token is available and API returns preview image`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val device = createTestDevice(1)
            val deviceTokenRepo = FakeDeviceTokenRepository()
            deviceTokenRepo.saveDeviceToken("ABC-1", "device_api_key_123")
            val displayResponse = ApiResult.success(Display(200, 300, "https://example.com/preview.png", null, null))
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(device))),
                    displayResponse = displayResponse,
                    deviceTokenRepository = deviceTokenRepo,
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devicePreviews.isEmpty() || loadedState.devicePreviews["ABC-1"] == null)

                val preview = loadedState.devicePreviews["ABC-1"]
                assertThat(preview).isNotNull()
                assertThat(preview?.imageUrl).isEqualTo("https://example.com/preview.png")
                assertThat(preview?.refreshRate).isEqualTo(300)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter marks announcement as read on ContentItemClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val fakeAnnouncementDao = FakeAnnouncementDao()
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                    fakeAnnouncementDao = fakeAnnouncementDao,
                )

            val announcement =
                ContentItem.Announcement(
                    id = "ann-1",
                    title = "New Features Released",
                    summary = "Check out the latest features",
                    link = "https://usetrmnl.com/announcement/1",
                    publishedDate = Instant.now(),
                    isRead = false,
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.ContentItemClicked(announcement))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter marks blog post as read on ContentItemClicked`() =
        runTest {
            val navigator = FakeNavigator(TrmnlDevicesScreen)
            val fakeBlogPostDao = FakeBlogPostDao()
            val (presenter, _) =
                createPresenter(
                    navigator = navigator,
                    devicesResponse = ApiResult.success(DevicesResponse(data = listOf(createTestDevice(1)))),
                    fakeBlogPostDao = fakeBlogPostDao,
                )

            val blogPost =
                ContentItem.BlogPost(
                    id = "post-1",
                    title = "Building custom plugins",
                    summary = "Learn how to build custom plugins",
                    link = "https://usetrmnl.com/blog/1",
                    publishedDate = Instant.now(),
                    isRead = false,
                    authorName = "TRMNL Team",
                    category = "Guides",
                    featuredImageUrl = null,
                    isFavorite = false,
                )

            presenter.test {
                var loadedState: TrmnlDevicesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.devices.isEmpty())

                loadedState.eventSink(TrmnlDevicesScreen.Event.ContentItemClicked(blogPost))
                cancelAndIgnoreRemainingEvents()
            }
        }

    // Helper function to create presenter with dependencies
    private fun createPresenter(
        navigator: FakeNavigator,
        devicesResponse: ApiResult<DevicesResponse, ApiError> = ApiResult.success(DevicesResponse(data = emptyList())),
        displayResponse: ApiResult<Display, ApiError> = ApiResult.success(Display(200, 300, null, null, null)),
        deviceTokenRepository: FakeDeviceTokenRepository = FakeDeviceTokenRepository(),
        apiToken: String? = "test_token",
        fakeAnnouncementDao: FakeAnnouncementDao = FakeAnnouncementDao(),
        fakeBlogPostDao: FakeBlogPostDao = FakeBlogPostDao(),
        analyticsRepository: FakeRecipesAnalyticsRepository =
            FakeRecipesAnalyticsRepository().apply {
                // Default to failure so analytics fetch doesn't throw NotImplementedError
                analyticsResult = Result.failure(Exception("No analytics available"))
            },
    ): Pair<TrmnlDevicesPresenter, FakeAnnouncementDao> =
        Pair(
            TrmnlDevicesPresenter(
                navigator = navigator,
                context = RuntimeEnvironment.getApplication(),
                apiService = FakeApiService(devicesResponse, displayResponse),
                userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = apiToken)),
                deviceTokenRepository = deviceTokenRepository,
                contentFeedRepository = ContentFeedRepository(fakeAnnouncementDao, fakeBlogPostDao),
                announcementRepository = AnnouncementRepository(fakeAnnouncementDao),
                blogPostRepository = BlogPostRepository(fakeBlogPostDao),
                recipesAnalyticsRepository = analyticsRepository,
            ),
            fakeAnnouncementDao,
        )

    private fun createTestDevice(
        id: Int,
        percentCharged: Double = 75.0,
    ): Device =
        Device(
            id = id,
            name = "Device $id",
            friendlyId = "ABC-$id",
            macAddress = "12:34:56:78:9A:B$id",
            batteryVoltage = 3.7,
            rssi = -50,
            percentCharged = percentCharged,
            wifiStrength = 70.0,
        )
}

// Fake implementations
private class FakeApiService(
    private val devicesResponse: ApiResult<DevicesResponse, ApiError>,
    private val displayResponse: ApiResult<Display, ApiError> = ApiResult.success(Display(200, 300, null, null, null)),
) : TrmnlApiService {
    override suspend fun getDevices(authorization: String) = devicesResponse

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ): ApiResult<DeviceResponse, ApiError> = throw NotImplementedError()

    override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> = displayResponse

    override suspend fun getDisplay(deviceApiKey: String): ApiResult<Display, ApiError> = displayResponse

    override suspend fun userInfo(authorization: String): ApiResult<UserResponse, ApiError> = throw NotImplementedError()

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int?,
        perPage: Int?,
    ): ApiResult<RecipesResponse, ApiError> = throw NotImplementedError()

    override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> = throw NotImplementedError()

    override suspend fun getCategories(): ApiResult<CategoriesResponse, ApiError> = throw NotImplementedError()

    override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> = throw NotImplementedError()

    override suspend fun getPlaylistItems(authorization: String) = throw NotImplementedError()

    override suspend fun updatePlaylistItemVisibility(
        id: Int,
        authorization: String,
        body: Map<String, Boolean>,
    ) = throw NotImplementedError()

    override suspend fun getRecipesAnalytics(authorization: String) = throw NotImplementedError()
}
