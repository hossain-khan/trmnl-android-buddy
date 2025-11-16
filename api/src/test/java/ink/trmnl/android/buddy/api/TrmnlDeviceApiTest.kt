package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Unit tests for TrmnlApiService device endpoints using MockWebServer.
 *
 * These tests verify device-related API endpoints:
 * - GET /devices - List all devices
 * - GET /devices/{id} - Get single device
 *
 * Tests cover:
 * - Successful responses
 * - HTTP error responses (4xx, 5xx)
 * - Network failures
 * - JSON parsing
 */
class TrmnlDeviceApiTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TrmnlApiService

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    @Before
    fun setup() {
        // Create and start MockWebServer
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
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getDevices returns success with device list`() =
        runTest {
            // Given: Mock server returns successful response with devices
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 12345,
                      "name": "Kitchen Display",
                      "friendly_id": "ABC123",
                      "mac_address": "00:11:22:33:44:55",
                      "battery_voltage": 3.8,
                      "rssi": -27,
                      "percent_charged": 66.67,
                      "wifi_strength": 100
                    },
                    {
                      "id": 67890,
                      "name": "Living Room Display",
                      "friendly_id": "XYZ789",
                      "mac_address": "AA:BB:CC:DD:EE:FF",
                      "battery_voltage": 4.1,
                      "rssi": -45,
                      "percent_charged": 85.0,
                      "wifi_strength": 75
                    }
                  ]
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getDevices
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify success result with correct data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            assertThat(successResult.value.data).hasSize(2)

            val firstDevice = successResult.value.data[0]
            assertThat(firstDevice.id).isEqualTo(12345)
            assertThat(firstDevice.name).isEqualTo("Kitchen Display")
            assertThat(firstDevice.friendlyId).isEqualTo("ABC123")
            assertThat(firstDevice.batteryVoltage ?: 0.0).isCloseTo(3.8, 0.01)

            val secondDevice = successResult.value.data[1]
            assertThat(secondDevice.id).isEqualTo(67890)
            assertThat(secondDevice.name).isEqualTo("Living Room Display")

            // Verify request was made correctly
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/devices")
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test_token")
        }

    @Test
    fun `getDevices returns empty list when no devices`() =
        runTest {
            // Given: Mock server returns empty device list
            val responseBody = """{"data": []}"""

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getDevices
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify success with empty list
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success
            assertThat(successResult.value.data).isEmpty()
        }

    @Test
    fun `getDevice returns success with single device`() =
        runTest {
            // Given: Mock server returns single device
            val responseBody =
                """
                {
                  "data": {
                    "id": 12345,
                    "name": "Kitchen Display",
                    "friendly_id": "ABC123",
                    "mac_address": "00:11:22:33:44:55",
                    "battery_voltage": 3.8,
                    "rssi": -27,
                    "percent_charged": 66.67,
                    "wifi_strength": 100
                  }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getDevice
            val result = apiService.getDevice(12345, "Bearer test_token")

            // Then: Verify success result with correct device
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            assertThat(successResult.value.data.id).isEqualTo(12345)
            assertThat(successResult.value.data.name).isEqualTo("Kitchen Display")
            assertThat(successResult.value.data.friendlyId).isEqualTo("ABC123")

            // Verify request
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/devices/12345")
            assertThat(request.method).isEqualTo("GET")
        }

    @Test
    fun `getDevices handles 401 unauthorized`() =
        runTest {
            // Given: Mock server returns 401
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}"""),
            )

            // When: Call getDevices
            val result = apiService.getDevices("Bearer invalid_token")

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(401)
        }

    @Test
    fun `getDevices handles 404 not found`() =
        runTest {
            // Given: Mock server returns 404
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"error": "Not found"}"""),
            )

            // When: Call getDevices
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(404)
        }

    @Test
    fun `getDevices handles 500 server error`() =
        runTest {
            // Given: Mock server returns 500
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"error": "Internal server error"}"""),
            )

            // When: Call getDevices
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(500)
        }

    @Test
    fun `getDevices handles network timeout`() =
        runTest {
            // Given: Mock server with slow response (simulates timeout)
            mockWebServer.enqueue(
                MockResponse()
                    .setBody("""{"data": []}""")
                    .setBodyDelay(10, java.util.concurrent.TimeUnit.SECONDS),
            )

            // When: Call getDevices with short timeout client
            val timeoutApiService = ApiServiceTestHelper.createApiServiceWithTimeout(mockWebServer, json, 1)
            val result = timeoutApiService.getDevices("Bearer test_token")

            // Then: Verify network failure
            assertThat(result).isInstanceOf(ApiResult.Failure.NetworkFailure::class)
        }

    @Test
    fun `getDevices handles malformed JSON`() =
        runTest {
            // Given: Mock server returns invalid JSON
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": [this is not valid json]}""")
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getDevices
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify unknown failure (parsing error)
            assertThat(result).isInstanceOf(ApiResult.Failure.UnknownFailure::class)
        }

    @Test
    fun `verify authorization header is sent correctly`() =
        runTest {
            // Given: Mock server
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": []}"""),
            )

            // When: Call with specific token
            val testToken = "Bearer user_test123"
            apiService.getDevices(testToken)

            // Then: Verify header
            val request = mockWebServer.takeRequest()
            assertThat(request.getHeader("Authorization")).isEqualTo(testToken)
        }

    @Test
    fun `device parsing validates all fields`() =
        runTest {
            // Given: Response with all device fields
            val responseBody =
                """
                {
                  "data": [{
                    "id": 999,
                    "name": "Test Device",
                    "friendly_id": "TEST99",
                    "mac_address": "11:22:33:44:55:66",
                    "battery_voltage": 3.5,
                    "rssi": -50,
                    "percent_charged": 50.0,
                    "wifi_strength": 60
                  }]
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody),
            )

            // When: Parse response
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify all fields parsed correctly
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val device = (result as ApiResult.Success).value.data[0]

            assertThat(device.id).isEqualTo(999)
            assertThat(device.name).isEqualTo("Test Device")
            assertThat(device.friendlyId).isEqualTo("TEST99")
            assertThat(device.macAddress).isEqualTo("11:22:33:44:55:66")
            assertThat(device.batteryVoltage ?: 0.0).isCloseTo(3.5, 0.01)
            assertThat(device.rssi).isEqualTo(-50)
            assertThat(device.percentCharged).isCloseTo(50.0, 0.01)
            assertThat(device.wifiStrength).isCloseTo(60.0, 0.01)
        }

    @Test
    fun `verify user agent header is sent correctly`() =
        runTest {
            // Given: Mock server
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": []}"""),
            )

            // Create API service with custom app version
            val appVersion = "1.6.0"
            val customApiService = ApiServiceTestHelper.createApiServiceWithAppVersion(mockWebServer, json, appVersion)

            // When: Call API
            customApiService.getDevices("Bearer test_token")

            // Then: Verify User-Agent header is present and contains expected values
            val request = mockWebServer.takeRequest()
            val userAgent = request.getHeader("User-Agent")

            assertThat(userAgent).isNotNull()
            assertThat(userAgent!!).contains("TrmnlAndroidBuddy")
            assertThat(userAgent).contains(appVersion)
            assertThat(userAgent).contains("Android")
            assertThat(userAgent).contains("OkHttp")
        }

    @Test
    fun `device health methods work correctly`() =
        runTest {
            // Given: Device with low battery
            val responseBody =
                """
                {
                  "data": [{
                    "id": 1,
                    "name": "Low Battery Device",
                    "friendly_id": "LOW001",
                    "mac_address": "00:00:00:00:00:00",
                    "battery_voltage": 3.3,
                    "rssi": -80,
                    "percent_charged": 15.0,
                    "wifi_strength": 20
                  }]
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody),
            )

            // When: Get device
            val result = apiService.getDevices("Bearer test_token")

            // Then: Verify health methods
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val device = (result as ApiResult.Success).value.data[0]

            assertThat(device.isBatteryLow()).isTrue()
            assertThat(device.isWifiWeak()).isTrue()
            assertThat(device.getBatteryStatus()).isEqualTo("Low")
            assertThat(device.getWifiStatus()).isEqualTo("Weak")
        }
}
