package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.CalendarEvent
import ink.trmnl.android.buddy.api.models.CalendarSyncRequest
import ink.trmnl.android.buddy.api.models.MergeVariables
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

/**
 * Unit tests for TrmnlApiService calendar sync endpoints using MockWebServer.
 *
 * Tests cover the Companion app 3-step workflow API endpoints:
 * - GET /plugin_settings?plugin_id=calendars
 * - POST /plugin_settings/{id}/data
 *
 * Includes:
 * - Successful sync response (204 No Content)
 * - Successful plugin settings retrieval (200)
 * - HTTP error responses (4xx, 5xx)
 * - Network failures
 * - Request body and header verification
 */
class TrmnlCalendarSyncApiTest : BaseApiTest() {
    companion object {
        private const val TEST_SETTING_ID = 12345
    }

    // ========================================
    // GET /plugin_settings tests
    // ========================================

    @Test
    fun `getPluginSettings returns success with plugin settings`() =
        runTest {
            // Given: Mock server returns plugin settings
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("""{"data": [{"id": 12345, "name": "My Calendar", "plugin_id": 42}]}"""),
            )

            // When: Get plugin settings for calendars
            val result = apiService.getPluginSettings("calendars", "Bearer test_token")

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val success = result as ApiResult.Success
            assertThat(success.value.data.size).isEqualTo(1)
            assertThat(
                success.value.data
                    .first()
                    .id,
            ).isEqualTo(12345)

            // Verify request details
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.path!!).contains("/plugin_settings")
            assertThat(recordedRequest.path!!).contains("plugin_id=calendars")
            assertThat(recordedRequest.method).isEqualTo("GET")
            assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer test_token")
        }

    @Test
    fun `getPluginSettings handles 401 unauthorized`() =
        runTest {
            // Given: Mock server returns 401
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}"""),
            )

            // When: Get plugin settings with invalid token
            val result = apiService.getPluginSettings("calendars", "Bearer invalid_token")

            // Then: Verify HTTP failure
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(401)
        }

    // ========================================
    // POST /plugin_settings/{id}/data tests
    // ========================================

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
                    mergeVariables =
                        MergeVariables(
                            events =
                                listOf(
                                    CalendarEvent(
                                        summary = "Team Meeting",
                                        start = "09:00",
                                        startFull = "2025-01-15T09:00:00.000-05:00",
                                        dateTime = "2025-01-15T09:00:00.000-05:00",
                                        end = "10:00",
                                        endFull = "2025-01-15T10:00:00.000-05:00",
                                        description = "Weekly sync",
                                        allDay = false,
                                        status = "confirmed",
                                        calendarIdentifier = "user@example.com",
                                    ),
                                ),
                        ),
                )
            val result = apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)

            // Verify request details
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.path).isEqualTo("/plugin_settings/$TEST_SETTING_ID/data")
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
                    mergeVariables =
                        MergeVariables(
                            events =
                                listOf(
                                    CalendarEvent(
                                        summary = "Morning Standup",
                                        start = "09:00",
                                        startFull = "2025-01-15T09:00:00.000-05:00",
                                        dateTime = "2025-01-15T09:00:00.000-05:00",
                                        end = "09:15",
                                        endFull = "2025-01-15T09:15:00.000-05:00",
                                        calendarIdentifier = "user@example.com",
                                    ),
                                    CalendarEvent(
                                        summary = "All Day Workshop",
                                        start = "00:00",
                                        startFull = "2025-01-16T00:00:00.000-05:00",
                                        dateTime = "2025-01-16T00:00:00.000-05:00",
                                        end = "23:59",
                                        endFull = "2025-01-16T23:59:59.999-05:00",
                                        allDay = true,
                                        calendarIdentifier = "user@example.com",
                                    ),
                                ),
                        ),
                )
            val result = apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)

            // Verify request path and method
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.path).isEqualTo("/plugin_settings/$TEST_SETTING_ID/data")
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
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            val result = apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

            // Then: Verify success result
            assertThat(result).isInstanceOf(ApiResult.Success::class)
        }

    @Test
    fun `syncCalendarEvents uses correct path with setting id`() =
        runTest {
            // Given: Mock server returns 204 No Content
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Sync with a specific setting ID
            val specificSettingId = 99999
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            apiService.syncCalendarEvents(specificSettingId, "Bearer test_token", request)

            // Then: Verify the setting ID is in the path
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.path).isEqualTo("/plugin_settings/$specificSettingId/data")
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
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            val result = apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer invalid_token", request)

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
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            val result = apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

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
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            val result = apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

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
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            val result = timeoutApiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

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
            val request = CalendarSyncRequest(mergeVariables = MergeVariables(events = emptyList()))
            apiService.syncCalendarEvents(TEST_SETTING_ID, testToken, request)

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
                    mergeVariables =
                        MergeVariables(
                            events =
                                listOf(
                                    CalendarEvent(
                                        summary = "Event",
                                        start = "09:00",
                                        startFull = "2025-01-15T09:00:00.000-05:00",
                                        dateTime = "2025-01-15T09:00:00.000-05:00",
                                        end = "10:00",
                                        endFull = "2025-01-15T10:00:00.000-05:00",
                                        calendarIdentifier = "user@example.com",
                                    ),
                                ),
                        ),
                )
            apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

            // Then: Verify Content-Type header
            val recordedRequest = mockWebServer.takeRequest()
            assertThat(recordedRequest.getHeader("Content-Type")).isEqualTo("application/json; charset=utf-8")
        }

    @Test
    fun `verify request body contains merge_variables wrapper`() =
        runTest {
            // Given: Mock server returns success
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(204),
            )

            // When: Sync with an event
            val request =
                CalendarSyncRequest(
                    mergeVariables =
                        MergeVariables(
                            events =
                                listOf(
                                    CalendarEvent(
                                        summary = "Meeting",
                                        start = "14:00",
                                        startFull = "2025-01-15T14:00:00.000-05:00",
                                        dateTime = "2025-01-15T14:00:00.000-05:00",
                                        end = "15:00",
                                        endFull = "2025-01-15T15:00:00.000-05:00",
                                        calendarIdentifier = "user@example.com",
                                    ),
                                ),
                        ),
                )
            apiService.syncCalendarEvents(TEST_SETTING_ID, "Bearer test_token", request)

            // Then: Verify request body has merge_variables wrapper
            val recordedRequest = mockWebServer.takeRequest()
            val requestBody = recordedRequest.body.readUtf8()
            assertThat(requestBody).contains("merge_variables")
            assertThat(requestBody).contains("events")
            assertThat(requestBody).contains("summary")
            assertThat(requestBody).contains("calendar_identifier")
        }
}
