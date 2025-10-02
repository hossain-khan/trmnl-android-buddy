package ink.trmnl.android.buddy.api

import com.slack.eithernet.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Unit tests for TrmnlApiService using MockWebServer.
 * 
 * These tests verify that the Retrofit service correctly handles:
 * - Successful responses
 * - HTTP error responses (4xx, 5xx)
 * - Network failures
 * - JSON parsing
 */
class TrmnlApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TrmnlApiService

    private val json = Json {
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
        
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(
                com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
            )
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .addCallAdapterFactory(
                com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
            )
            .build()

        apiService = retrofit.create(TrmnlApiService::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getDevices returns success with device list`() = runTest {
        // Given: Mock server returns successful response with devices
        val responseBody = """
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
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getDevices
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify success result with correct data
        assertTrue("Result should be Success", result is ApiResult.Success)
        val successResult = result as ApiResult.Success
        
        assertEquals(2, successResult.value.data.size)
        
        val firstDevice = successResult.value.data[0]
        assertEquals(12345, firstDevice.id)
        assertEquals("Kitchen Display", firstDevice.name)
        assertEquals("ABC123", firstDevice.friendlyId)
        assertEquals(3.8, firstDevice.batteryVoltage ?: 0.0, 0.01)
        
        val secondDevice = successResult.value.data[1]
        assertEquals(67890, secondDevice.id)
        assertEquals("Living Room Display", secondDevice.name)

        // Verify request was made correctly
        val request = mockWebServer.takeRequest()
        assertEquals("/devices", request.path)
        assertEquals("GET", request.method)
        assertEquals("Bearer test_token", request.getHeader("Authorization"))
    }

    @Test
    fun `getDevices returns empty list when no devices`() = runTest {
        // Given: Mock server returns empty device list
        val responseBody = """{"data": []}"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getDevices
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify success with empty list
        assertTrue(result is ApiResult.Success)
        val successResult = result as ApiResult.Success
        assertTrue(successResult.value.data.isEmpty())
    }

    @Test
    fun `getDevice returns success with single device`() = runTest {
        // Given: Mock server returns single device
        val responseBody = """
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
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getDevice
        val result = apiService.getDevice(12345, "Bearer test_token")

        // Then: Verify success result with correct device
        assertTrue(result is ApiResult.Success)
        val successResult = result as ApiResult.Success
        
        assertEquals(12345, successResult.value.data.id)
        assertEquals("Kitchen Display", successResult.value.data.name)
        assertEquals("ABC123", successResult.value.data.friendlyId)

        // Verify request
        val request = mockWebServer.takeRequest()
        assertEquals("/devices/12345", request.path)
        assertEquals("GET", request.method)
    }

    @Test
    fun `getDevices handles 401 unauthorized`() = runTest {
        // Given: Mock server returns 401
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
        )

        // When: Call getDevices
        val result = apiService.getDevices("Bearer invalid_token")

        // Then: Verify HTTP failure
        assertTrue(result is ApiResult.Failure.HttpFailure)
        val failure = result as ApiResult.Failure.HttpFailure
        assertEquals(401, failure.code)
    }

    @Test
    fun `getDevices handles 404 not found`() = runTest {
        // Given: Mock server returns 404
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Not found"}""")
        )

        // When: Call getDevices
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify HTTP failure
        assertTrue(result is ApiResult.Failure.HttpFailure)
        val failure = result as ApiResult.Failure.HttpFailure
        assertEquals(404, failure.code)
    }

    @Test
    fun `getDevices handles 500 server error`() = runTest {
        // Given: Mock server returns 500
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "Internal server error"}""")
        )

        // When: Call getDevices
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify HTTP failure
        assertTrue(result is ApiResult.Failure.HttpFailure)
        val failure = result as ApiResult.Failure.HttpFailure
        assertEquals(500, failure.code)
    }

    @Test
    fun `getDevices handles network timeout`() = runTest {
        // Given: Mock server with slow response (simulates timeout)
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"data": []}""")
                .setBodyDelay(10, java.util.concurrent.TimeUnit.SECONDS)
        )

        // When: Call getDevices with short timeout client
        val shortTimeoutClient = OkHttpClient.Builder()
            .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val shortTimeoutRetrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(shortTimeoutClient)
            .addConverterFactory(
                com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
            )
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .addCallAdapterFactory(
                com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
            )
            .build()

        val timeoutApiService = shortTimeoutRetrofit.create(TrmnlApiService::class.java)
        val result = timeoutApiService.getDevices("Bearer test_token")

        // Then: Verify network failure
        assertTrue(result is ApiResult.Failure.NetworkFailure)
    }

    @Test
    fun `getDevices handles malformed JSON`() = runTest {
        // Given: Mock server returns invalid JSON
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [this is not valid json]}""")
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getDevices
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify unknown failure (parsing error)
        assertTrue(result is ApiResult.Failure.UnknownFailure)
    }

    @Test
    fun `verify authorization header is sent correctly`() = runTest {
        // Given: Mock server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": []}""")
        )

        // When: Call with specific token
        val testToken = "Bearer user_test123"
        apiService.getDevices(testToken)

        // Then: Verify header
        val request = mockWebServer.takeRequest()
        assertEquals(testToken, request.getHeader("Authorization"))
    }

    @Test
    fun `device parsing validates all fields`() = runTest {
        // Given: Response with all device fields
        val responseBody = """
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
                .setBody(responseBody)
        )

        // When: Parse response
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify all fields parsed correctly
        assertTrue(result is ApiResult.Success)
        val device = (result as ApiResult.Success).value.data[0]
        
        assertEquals(999, device.id)
        assertEquals("Test Device", device.name)
        assertEquals("TEST99", device.friendlyId)
        assertEquals("11:22:33:44:55:66", device.macAddress)
        assertEquals(3.5, device.batteryVoltage ?: 0.0, 0.01)
        assertEquals(-50, device.rssi)
        assertEquals(50.0, device.percentCharged, 0.01)
        assertEquals(60.0, device.wifiStrength, 0.01)
    }

    @Test
    fun `device health methods work correctly`() = runTest {
        // Given: Device with low battery
        val responseBody = """
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
                .setBody(responseBody)
        )

        // When: Get device
        val result = apiService.getDevices("Bearer test_token")

        // Then: Verify health methods
        assertTrue(result is ApiResult.Success)
        val device = (result as ApiResult.Success).value.data[0]
        
        assertTrue("Battery should be low", device.isBatteryLow())
        assertTrue("WiFi should be weak", device.isWifiWeak())
        assertEquals("Low", device.getBatteryStatus())
        assertEquals("Weak", device.getWifiStatus())
    }

    // ========================================
    // /me Endpoint Tests
    // ========================================

    @Test
    fun `userInfo returns success with user data`() = runTest {
        // Given: Mock server returns successful user response
        val responseBody = """
            {
              "data": {
                "name": "Jim Bob",
                "email": "jimbob@gmail.net",
                "first_name": "Jim",
                "last_name": "Bob",
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
                .addHeader("Content-Type", "application/json")
        )

        // When: Call userInfo
        val result = apiService.userInfo("Bearer user_abc123")

        // Then: Verify success result with correct user data
        assertTrue("Result should be Success", result is ApiResult.Success)
        val successResult = result as ApiResult.Success

        val user = successResult.value.data
        assertEquals("Jim Bob", user.name)
        assertEquals("jimbob@gmail.net", user.email)
        assertEquals("Jim", user.firstName)
        assertEquals("Bob", user.lastName)
        assertEquals("en", user.locale)
        assertEquals("Eastern Time (US & Canada)", user.timeZone)
        assertEquals("America/New_York", user.timeZoneIana)
        assertEquals(-14400, user.utcOffset)
        assertEquals("user_abc123", user.apiKey)

        // Verify request was made correctly
        val request = mockWebServer.takeRequest()
        assertEquals("/me", request.path)
        assertEquals("GET", request.method)
        assertEquals("Bearer user_abc123", request.getHeader("Authorization"))
    }

    @Test
    fun `userInfo returns 401 unauthorized with invalid token`() = runTest {
        // Given: Mock server returns 401 Unauthorized
        val errorBody = """{"error": "Unauthorized"}"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody(errorBody)
                .addHeader("Content-Type", "application/json")
        )

        // When: Call userInfo with invalid token
        val result = apiService.userInfo("Bearer invalid_token")

        // Then: Verify HttpFailure with 401 status
        assertTrue("Result should be HttpFailure", result is ApiResult.Failure.HttpFailure)
        val failureResult = result as ApiResult.Failure.HttpFailure
        assertEquals(401, failureResult.code)
    }

    @Test
    fun `userInfo handles different time zones correctly`() = runTest {
        // Given: Mock server returns user with Pacific time zone
        val responseBody = """
            {
              "data": {
                "name": "Alice Smith",
                "email": "alice@example.com",
                "first_name": "Alice",
                "last_name": "Smith",
                "locale": "en",
                "time_zone": "Pacific Time (US & Canada)",
                "time_zone_iana": "America/Los_Angeles",
                "utc_offset": -28800,
                "api_key": "user_xyz789"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // When: Call userInfo
        val result = apiService.userInfo("Bearer user_xyz789")

        // Then: Verify Pacific timezone data
        assertTrue(result is ApiResult.Success)
        val user = (result as ApiResult.Success).value.data

        assertEquals("Pacific Time (US & Canada)", user.timeZone)
        assertEquals("America/Los_Angeles", user.timeZoneIana)
        assertEquals(-28800, user.utcOffset) // PST offset
    }

    @Test
    fun `userInfo handles international users correctly`() = runTest {
        // Given: Mock server returns user with international locale and timezone
        val responseBody = """
            {
              "data": {
                "name": "Hans Müller",
                "email": "hans@example.de",
                "first_name": "Hans",
                "last_name": "Müller",
                "locale": "de",
                "time_zone": "Berlin",
                "time_zone_iana": "Europe/Berlin",
                "utc_offset": 3600,
                "api_key": "user_de123"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // When: Call userInfo
        val result = apiService.userInfo("Bearer user_de123")

        // Then: Verify international user data
        assertTrue(result is ApiResult.Success)
        val user = (result as ApiResult.Success).value.data

        assertEquals("Hans Müller", user.name)
        assertEquals("de", user.locale)
        assertEquals("Europe/Berlin", user.timeZoneIana)
        assertEquals(3600, user.utcOffset) // CET offset
    }

    @Test
    fun `userInfo handles server errors correctly`() = runTest {
        // Given: Mock server returns 500 Internal Server Error
        val errorBody = """{"error": "Internal server error"}"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody(errorBody)
        )

        // When: Call userInfo
        val result = apiService.userInfo("Bearer user_abc123")

        // Then: Verify HttpFailure with 500 status
        assertTrue("Result should be HttpFailure", result is ApiResult.Failure.HttpFailure)
        val failureResult = result as ApiResult.Failure.HttpFailure
        assertEquals(500, failureResult.code)
    }

    @Test
    fun `userInfo handles network failures`() = runTest {
        // Given: Mock server is shut down (simulating network failure)
        mockWebServer.shutdown()

        // When: Call userInfo
        val result = apiService.userInfo("Bearer user_abc123")

        // Then: Verify NetworkFailure
        assertTrue(
            "Result should be NetworkFailure or UnknownFailure",
            result is ApiResult.Failure.NetworkFailure || result is ApiResult.Failure.UnknownFailure
        )
    }

    @Test
    fun `userInfo parses all user fields correctly`() = runTest {
        // Given: Mock server returns complete user data with all fields
        val responseBody = """
            {
              "data": {
                "name": "Test User",
                "email": "test@example.com",
                "first_name": "Test",
                "last_name": "User",
                "locale": "en",
                "time_zone": "Central Time (US & Canada)",
                "time_zone_iana": "America/Chicago",
                "utc_offset": -21600,
                "api_key": "user_test123"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // When: Call userInfo
        val result = apiService.userInfo("Bearer user_test123")

        // Then: Verify all fields are parsed
        assertTrue(result is ApiResult.Success)
        val user = (result as ApiResult.Success).value.data

        assertNotNull("Name should not be null", user.name)
        assertNotNull("Email should not be null", user.email)
        assertNotNull("First name should not be null", user.firstName)
        assertNotNull("Last name should not be null", user.lastName)
        assertNotNull("Locale should not be null", user.locale)
        assertNotNull("Time zone should not be null", user.timeZone)
        assertNotNull("Time zone IANA should not be null", user.timeZoneIana)
        assertNotNull("API key should not be null", user.apiKey)
    }
}
