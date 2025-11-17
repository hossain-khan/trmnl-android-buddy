package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.api.models.DeviceResponse
import ink.trmnl.android.buddy.api.models.DevicesResponse
import ink.trmnl.android.buddy.api.models.User
import ink.trmnl.android.buddy.api.models.UserResponse
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TrmnlDeviceRepository.
 *
 * Tests the repository layer that wraps the API service,
 * verifying proper error handling, data transformation, and convenience methods.
 */
class TrmnlDeviceRepositoryTest : BaseApiTest() {
    private lateinit var repository: TrmnlDeviceRepository

    private val testApiKey = "user_test123"

    @Before
    override fun setUp() {
        super.setUp()
        repository = TrmnlDeviceRepository(apiService, testApiKey)
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
                      "id": 1,
                      "name": "Device 1",
                      "friendly_id": "DEV1",
                      "mac_address": "00:11:22:33:44:55",
                      "battery_voltage": 3.8,
                      "rssi": -50,
                      "percent_charged": 75.0,
                      "wifi_strength": 80.0
                    },
                    {
                      "id": 2,
                      "name": "Device 2",
                      "friendly_id": "DEV2",
                      "mac_address": "AA:BB:CC:DD:EE:FF",
                      "battery_voltage": 4.0,
                      "rssi": -40,
                      "percent_charged": 90.0,
                      "wifi_strength": 85.0
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

            // When: Get devices through repository
            val result = repository.getDevices()

            // Then: Verify success result with unwrapped device list
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val devices = (result as ApiResult.Success).value
            assertThat(devices).hasSize(2)
            assertThat(devices[0].id).isEqualTo(1)
            assertThat(devices[1].id).isEqualTo(2)

            // Verify auth header was sent
            val request = mockWebServer.takeRequest()
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $testApiKey")
        }

