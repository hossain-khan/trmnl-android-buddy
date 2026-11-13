package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

/**
 * Unit tests for TrmnlApiService device diagnostics endpoints using MockWebServer:
 * - GET /devices/{device_id}/forecast
 * - GET /devices/{device_id}/timeline
 * - GET /devices/{device_id}/logs
 */
class DeviceDiagnosticsApiTest : BaseApiTest() {
    @Test
    fun `getDeviceForecast returns parsed forecast steps`() =
        runTest {
            val responseBody =
                """
                {
                  "data": {
                    "steps": [
                      {
                        "at": "2026-11-13T05:15:00Z",
                        "playlist_item_id": 42,
                        "plugin_setting_id": 99,
                        "name": "Weather Dashboard",
                        "refresh_seconds": 900,
                        "refresh_rate_seconds": 900,
                        "reason": "scheduled_refresh",
                        "render_at": "2026-11-13T05:14:30Z",
                        "render_reason": "pre_render"
                      }
                    ]
                  }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .setHeader("Content-Type", "application/json"),
            )

            val result = apiService.getDeviceForecast("Bearer test_token", deviceId = 123, hours = 12)

            assertThat(result).isInstanceOf<ApiResult.Success<*>>()
            val response = (result as ApiResult.Success).value
            assertThat(response.data.steps).hasSize(1)
            val step = response.data.steps.first()
            assertThat(step.at).isEqualTo("2026-11-13T05:15:00Z")
            assertThat(step.name).isEqualTo("Weather Dashboard")
            assertThat(step.refreshRateSeconds).isEqualTo(900)

            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/devices/123/forecast?hours=12")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test_token")
        }

    @Test
    fun `getDeviceTimeline returns check-in status, recorded items, and expected steps`() =
        runTest {
            val responseBody =
                """
                {
                  "data": {
                    "date": "2026-11-13",
                    "today": "2026-11-13",
                    "zone": "America/New_York",
                    "available": true,
                    "query_failed": false,
                    "truncated": false,
                    "check_in": {
                      "state": "on_time",
                      "expected_at": "2026-11-13T05:15:00Z"
                    },
                    "recorded": [
                      {
                        "at": "2026-11-13T05:00:00Z",
                        "name": "Weather Dashboard",
                        "outcome": "delivered",
                        "leading": { "status": 200 },
                        "supporting": [],
                        "trace_entries": []
                      }
                    ],
                    "expected": [],
                    "strip": []
                  }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .setHeader("Content-Type", "application/json"),
            )

            val result = apiService.getDeviceTimeline("Bearer test_token", deviceId = 123, hours = 24)

            assertThat(result).isInstanceOf<ApiResult.Success<*>>()
            val timeline = (result as ApiResult.Success).value.data
            assertThat(timeline.available).isTrue()
            assertThat(timeline.queryFailed).isFalse()
            assertThat(timeline.checkIn?.state).isEqualTo("on_time")
            assertThat(timeline.recorded).hasSize(1)
            assertThat(timeline.recorded.first().outcome).isEqualTo("delivered")

            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/devices/123/timeline?hours=24")
        }

    @Test
    fun `getDeviceLogs parses dynamic JsonObject logs and cursor`() =
        runTest {
            val responseBody =
                """
                {
                  "data": {
                    "logs": [
                      {
                        "event_id": "evt_100",
                        "ts": "2026-11-13T05:00:00Z",
                        "level": "warn",
                        "source": "device",
                        "message": "WiFi reconnect attempt"
                      }
                    ],
                    "more": true,
                    "oldest_cursor": {
                      "ts": "2026-11-13T05:00:00Z",
                      "event_id": "evt_100"
                    }
                  }
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .setHeader("Content-Type", "application/json"),
            )

            val result =
                apiService.getDeviceLogs(
                    authorization = "Bearer test_token",
                    deviceId = 123,
                    level = "warn",
                    limit = 50,
                )

            assertThat(result).isInstanceOf<ApiResult.Success<*>>()
            val logsData = (result as ApiResult.Success).value.data
            assertThat(logsData.logs).hasSize(1)
            assertThat(
                logsData.logs
                    .first()["message"]
                    ?.jsonPrimitive
                    ?.content,
            ).isEqualTo("WiFi reconnect attempt")
            assertThat(logsData.more).isTrue()
            assertThat(logsData.oldestCursor).isNotNull()
            assertThat(logsData.oldestCursor?.eventId).isEqualTo("evt_100")

            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/devices/123/logs?level=warn&limit=50")
        }

    @Test
    fun `getDeviceForecast handles HTTP 404 failure`() =
        runTest {
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"error":"Device not found"}""")
                    .setHeader("Content-Type", "application/json"),
            )

            val result = apiService.getDeviceForecast("Bearer test_token", deviceId = 999)

            assertThat(result).isInstanceOf<ApiResult.Failure.HttpFailure<*>>()
            val failure = result as ApiResult.Failure.HttpFailure<*>
            assertThat(failure.code).isEqualTo(404)
        }

    @Test
    fun `getDeviceTimeline decodes HTTP 403 legacy API key error body`() =
        runTest {
            val message =
                "This is a legacy API key, which reaches only the endpoints it had before API keys held capabilities. " +
                    "Create an API key with the capabilities you need at https://trmnl.com/account."
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(403)
                    .setBody("""{"error":"$message"}""")
                    .setHeader("Content-Type", "application/json"),
            )

            val result = apiService.getDeviceTimeline("Bearer legacy_key", deviceId = 41448, hours = 12)

            assertThat(result).isInstanceOf<ApiResult.Failure.HttpFailure<*>>()
            val failure = result as ApiResult.Failure.HttpFailure<*>
            assertThat(failure.code).isEqualTo(403)
            assertThat(failure.error).isEqualTo(ApiError(message))
        }
}
