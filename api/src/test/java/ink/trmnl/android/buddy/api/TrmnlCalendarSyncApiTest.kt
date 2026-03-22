package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.CalendarEvent
import ink.trmnl.android.buddy.api.models.CalendarSyncRequest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

/**
 * Unit tests for TrmnlApiService calendar sync endpoint using MockWebServer.
 *
 * Tests cover:
 * - Successful sync response (204 No Content)
 * - HTTP error responses (4xx, 5xx)
 * - Network failures
 * - Request body and header verification
 */
class TrmnlCalendarSyncApiTest : BaseApiTest() {
    @Test
    fun `syncCalendarEvents returns success with single event`() =
        runTest {
            // Given: Mock server returns 204 No Content
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Sync a single calendar event
            val request =
                CalendarSyncRequest(
                    events =
                        listOf(
                            CalendarEvent(
                                title = "Team Meeting",
                                startTime = "2025-01-15T09:00:00Z",
                                endTime = "2025-01-15T10:00:00Z",
                                location = "Conference Room A",
                                description = "Weekly sync",
                                allDay = false,
                            ),
                        ),
                )
            val result = apiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)

            // Verify request details
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.path).isEqualTo("/calendar/sync")
            assertThat(recordedRequest.method).isEqualTo("POST")
            assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test_token")
        }

    @Test
    fun `syncCalendarEvents returns success with multiple events`() =
        runTest {
            // Given: Mock server returns 204 No Content
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Sync multiple calendar events
            val request =
                CalendarSyncRequest(
                    events =
                        listOf(
                            CalendarEvent(
                                title = "Morning Standup",
                                startTime = "2025-01-15T09:00:00Z",
                                endTime = "2025-01-15T09:15:00Z",
                            ),
                            CalendarEvent(
                                title = "All Day Workshop",
                                startTime = "2025-01-16T00:00:00Z",
                                endTime = "2025-01-16T23:59:59Z",
                                allDay = true,
                            ),
                        ),
                )
            val result = apiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)

            // Verify request path and method
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.path).isEqualTo("/calendar/sync")
            assertThat(recordedRequest.method).isEqualTo("POST")
        }

    @Test
    fun `syncCalendarEvents returns success with empty events list`() =
        runTest {
            // Given: Mock server returns 204 No Content
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Sync with empty events list
            val request = CalendarSyncRequest(events = emptyList())
            val result = apiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)
        }

    @Test
    fun `syncCalendarEvents handles 401 unauthorized`() =
        runTest {
            // Given: Mock server returns 401
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}"""),
            )

            // When: Sync with invalid token
            val request = CalendarSyncRequest(events = emptyList())
            val result = apiService.syncCalendarEvents("Bearer invalid_token", request)

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(401)
        }

    @Test
    fun `syncCalendarEvents handles 422 validation error`() =
        runTest {
            // Given: Mock server returns 422
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(422)
                    .setBody("""{"error": "Validation failed"}"""),
            )

            // When: Sync with invalid request data
            val request = CalendarSyncRequest(events = emptyList())
            val result = apiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(422)
        }

    @Test
    fun `syncCalendarEvents handles 500 server error`() =
        runTest {
            // Given: Mock server returns 500
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"error": "Internal server error"}"""),
            )

            // When: Sync calendar events
            val request = CalendarSyncRequest(events = emptyList())
            val result = apiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(500)
        }

    @Test
    fun `syncCalendarEvents handles network timeout`() =
        runTest {
            // Given: Mock server with slow response (simulates timeout)
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("{}")
                    .setBodyDelay(10, java.util.concurrent.TimeUnit.SECONDS),
            )

            // When: Sync with short timeout client
            val timeoutApiService = ApiServiceTestHelper.createApiServiceWithTimeout(mockWebServer, json, 1)
            val request = CalendarSyncRequest(events = emptyList())
            val result = timeoutApiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify network failure
            assertThat(result).isInstanceOf(ApiResult.Failure.NetworkFailure::class)
        }

    @Test
    fun `verify authorization header is sent correctly for syncCalendarEvents`() =
        runTest {
            // Given: Mock server returns success
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Call with specific token
            val testToken = "Bearer user_test456"
            val request = CalendarSyncRequest(events = emptyList())
            apiService.syncCalendarEvents(testToken, request)

            // Then: Verify Authorization header
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Authorization")).isEqualTo(testToken)
        }

    @Test
    fun `verify content-type header is sent correctly for syncCalendarEvents`() =
        runTest {
            // Given: Mock server returns success
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Call syncCalendarEvents
            val request =
                CalendarSyncRequest(
                    events =
                        listOf(
                            CalendarEvent(
                                title = "Event",
                                startTime = "2025-01-15T09:00:00Z",
                                endTime = "2025-01-15T10:00:00Z",
                            ),
                        ),
                )
            apiService.syncCalendarEvents("Bearer test_token", request)

            // Then: Verify Content-Type header
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8")
        }
}
