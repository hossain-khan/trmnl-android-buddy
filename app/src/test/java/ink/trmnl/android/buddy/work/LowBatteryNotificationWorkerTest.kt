package ink.trmnl.android.buddy.work

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import ink.trmnl.android.buddy.notification.NotificationHelper
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.IOException

/**
 * Unit tests for [LowBatteryNotificationWorker].
 *
 * Tests verify:
 * - Battery threshold logic (below/above/exactly at threshold)
 * - Notification posting for low battery devices
 * - API result handling (Success, HttpFailure, NetworkFailure, ApiFailure, UnknownFailure)
 * - Preferences handling (enabled/disabled, no token, custom thresholds)
 * - Edge cases (no devices, authentication errors, etc.)
 *
 * Uses Robolectric for Android framework testing and fakes for dependencies.
 */
@RunWith(RobolectricTestRunner::class)
class LowBatteryNotificationWorkerTest {
    private lateinit var context: Context
    private lateinit var fakeApiService: FakeTrmnlApiService
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var notificationManager: NotificationManager
    private lateinit var workerParams: androidx.work.WorkerParameters

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeApiService = FakeTrmnlApiService()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channels
        NotificationHelper.createNotificationChannels(context)

        // Create WorkerParameters by building a simple test worker
        // We use a concrete Worker implementation to get valid WorkerParameters
        class DummyWorker(
            context: Context,
            params: androidx.work.WorkerParameters,
        ) : androidx.work.Worker(context, params) {
            override fun doWork(): Result = Result.success()
        }

        val dummyWorker = TestListenableWorkerBuilder<DummyWorker>(context).build()

