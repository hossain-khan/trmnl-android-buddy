package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

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
class TrmnlUserApiTest : BaseApiTest() {
    @Test
    fun `userInfo returns success with user data`() =
        runTest {
            // Given: Mock server returns successful user response
            val responseBody =
                """
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
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call userInfo
            val result = apiService.userInfo("Bearer user_abc123")

            // Then: Verify success result with correct user data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            val user = successResult.value.data
            assertThat(user.name).isEqualTo("Jim Bob")
            assertThat(user.email).isEqualTo("jimbob@gmail.net")
            assertThat(user.firstName).isEqualTo("Jim")
            assertThat(user.lastName).isEqualTo("Bob")
            assertThat(user.locale).isEqualTo("en")
            assertThat(user.timeZone).isEqualTo("Eastern Time (US & Canada)")
            assertThat(user.timeZoneIana).isEqualTo("America/New_York")
            assertThat(user.utcOffset).isEqualTo(-14400)
            assertThat(user.apiKey).isEqualTo("user_abc123")

            // Verify request was made correctly
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/me")
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer user_abc123")
        }

    @Test
    fun `userInfo returns 401 unauthorized with invalid token`() =
        runTest {
            // Given: Mock server returns 401 Unauthorized
            val errorBody = """{"error": "Unauthorized"}"""

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody(errorBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call userInfo with invalid token
            val result = apiService.userInfo("Bearer invalid_token")

            // Then: Verify HttpFailure with 401 status
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failureResult = result as ApiResult.Failure.HttpFailure
            assertThat(failureResult.code).isEqualTo(401)
        }

    @Test
    fun `userInfo handles different time zones correctly`() =
        runTest {
            // Given: Mock server returns user with Pacific time zone
            val responseBody =
                """
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
                    .setBody(responseBody),
            )

            // When: Call userInfo
            val result = apiService.userInfo("Bearer user_xyz789")

            // Then: Verify Pacific timezone data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val user = (result as ApiResult.Success).value.data

            assertThat(user.timeZone).isEqualTo("Pacific Time (US & Canada)")
            assertThat(user.timeZoneIana).isEqualTo("America/Los_Angeles")
            assertThat(user.utcOffset).isEqualTo(-28800) // PST offset
        }

    @Test
    fun `userInfo handles international users correctly`() =
        runTest {
            // Given: Mock server returns user with international locale and timezone
            val responseBody =
                """
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
                    .setBody(responseBody),
            )

            // When: Call userInfo
            val result = apiService.userInfo("Bearer user_de123")

            // Then: Verify international user data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val user = (result as ApiResult.Success).value.data

            assertThat(user.name).isEqualTo("Hans Müller")
            assertThat(user.locale).isEqualTo("de")
            assertThat(user.timeZoneIana).isEqualTo("Europe/Berlin")
            assertThat(user.utcOffset).isEqualTo(3600) // CET offset
        }

    @Test
    fun `userInfo handles server errors correctly`() =
        runTest {
            // Given: Mock server returns 500 Internal Server Error
            val errorBody = """{"error": "Internal server error"}"""

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody(errorBody),
            )

            // When: Call userInfo
            val result = apiService.userInfo("Bearer user_abc123")

            // Then: Verify HttpFailure with 500 status
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failureResult = result as ApiResult.Failure.HttpFailure
            assertThat(failureResult.code).isEqualTo(500)
        }

    @Test
    fun `userInfo handles network failures`() =
        runTest {
            // Given: Mock server is shut down (simulating network failure)
            mockWebServer.shutdown()

            // When: Call userInfo
            val result = apiService.userInfo("Bearer user_abc123")

            // Then: Verify NetworkFailure
            assertThat(result).isInstanceOf(ApiResult.Failure::class)
        }

    @Test
    fun `userInfo parses all user fields correctly`() =
        runTest {
            // Given: Mock server returns complete user data with all fields
            val responseBody =
                """
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
                    .setBody(responseBody),
            )

            // When: Call userInfo
            val result = apiService.userInfo("Bearer user_test123")

            // Then: Verify all fields are parsed
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val user = (result as ApiResult.Success).value.data

            assertThat(user.name).isNotNull()
            assertThat(user.email).isNotNull()
            assertThat(user.firstName).isNotNull()
            assertThat(user.lastName).isNotNull()
            assertThat(user.locale).isNotNull()
            assertThat(user.timeZone).isNotNull()
            assertThat(user.timeZoneIana).isNotNull()
            assertThat(user.apiKey).isNotNull()
        }
}
