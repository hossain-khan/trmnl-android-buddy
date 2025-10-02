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
 * Unit tests for TrmnlApiService user endpoints using MockWebServer.
 *
 * These tests verify user-related API endpoints:
 * - GET /me - Get authenticated user info
 *
 * Tests cover:
 * - Successful responses
 * - HTTP error responses (4xx, 5xx)
 * - Network failures
 * - JSON parsing
 * - International users (different locales and timezones)
 */
class TrmnlUserApiTest {

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