    @Test
    fun `getDevices propagates API failures`() =
        runTest {
            // Given: Mock server returns 401 error
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}"""),
            )

            // When: Get devices through repository
            val result = repository.getDevices()

            // Then: Verify failure is propagated
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(401)
        }

    @Test
    fun `getDevice returns success with single device`() =
        runTest {
            // Given: Mock server returns single device
            val deviceId = 123
            val responseBody =
                """
                {
                  "data": {
                    "id": $deviceId,
                    "name": "Test Device",
                    "friendly_id": "TEST",
                    "mac_address": "00:11:22:33:44:55",
                    "battery_voltage": 3.9,
                    "rssi": -45,
                    "percent_charged": 80.0,
                    "wifi_strength": 75.0
                  }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Get device through repository
            val result = repository.getDevice(deviceId)

            // Then: Verify success result with unwrapped device
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val device = (result as ApiResult.Success).value
            assertThat(device.id).isEqualTo(deviceId)
            assertThat(device.name).isEqualTo("Test Device")

            // Verify request path
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/devices/$deviceId")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $testApiKey")
        }

    @Test
    fun `getDevice propagates 404 not found error`() =
        runTest {
            // Given: Mock server returns 404
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"error": "Device not found"}"""),
            )

            // When: Get non-existent device
            val result = repository.getDevice(99999)

            // Then: Verify failure is propagated
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(404)
        }

    @Test
    fun `getDevicesWithLowBattery filters devices correctly`() =
        runTest {
            // Given: Mock server returns devices with mixed battery levels
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 1,
                      "name": "Low Battery",
                      "friendly_id": "LOW",
                      "mac_address": "00:11:22:33:44:55",
                      "battery_voltage": 3.3,
                      "rssi": -50,
                      "percent_charged": 15.0,
                      "wifi_strength": 80.0
                    },
                    {
                      "id": 2,
                      "name": "Good Battery",
                      "friendly_id": "GOOD",
                      "mac_address": "AA:BB:CC:DD:EE:FF",
                      "battery_voltage": 4.0,
                      "rssi": -40,
                      "percent_charged": 85.0,
                      "wifi_strength": 85.0
                    },
                    {
                      "id": 3,
                      "name": "Critical Battery",
                      "friendly_id": "CRIT",
                      "mac_address": "11:22:33:44:55:66",
                      "battery_voltage": 3.2,
                      "rssi": -45,
                      "percent_charged": 10.0,
                      "wifi_strength": 75.0
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

            // When: Get devices with low battery
            val result = repository.getDevicesWithLowBattery()

            // Then: Verify only low battery devices are returned
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val lowBatteryDevices = (result as ApiResult.Success).value
            assertThat(lowBatteryDevices).hasSize(2)
            assertThat(lowBatteryDevices[0].id).isEqualTo(1)
            assertThat(lowBatteryDevices[1].id).isEqualTo(3)
        }

    @Test
    fun `getDevicesWithLowBattery returns empty list when no low battery devices`() =
        runTest {
            // Given: Mock server returns devices with good battery
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 1,
                      "name": "Good Battery",
                      "friendly_id": "GOOD",
                      "mac_address": "00:11:22:33:44:55",
                      "battery_voltage": 4.0,
                      "rssi": -50,
                      "percent_charged": 80.0,
                      "wifi_strength": 80.0
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

            // When: Get devices with low battery
            val result = repository.getDevicesWithLowBattery()

            // Then: Verify empty list is returned
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val lowBatteryDevices = (result as ApiResult.Success).value
            assertThat(lowBatteryDevices).hasSize(0)
        }

    @Test
    fun `getDevicesWithLowBattery propagates API errors`() =
        runTest {
            // Given: Mock server returns error
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"error": "Internal server error"}"""),
            )

            // When: Get devices with low battery
            val result = repository.getDevicesWithLowBattery()

            // Then: Verify failure is propagated
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        }

    @Test
    fun `getDevicesWithWeakWifi filters devices correctly`() =
        runTest {
            // Given: Mock server returns devices with mixed WiFi strength
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 1,
                      "name": "Weak WiFi",
                      "friendly_id": "WEAK",
                      "mac_address": "00:11:22:33:44:55",
                      "battery_voltage": 3.8,
                      "rssi": -80,
                      "percent_charged": 75.0,
                      "wifi_strength": 25.0
                    },
                    {
                      "id": 2,
                      "name": "Strong WiFi",
                      "friendly_id": "STRONG",
                      "mac_address": "AA:BB:CC:DD:EE:FF",
                      "battery_voltage": 4.0,
                      "rssi": -40,
                      "percent_charged": 85.0,
                      "wifi_strength": 90.0
                    },
                    {
                      "id": 3,
                      "name": "Poor WiFi",
                      "friendly_id": "POOR",
                      "mac_address": "11:22:33:44:55:66",
                      "battery_voltage": 3.9,
                      "rssi": -85,
                      "percent_charged": 80.0,
                      "wifi_strength": 15.0
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

            // When: Get devices with weak WiFi
            val result = repository.getDevicesWithWeakWifi()

            // Then: Verify only weak WiFi devices are returned
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val weakWifiDevices = (result as ApiResult.Success).value
            assertThat(weakWifiDevices).hasSize(2)
            assertThat(weakWifiDevices[0].id).isEqualTo(1)
            assertThat(weakWifiDevices[1].id).isEqualTo(3)
        }

    @Test
    fun `getDevicesWithWeakWifi returns empty list when all WiFi is strong`() =
        runTest {
            // Given: Mock server returns devices with strong WiFi
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 1,
                      "name": "Strong WiFi",
                      "friendly_id": "STRONG",
                      "mac_address": "00:11:22:33:44:55",
                      "battery_voltage": 4.0,
                      "rssi": -40,
                      "percent_charged": 80.0,
                      "wifi_strength": 90.0
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

            // When: Get devices with weak WiFi
            val result = repository.getDevicesWithWeakWifi()

            // Then: Verify empty list is returned
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val weakWifiDevices = (result as ApiResult.Success).value
            assertThat(weakWifiDevices).hasSize(0)
        }

    @Test
    fun `getDevicesWithWeakWifi propagates API errors`() =
        runTest {
            // Given: Mock server returns error
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}"""),
            )

            // When: Get devices with weak WiFi
            val result = repository.getDevicesWithWeakWifi()

            // Then: Verify failure is propagated
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        }

    @Test
    fun `userInfo returns success with user data`() =
        runTest {
            // Given: Mock server returns user data
            val responseBody =
                """
                {
                  "data": {
                    "name": "John Doe",
                    "email": "john@example.com",
                    "first_name": "John",
                    "last_name": "Doe",
                    "locale": "en",
                    "time_zone": "Eastern Time (US & Canada)",
                    "time_zone_iana": "America/New_York",
                    "utc_offset": -14400,
                    "api_key": "user_abc123"
                  }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Get user info through repository
            val result = repository.userInfo()

            // Then: Verify success result with unwrapped user data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val user = (result as ApiResult.Success).value
            assertThat(user.name).isEqualTo("John Doe")
            assertThat(user.email).isEqualTo("john@example.com")
            assertThat(user.firstName).isEqualTo("John")
            assertThat(user.lastName).isEqualTo("Doe")

            // Verify auth header was sent
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/me")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $testApiKey")
        }

    @Test
    fun `userInfo propagates API failures`() =
        runTest {
            // Given: Mock server returns 401 error
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Invalid API key"}"""),
            )

            // When: Get user info through repository
            val result = repository.userInfo()

            // Then: Verify failure is propagated
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(401)
        }
}