        // Extract WorkerParameters using reflection
        val paramsField = androidx.work.ListenableWorker::class.java.getDeclaredField("mWorkerParams")
        paramsField.isAccessible = true
        workerParams = paramsField.get(dummyWorker) as androidx.work.WorkerParameters
    }

    private suspend fun runWorker(): ListenableWorker.Result {
        // Create our worker with fake dependencies and test WorkerParameters
        val worker =
            LowBatteryNotificationWorker(
                context = context,
                params = workerParams,
                apiService = fakeApiService,
                userPreferencesRepository = fakeUserPreferencesRepository,
            )
        return worker.doWork()
    }

    private fun createDevice(
        id: Int = 1,
        name: String = "Test Device",
        percentCharged: Double = 50.0,
    ): Device =
        Device(
            id = id,
            name = name,
            friendlyId = "ABC-$id",
            macAddress = "00:11:22:33:44:$id",
            batteryVoltage = 3.7,
            rssi = -50,
            percentCharged = percentCharged,
            wifiStrength = 80.0,
        )

    // ========================================
    // Core Functionality Tests
    // ========================================

    @Test
    fun `doWork returns success when notifications disabled`() =
        runTest {
            // Given: Notifications are disabled
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = false,
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns success without calling API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeApiService.getDevicesCallCount).isEqualTo(0)
        }

    @Test
    fun `doWork returns success when no API token`() =
        runTest {
            // Given: No API token
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = null,
                        isLowBatteryNotificationEnabled = true,
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns success without calling API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeApiService.getDevicesCallCount).isEqualTo(0)
        }

    @Test
    fun `doWork returns success when blank API token`() =
        runTest {
            // Given: Blank API token
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "   ",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns success without calling API
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeApiService.getDevicesCallCount).isEqualTo(0)
        }

    @Test
    fun `doWork fetches devices with correct authorization header`() =
        runTest {
            // Given: Valid preferences and API setup
            val apiToken = "user_test123"
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = apiToken,
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(data = emptyList()),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Calls API with correct auth header
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeApiService.lastAuthorizationHeader).isEqualTo("Bearer $apiToken")
            assertThat(fakeApiService.getDevicesCallCount).isEqualTo(1)
        }

    @Test
    fun `doWork returns success with no devices`() =
        runTest {
            // Given: API returns empty device list
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(data = emptyList()),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns success without posting notification
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(0)
        }

    // ========================================
    // Battery Threshold Logic Tests
    // ========================================

    @Test
    fun `posts notification for device below 20 percent threshold`() =
        runTest {
            // Given: Device with 15% battery (default threshold is 20%)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Low Battery Device", percentCharged = 15.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success and notification posted
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(1)
        }

    @Test
    fun `does not post notification for device above 20 percent threshold`() =
        runTest {
            // Given: Device with 25% battery (above 20% threshold)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Good Battery Device", percentCharged = 25.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success but no notification
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(0)
        }

    @Test
    fun `does not post notification for device at exactly 20 percent threshold`() =
        runTest {
            // Given: Device with exactly 20% battery (at threshold, not below)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "At Threshold Device", percentCharged = 20.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success but no notification (threshold not crossed)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(0)
        }

    @Test
    fun `posts notification for multiple low battery devices`() =
        runTest {
            // Given: Multiple devices below threshold
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Device 1", percentCharged = 15.0),
                                createDevice(id = 2, name = "Device 2", percentCharged = 10.0),
                                createDevice(id = 3, name = "Device 3", percentCharged = 5.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success and single aggregated notification posted
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(1)
        }

    @Test
    fun `posts notification only for devices below threshold when mixed battery levels`() =
        runTest {
            // Given: Some devices below, some above threshold
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Low Device", percentCharged = 15.0),
                                createDevice(id = 2, name = "Good Device", percentCharged = 50.0),
                                createDevice(id = 3, name = "Another Low", percentCharged = 10.0),
                                createDevice(id = 4, name = "High Device", percentCharged = 90.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success and notification posted (for 2 low devices)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(1)
        }

    @Test
    fun `respects custom threshold of 30 percent`() =
        runTest {
            // Given: Custom 30% threshold
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 30,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Device at 25%", percentCharged = 25.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Notification posted (25% is below 30% threshold)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(1)
        }

    @Test
    fun `respects custom threshold of 10 percent`() =
        runTest {
            // Given: Custom 10% threshold
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 10,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Device at 15%", percentCharged = 15.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: No notification (15% is above 10% threshold)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(0)
        }

    // ========================================
    // Edge Case Battery Values
    // ========================================

    @Test
    fun `handles zero percent battery`() =
        runTest {
            // Given: Device with 0% battery
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Dead Device", percentCharged = 0.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success and notification posted
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(1)
        }

    @Test
    fun `handles 100 percent battery`() =
        runTest {
            // Given: Device with 100% battery
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Full Device", percentCharged = 100.0),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success but no notification
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(0)
        }

    @Test
    fun `handles fractional battery percentage`() =
        runTest {
            // Given: Device with 19.5% battery (below 20% threshold)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                        lowBatteryThresholdPercent = 20,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.success(
                    DevicesResponse(
                        data =
                            listOf(
                                createDevice(id = 1, name = "Fractional Device", percentCharged = 19.5),
                            ),
                    ),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Success and notification posted
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(getPostedNotificationCount()).isEqualTo(1)
        }

    // ========================================
    // API Error Handling Tests
    // ========================================

    @Test
    fun `returns retry on HTTP 500 error`() =
        runTest {
            // Given: API returns HTTP 500 error
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.httpFailure(
                    code = 500,
                    error = ApiError(error = "Internal Server Error"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns retry (temporary HTTP error)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `returns retry on HTTP 503 error`() =
        runTest {
            // Given: API returns HTTP 503 error (service unavailable)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.httpFailure(
                    code = 503,
                    error = ApiError(error = "Service Unavailable"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns retry
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `returns failure on HTTP 401 error`() =
        runTest {
            // Given: API returns HTTP 401 error (unauthorized)
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "invalid_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.httpFailure(
                    code = 401,
                    error = ApiError(error = "Unauthorized"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns failure (authentication issue, won't resolve with retry)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    @Test
    fun `returns retry on HTTP 404 error`() =
        runTest {
            // Given: API returns HTTP 404 error
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.httpFailure(
                    code = 404,
                    error = ApiError(error = "Not Found"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns retry (temporary issue)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `returns retry on network failure`() =
        runTest {
            // Given: Network failure
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.networkFailure(
                    IOException("Network connection failed"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns retry (network issues are temporary)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    @Test
    fun `returns failure on API failure`() =
        runTest {
            // Given: API-specific failure
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.apiFailure(
                    ApiError(error = "API Error"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns failure
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    @Test
    fun `returns failure on unknown failure`() =
        runTest {
            // Given: Unknown failure
            fakeUserPreferencesRepository =
                FakeUserPreferencesRepository(
                    UserPreferences(
                        apiToken = "test_token",
                        isLowBatteryNotificationEnabled = true,
                    ),
                )
            fakeApiService.getDevicesResult =
                ApiResult.unknownFailure(
                    RuntimeException("Something went wrong"),
                )

            // When: Worker executes
            val result = runWorker()

            // Then: Returns failure
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    // ========================================
    // Helper Methods
    // ========================================

    private fun getPostedNotificationCount(): Int {
        val shadowNotificationManager = shadowOf(notificationManager)
        return shadowNotificationManager.allNotifications.size
    }
}
