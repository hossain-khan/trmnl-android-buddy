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
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.DeviceModelsResponse
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.Display
import ink.trmnl.android.buddy.api.models.RecipeDetailResponse
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.api.models.UserResponse
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostDao
import ink.trmnl.android.buddy.content.db.BlogPostEntity
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

                assertThat(errorState.errorMessage).isEqualTo("Unauthorized. Please check your API token.")
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

                assertThat(errorState.errorMessage).isEqualTo("API endpoint not found.")
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

    // Helper function to create presenter with dependencies
    private fun createPresenter(
        navigator: FakeNavigator,
        devicesResponse: ApiResult<DevicesResponse, ApiError> = ApiResult.success(DevicesResponse(data = emptyList())),
        apiToken: String? = "test_token",
    ): Pair<TrmnlDevicesPresenter, FakeAnnouncementDao> {
        val fakeAnnouncementDao = FakeAnnouncementDao()
        val fakeBlogPostDao = FakeBlogPostDao()

        return Pair(
            TrmnlDevicesPresenter(
                navigator = navigator,
                context = RuntimeEnvironment.getApplication(),
                apiService = FakeApiService(devicesResponse),
                userPreferencesRepository = FakeUserPreferencesRepository(UserPreferences(apiToken = apiToken)),
                deviceTokenRepository = FakeDeviceTokenRepository(),
                contentFeedRepository = ContentFeedRepository(fakeAnnouncementDao, fakeBlogPostDao),
                announcementRepository = AnnouncementRepository(fakeAnnouncementDao),
                blogPostRepository = BlogPostRepository(fakeBlogPostDao),
            ),
            fakeAnnouncementDao,
        )
    }

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
) : TrmnlApiService {
    override suspend fun getDevices(authorization: String) = devicesResponse

    override suspend fun getDevice(
        id: Int,
        authorization: String,
    ): ApiResult<DeviceResponse, ApiError> = throw NotImplementedError()

    override suspend fun getDisplayCurrent(deviceApiKey: String): ApiResult<Display, ApiError> =
        ApiResult.success(Display(200, 300, null, null, null))

    override suspend fun userInfo(authorization: String): ApiResult<UserResponse, ApiError> = throw NotImplementedError()

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int?,
        perPage: Int?,
    ): ApiResult<RecipesResponse, ApiError> = throw NotImplementedError()

    override suspend fun getRecipe(id: Int): ApiResult<RecipeDetailResponse, ApiError> = throw NotImplementedError()

    override suspend fun getDeviceModels(authorization: String): ApiResult<DeviceModelsResponse, ApiError> = throw NotImplementedError()
}

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

    override suspend fun setOnboardingCompleted() {}

    override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {}

    override suspend fun setLowBatteryNotificationEnabled(enabled: Boolean) {}

    override suspend fun setLowBatteryThreshold(percent: Int) {}

    override suspend fun setRssFeedContentEnabled(enabled: Boolean) {}

    override suspend fun setRssFeedContentNotificationEnabled(enabled: Boolean) {}

    override suspend fun setAnnouncementAuthBannerDismissed(dismissed: Boolean) {}

    override suspend fun setSecurityEnabled(enabled: Boolean) {}

    override suspend fun clearAll() {}
}

private class FakeDeviceTokenRepository : DeviceTokenRepository {
    private val tokens = mutableMapOf<String, String>()

    override suspend fun saveDeviceToken(
        deviceFriendlyId: String,
        token: String,
    ) {
        tokens[deviceFriendlyId] = token
    }

    override suspend fun getDeviceToken(deviceFriendlyId: String) = tokens[deviceFriendlyId]

    override fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?> = flowOf(tokens[deviceFriendlyId])

    override suspend fun clearDeviceToken(deviceFriendlyId: String) {
        tokens.remove(deviceFriendlyId)
    }

    override suspend fun hasDeviceToken(deviceFriendlyId: String) = tokens.containsKey(deviceFriendlyId)

    override suspend fun clearAll() {
        tokens.clear()
    }
}

private class FakeAnnouncementDao : AnnouncementDao {
    override fun getAll() = flowOf(emptyList<AnnouncementEntity>())

    override fun getLatest(limit: Int) = flowOf(emptyList<AnnouncementEntity>())

    override fun getUnread() = flowOf(emptyList<AnnouncementEntity>())

    override fun getRead() = flowOf(emptyList<AnnouncementEntity>())

    override suspend fun insertAll(announcements: List<AnnouncementEntity>) {}

    override suspend fun markAsRead(id: String) {}

    override suspend fun markAsUnread(id: String) {}

    override suspend fun markAllAsRead() {}

    override suspend fun deleteOlderThan(threshold: Long) {}

    override fun getUnreadCount() = flowOf(0)
}

private class FakeBlogPostDao : BlogPostDao {
    override fun getAll() = flowOf(emptyList<BlogPostEntity>())

    override fun getByCategory(category: String) = flowOf(emptyList<BlogPostEntity>())

    override fun getFavorites() = flowOf(emptyList<BlogPostEntity>())

    override fun getUnread() = flowOf(emptyList<BlogPostEntity>())

    override fun getRecentlyRead() = flowOf(emptyList<BlogPostEntity>())

    override suspend fun insertAll(posts: List<BlogPostEntity>) {}

    override suspend fun markAsRead(id: String) {}

    override suspend fun markAllAsRead(timestamp: java.time.Instant) {}

    override fun getUnreadCount() = flowOf(0)

    override suspend fun updateReadingProgress(
        id: String,
        progress: Float,
        timestamp: java.time.Instant,
    ) {}

    override suspend fun toggleFavorite(id: String) {}

    override suspend fun updateSummary(
        id: String,
        summary: String,
    ) {}

    override suspend fun deleteOlderThan(threshold: Long) {}

    override fun searchPosts(query: String) = flowOf(emptyList<BlogPostEntity>())
}
