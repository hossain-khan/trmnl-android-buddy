package ink.trmnl.android.buddy.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeBatteryHistoryRepository
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Comprehensive unit tests for [BatteryCollectionWorker].
 *
 * Tests cover:
 * - Successful battery data collection from API
 * - Data persistence to Room database
 * - API integration scenarios (all ApiResult types)
 * - Error handling (network, HTTP, auth failures)
 * - Edge cases (empty devices, null values, disabled tracking)
 *
 * Uses MockWebServer and fake implementations following Android testing best practices.
 */
@RunWith(RobolectricTestRunner::class)
class BatteryCollectionWorkerTest {
    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TrmnlApiService
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var fakeBatteryHistoryRepository: FakeBatteryHistoryRepository

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Setup MockWebServer for API calls
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Create Retrofit instance pointing to mock server
        val okHttpClient = OkHttpClient.Builder().build()

        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(okHttpClient)
                .addConverterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultConverterFactory,
                ).addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                ).addCallAdapterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory,
                ).build()

        apiService = retrofit.create(TrmnlApiService::class.java)

        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
        fakeBatteryHistoryRepository = FakeBatteryHistoryRepository()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun createWorker(): BatteryCollectionWorker =
        TestListenableWorkerBuilder<BatteryCollectionWorker>(context)
            .setWorkerFactory(
                TestBatteryCollectionWorkerFactory(
                    apiService,
                    fakeUserPreferencesRepository,
                    fakeBatteryHistoryRepository,
                ),
            ).build()

    // ========================================
    // Core Functionality Tests
    // ========================================

    @Test
    fun `successful battery data collection saves to database`() =
        runTest {
            // Given: User has API token and battery tracking enabled
            fakeUserPreferencesRepository.saveApiToken("test-token-123")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            // Given: API returns a device with battery data
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [{
                            "id": 1,
                            "name": "Kitchen Display",
                            "friendly_id": "ABC-123",
                            "mac_address": "00:11:22:33:44:55",
                            "battery_voltage": 3.8,
                            "rssi": -27,
                            "percent_charged": 66.67,
                            "wifi_strength": 100.0
                          }]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Worker succeeds
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: Battery data is saved to database
            val recordings = fakeBatteryHistoryRepository.recordedReadings
            assertThat(recordings).hasSize(1)
            assertThat(recordings[0].deviceId).isEqualTo("ABC-123")
            assertThat(recordings[0].percentCharged).isEqualTo(66.67)
            assertThat(recordings[0].batteryVoltage).isEqualTo(3.8)
        }

    @Test
    fun `multiple devices all get battery data recorded`() =
        runTest {
            // Given: User has API token and battery tracking enabled
            fakeUserPreferencesRepository.saveApiToken("test-token-456")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            // Given: API returns multiple devices
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [
                            {
                              "id": 1,
                              "name": "Device 1",
                              "friendly_id": "DEV-001",
                              "mac_address": "00:11:22:33:44:55",
                              "battery_voltage": 3.9,
                              "rssi": -50,
                              "percent_charged": 80.0,
                              "wifi_strength": 80.0
                            },
                            {
                              "id": 2,
                              "name": "Device 2",
                              "friendly_id": "DEV-002",
                              "mac_address": "AA:BB:CC:DD:EE:FF",
                              "battery_voltage": 3.5,
                              "rssi": -60,
                              "percent_charged": 45.0,
                              "wifi_strength": 70.0
                            },
                            {
                              "id": 3,
                              "name": "Device 3",
                              "friendly_id": "DEV-003",
                              "mac_address": "11:22:33:44:55:66",
                              "battery_voltage": 4.0,
                              "rssi": -40,
                              "percent_charged": 90.0,
                              "wifi_strength": 90.0
                            }
                          ]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Worker succeeds
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: All devices have battery data recorded
            val recordings = fakeBatteryHistoryRepository.recordedReadings
            assertThat(recordings).hasSize(3)
            assertThat(recordings[0].deviceId).isEqualTo("DEV-001")
            assertThat(recordings[0].percentCharged).isEqualTo(80.0)
            assertThat(recordings[1].deviceId).isEqualTo("DEV-002")
            assertThat(recordings[1].percentCharged).isEqualTo(45.0)
            assertThat(recordings[2].deviceId).isEqualTo("DEV-003")
            assertThat(recordings[2].percentCharged).isEqualTo(90.0)
        }

    @Test
    fun `device with null battery voltage is handled gracefully`() =
        runTest {
            // Given: User has API token and battery tracking enabled
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            // Given: Device has null battery voltage
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [{
                            "id": 1,
                            "name": "Test Device",
                            "friendly_id": "DEV-001",
                            "mac_address": "00:11:22:33:44:55",
                            "battery_voltage": null,
                            "rssi": -50,
                            "percent_charged": 75.0,
                            "wifi_strength": 80.0
                          }]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val worker = createWorker()
            val result = worker.doWork()

            // Then: Worker succeeds
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: Battery data is saved with null voltage
            val recordings = fakeBatteryHistoryRepository.recordedReadings
            assertThat(recordings).hasSize(1)
            assertThat(recordings[0].batteryVoltage).isNull()
            assertThat(recordings[0].percentCharged).isEqualTo(75.0)
        }

    @Test
    fun `battery percentages at boundaries are recorded correctly`() =
        runTest {
            // Given: Devices with edge case battery percentages
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [
                            {
                              "id": 1,
                              "name": "Empty",
                              "friendly_id": "DEV-EMPTY",
                              "mac_address": "00:11:22:33:44:55",
                              "battery_voltage": 3.2,
                              "rssi": -50,
                              "percent_charged": 0.0,
                              "wifi_strength": 80.0
                            },
                            {
                              "id": 2,
                              "name": "Full",
                              "friendly_id": "DEV-FULL",
                              "mac_address": "AA:BB:CC:DD:EE:FF",
                              "battery_voltage": 4.2,
                              "rssi": -40,
                              "percent_charged": 100.0,
                              "wifi_strength": 90.0
                            },
                            {
                              "id": 3,
                              "name": "Low",
                              "friendly_id": "DEV-LOW",
                              "mac_address": "11:22:33:44:55:66",
                              "battery_voltage": 3.3,
                              "rssi": -60,
                              "percent_charged": 5.5,
                              "wifi_strength": 70.0
                            }
                          ]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: All percentages recorded correctly
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val recordings = fakeBatteryHistoryRepository.recordedReadings
            assertThat(recordings).hasSize(3)
            assertThat(recordings[0].percentCharged).isEqualTo(0.0)
            assertThat(recordings[1].percentCharged).isEqualTo(100.0)
            assertThat(recordings[2].percentCharged).isEqualTo(5.5)
        }

    // ========================================
    // Battery Tracking Configuration Tests
    // ========================================

    @Test
    fun `battery tracking disabled skips collection`() =
        runTest {
            // Given: Battery tracking is disabled
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(false)

            // When: Worker executes (no API call should be made)
            val result = createWorker().doWork()

            // Then: Worker succeeds without collecting data
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: No battery data is recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    @Test
    fun `missing API token skips collection`() =
        runTest {
            // Given: No API token set
            fakeUserPreferencesRepository.clearApiToken()
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker succeeds without collecting data
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: No battery data is recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    @Test
    fun `blank API token skips collection`() =
        runTest {
            // Given: Blank API token
            fakeUserPreferencesRepository.saveApiToken("   ")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker succeeds without collecting data
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: No battery data is recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    // ========================================
    // API Integration Tests - Success Cases
    // ========================================

    @Test
    fun `empty device list returns success`() =
        runTest {
            // Given: API returns empty device list
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": []}""")
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker succeeds
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)

            // Then: No battery data recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    // ========================================
    // API Integration Tests - HTTP Failures
    // ========================================

    @Test
    fun `HTTP 401 unauthorized returns failure`() =
        runTest {
            // Given: API returns 401 unauthorized
            fakeUserPreferencesRepository.saveApiToken("invalid-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}"""),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker returns failure (don't retry invalid token)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)

            // Then: No battery data recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    @Test
    fun `HTTP 404 not found returns retry`() =
        runTest {
            // Given: API returns 404
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"error": "Not found"}"""),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker returns retry (temporary error)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)

            // Then: No battery data recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    @Test
    fun `HTTP 500 server error returns retry`() =
        runTest {
            // Given: API returns 500 server error
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"error": "Internal server error"}"""),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker returns retry (temporary server error)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)

            // Then: No battery data recorded
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(0)
        }

    @Test
    fun `HTTP 503 service unavailable returns retry`() =
        runTest {
            // Given: API returns 503 service unavailable
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(503)
                    .setBody("""{"error": "Service unavailable"}"""),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker returns retry (service temporarily down)
            assertThat(result).isInstanceOf(ListenableWorker.Result.Retry::class.java)
        }

    // ========================================
    // Database Error Handling Tests
    // ========================================

    @Test
    fun `database write failure is propagated as exception`() =
        runTest {
            // Given: Database throws exception on write
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [{
                            "id": 1,
                            "name": "Test",
                            "friendly_id": "TEST-123",
                            "mac_address": "00:11:22:33:44:55",
                            "battery_voltage": 3.7,
                            "rssi": -50,
                            "percent_charged": 80.0,
                            "wifi_strength": 80.0
                          }]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            val throwingRepository = FakeBatteryHistoryRepository(shouldThrowOnRecord = true)

            // When: Worker executes with throwing repository
            val worker =
                TestListenableWorkerBuilder<BatteryCollectionWorker>(context)
                    .setWorkerFactory(
                        TestBatteryCollectionWorkerFactory(
                            apiService,
                            fakeUserPreferencesRepository,
                            throwingRepository,
                        ),
                    ).build()

            val result = worker.doWork()

            // Then: Worker returns failure
            assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
        }

    // ========================================
    // Edge Cases and Boundary Tests
    // ========================================

    @Test
    fun `device with null RSSI is handled`() =
        runTest {
            // Given: Device with null RSSI
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [{
                            "id": 1,
                            "name": "Test",
                            "friendly_id": "DEV-001",
                            "mac_address": "00:11:22:33:44:55",
                            "battery_voltage": 3.7,
                            "rssi": null,
                            "percent_charged": 75.0,
                            "wifi_strength": 80.0
                          }]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: Worker succeeds
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(fakeBatteryHistoryRepository.recordedReadings).hasSize(1)
        }

    @Test
    fun `timestamp is set correctly for all recordings`() =
        runTest {
            // Given: Multiple devices
            fakeUserPreferencesRepository.saveApiToken("test-token")
            fakeUserPreferencesRepository.setBatteryTrackingEnabled(true)

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "data": [
                            {
                              "id": 1, "name": "Dev1", "friendly_id": "DEV-001",
                              "mac_address": "00:11:22:33:44:55",
                              "battery_voltage": 3.7, "rssi": -50,
                              "percent_charged": 75.0, "wifi_strength": 80.0
                            },
                            {
                              "id": 2, "name": "Dev2", "friendly_id": "DEV-002",
                              "mac_address": "AA:BB:CC:DD:EE:FF",
                              "battery_voltage": 3.8, "rssi": -60,
                              "percent_charged": 80.0, "wifi_strength": 70.0
                            }
                          ]
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json"),
            )

            // When: Worker executes
            val result = createWorker().doWork()

            // Then: All recordings have timestamps
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            val recordings = fakeBatteryHistoryRepository.recordedReadings
            assertThat(recordings).hasSize(2)

            // All timestamps should be the same (recorded at same time)
            val firstTimestamp = recordings[0].timestamp
            assertThat(recordings[1].timestamp).isEqualTo(firstTimestamp)

            // Timestamp should be recent and positive
            assertThat(firstTimestamp).isNotNull()
            assertThat(firstTimestamp).isGreaterThan(0L)
        }
}
