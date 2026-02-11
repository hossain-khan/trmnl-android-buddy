package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

/**
 * Unit tests for TrmnlApiService playlist items endpoints using MockWebServer.
 *
 * These tests verify playlist items API endpoints:
 * - GET /playlists/items - List all playlist items for user's devices
 *
 * Note: The API returns items for ALL devices. Filtering by device_id
 * happens client-side in the repository layer.
 *
 * Tests cover:
 * - Successful responses with playlist items
 * - Items with plugin settings vs mashups
 * - Visible vs hidden items
 * - Rendered vs never rendered items
 * - HTTP error responses
 */
class TrmnlPlaylistItemsApiTest : BaseApiTest() {
    @Test
    fun `getPlaylistItems returns success with playlist items`() =
        runTest {
            // Given: Mock server returns playlist items
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 491784,
                      "device_id": 41448,
                      "plugin_setting_id": 241324,
                      "mashup_id": null,
                      "visible": true,
                      "rendered_at": "2026-02-09T01:27:58.423Z",
                      "row_order": 2146435072,
                      "created_at": "2026-02-09T15:15:47.444Z",
                      "updated_at": "2026-02-09T15:15:47.444Z",
                      "mirror": false,
                      "plugin_setting": {
                        "id": 241324,
                        "name": "Kung Fu Panda Quotes",
                        "plugin_id": 37
                      }
                    },
                    {
                      "id": 490127,
                      "device_id": 41448,
                      "plugin_setting_id": null,
                      "mashup_id": 133326,
                      "visible": true,
                      "rendered_at": "2026-02-09T01:27:58.423Z",
                      "row_order": 2145386496,
                      "created_at": "2026-02-07T22:29:09.740Z",
                      "updated_at": "2026-02-09T01:27:58.423Z",
                      "mirror": false,
                      "plugin_setting": null
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

            // When: Call getPlaylistItems
            val result = apiService.getPlaylistItems("Bearer test_token")

            // Then: Verify success result with correct data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            assertThat(successResult.value.data).hasSize(2)

            // Verify first item (plugin setting)
            val firstItem = successResult.value.data[0]
            assertThat(firstItem.id).isEqualTo(491784)
            assertThat(firstItem.deviceId).isEqualTo(41448)
            assertThat(firstItem.pluginSettingId).isEqualTo(241324)
            assertThat(firstItem.mashupId).isNull()
            assertThat(firstItem.visible).isTrue()
            assertThat(firstItem.renderedAt).isEqualTo("2026-02-09T01:27:58.423Z")
            assertThat(firstItem.mirror).isFalse()

            assertThat(firstItem.pluginSetting).isNotNull()
            assertThat(firstItem.pluginSetting?.name).isEqualTo("Kung Fu Panda Quotes")
            assertThat(firstItem.pluginSetting?.pluginId).isEqualTo(37)

            // Verify second item (mashup)
            val secondItem = successResult.value.data[1]
            assertThat(secondItem.id).isEqualTo(490127)
            assertThat(secondItem.pluginSettingId).isNull()
            assertThat(secondItem.mashupId).isEqualTo(133326)
            assertThat(secondItem.pluginSetting).isNull()

            // Verify request was made correctly
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/playlists/items")
            assertThat(request.method).isEqualTo("GET")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test_token")
        }

    @Test
    fun `getPlaylistItems returns items with null rendered_at for never displayed content`() =
        runTest {
            // Given: Mock server returns playlist item that was never rendered
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 491790,
                      "device_id": 41448,
                      "plugin_setting_id": 241324,
                      "mashup_id": null,
                      "visible": true,
                      "rendered_at": null,
                      "row_order": 2097152000,
                      "created_at": "2026-02-09T15:18:34.013Z",
                      "updated_at": "2026-02-09T15:22:10.730Z",
                      "mirror": false,
                      "plugin_setting": {
                        "id": 241324,
                        "name": "Kung Fu Panda Quotes",
                        "plugin_id": 37
                      }
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

            // When: Call getPlaylistItems
            val result = apiService.getPlaylistItems("Bearer test_token")

            // Then: Verify rendered_at is null
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            val item = successResult.value.data[0]
            assertThat(item.renderedAt).isNull()
        }

    @Test
    fun `getPlaylistItems includes hidden items`() =
        runTest {
            // Given: Mock server returns mix of visible and hidden items
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 491784,
                      "device_id": 41448,
                      "plugin_setting_id": 241324,
                      "mashup_id": null,
                      "visible": true,
                      "rendered_at": "2026-02-09T01:27:58.423Z",
                      "row_order": 2146435072,
                      "created_at": "2026-02-09T15:15:47.444Z",
                      "updated_at": "2026-02-09T15:15:47.444Z",
                      "mirror": false,
                      "plugin_setting": {
                        "id": 241324,
                        "name": "Active Plugin",
                        "plugin_id": 37
                      }
                    },
                    {
                      "id": 490125,
                      "device_id": 41448,
                      "plugin_setting_id": 240432,
                      "mashup_id": null,
                      "visible": false,
                      "rendered_at": "2026-02-09T01:12:57.677Z",
                      "row_order": 2139095040,
                      "created_at": "2026-02-07T22:28:59.952Z",
                      "updated_at": "2026-02-09T01:12:57.677Z",
                      "mirror": false,
                      "plugin_setting": {
                        "id": 240432,
                        "name": "Hidden Plugin",
                        "plugin_id": 37
                      }
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

            // When: Call getPlaylistItems
            val result = apiService.getPlaylistItems("Bearer test_token")

            // Then: Verify both visible and hidden items are returned
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            assertThat(successResult.value.data).hasSize(2)
            assertThat(successResult.value.data[0].visible).isTrue()
            assertThat(successResult.value.data[1].visible).isFalse()
        }

    @Test
    fun `getPlaylistItems returns items from multiple devices`() =
        runTest {
            // Given: Mock server returns playlist items for multiple devices
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 491784,
                      "device_id": 41448,
                      "plugin_setting_id": 241324,
                      "mashup_id": null,
                      "visible": true,
                      "rendered_at": null,
                      "row_order": 2146435072,
                      "created_at": "2026-02-09T15:15:47.444Z",
                      "updated_at": "2026-02-09T15:15:47.444Z",
                      "mirror": false,
                      "plugin_setting": {
                        "id": 241324,
                        "name": "Device 1 Plugin",
                        "plugin_id": 37
                      }
                    },
                    {
                      "id": 466844,
                      "device_id": 13026,
                      "plugin_setting_id": 227882,
                      "mashup_id": null,
                      "visible": true,
                      "rendered_at": "2026-02-10T20:54:05.478Z",
                      "row_order": 2143644933,
                      "created_at": "2026-01-19T23:48:28.427Z",
                      "updated_at": "2026-02-10T20:54:05.478Z",
                      "mirror": false,
                      "plugin_setting": {
                        "id": 227882,
                        "name": "Device 2 Plugin",
                        "plugin_id": 37
                      }
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

            // When: Call getPlaylistItems
            val result = apiService.getPlaylistItems("Bearer test_token")

            // Then: Verify items from multiple devices are returned
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            assertThat(successResult.value.data).hasSize(2)
            assertThat(successResult.value.data[0].deviceId).isEqualTo(41448)
            assertThat(successResult.value.data[1].deviceId).isEqualTo(13026)

            // Verify request path
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/playlists/items")
        }

    @Test
    fun `getPlaylistItems returns HTTP 401 for unauthorized`() =
        runTest {
            // Given: Mock server returns 401 Unauthorized
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"error": "Unauthorized"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getPlaylistItems with invalid token
            val result = apiService.getPlaylistItems("Bearer invalid_token")

            // Then: Verify HTTP failure with 401
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(401)
        }

    @Test
    fun `getPlaylistItems returns HTTP 404 when no playlists exist`() =
        runTest {
            // Given: Mock server returns 404 Not Found
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"error": "Device not found"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getPlaylistItems
            val result = apiService.getPlaylistItems("Bearer test_token")

            // Then: Verify HTTP failure with 404
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failure = result as ApiResult.Failure.HttpFailure
            assertThat(failure.code).isEqualTo(404)
        }

    @Test
    fun `getPlaylistItems handles missing plugin_setting field in JSON`() =
        runTest {
            // Given: Mock server returns playlist item WITHOUT plugin_setting field
            // (not even null, field is completely omitted from JSON)
            val responseBody =
                """
                {
                  "data": [
                    {
                      "id": 490127,
                      "device_id": 41448,
                      "plugin_setting_id": null,
                      "mashup_id": 133326,
                      "visible": true,
                      "rendered_at": "2026-02-09T01:27:58.423Z",
                      "row_order": 2145386496,
                      "created_at": "2026-02-07T22:29:09.740Z",
                      "updated_at": "2026-02-09T01:27:58.423Z",
                      "mirror": false
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

            // When: Call getPlaylistItems
            val result = apiService.getPlaylistItems("Bearer test_token")

            // Then: Verify success result with null plugin_setting
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            assertThat(successResult.value.data).hasSize(1)

            val item = successResult.value.data[0]
            assertThat(item.id).isEqualTo(490127)
            assertThat(item.mashupId).isEqualTo(133326)
            assertThat(item.pluginSettingId).isNull()
            assertThat(item.pluginSetting).isNull() // Field was omitted, should default to null
            assertThat(item.isMashup()).isTrue()
            assertThat(item.displayName()).isEqualTo("Mashup #133326")
        }

    @Test
    fun `PlaylistItem displayName returns plugin name when available`() {
        // Given: Playlist item with plugin setting
        val item =
            ink.trmnl.android.buddy.api.models.PlaylistItem(
                id = 1,
                deviceId = 123,
                pluginSettingId = 456,
                mashupId = null,
                visible = true,
                renderedAt = null,
                rowOrder = 100,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                mirror = false,
                pluginSetting =
                    ink.trmnl.android.buddy.api.models.PluginSetting(
                        id = 456,
                        name = "My Plugin",
                        pluginId = 37,
                    ),
            )

        // When: Get display name
        val displayName = item.displayName()

        // Then: Returns plugin name
        assertThat(displayName).isEqualTo("My Plugin")
    }

    @Test
    fun `PlaylistItem displayName returns formatted mashup ID when plugin not available`() {
        // Given: Playlist item with mashup (no plugin setting)
        val item =
            ink.trmnl.android.buddy.api.models.PlaylistItem(
                id = 1,
                deviceId = 123,
                pluginSettingId = null,
                mashupId = 789,
                visible = true,
                renderedAt = null,
                rowOrder = 100,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                mirror = false,
                pluginSetting = null,
            )

        // When: Get display name
        val displayName = item.displayName()

        // Then: Returns formatted mashup ID
        assertThat(displayName).isEqualTo("Mashup #789")
    }

    @Test
    fun `PlaylistItem isNeverRendered returns true when rendered_at is null`() {
        // Given: Playlist item with null rendered_at
        val item =
            ink.trmnl.android.buddy.api.models.PlaylistItem(
                id = 1,
                deviceId = 123,
                pluginSettingId = 456,
                mashupId = null,
                visible = true,
                renderedAt = null,
                rowOrder = 100,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                mirror = false,
                pluginSetting = null,
            )

        // When: Check if never rendered
        val isNeverRendered = item.isNeverRendered()

        // Then: Returns true
        assertThat(isNeverRendered).isTrue()
    }

    @Test
    fun `PlaylistItem isNeverRendered returns false when rendered_at has value`() {
        // Given: Playlist item with rendered_at timestamp
        val item =
            ink.trmnl.android.buddy.api.models.PlaylistItem(
                id = 1,
                deviceId = 123,
                pluginSettingId = 456,
                mashupId = null,
                visible = true,
                renderedAt = "2026-02-09T01:27:58.423Z",
                rowOrder = 100,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                mirror = false,
                pluginSetting = null,
            )

        // When: Check if never rendered
        val isNeverRendered = item.isNeverRendered()

        // Then: Returns false
        assertThat(isNeverRendered).isFalse()
    }

    @Test
    fun `PlaylistItem isMashup returns true when mashup_id is not null`() {
        // Given: Playlist item with mashup ID
        val item =
            ink.trmnl.android.buddy.api.models.PlaylistItem(
                id = 1,
                deviceId = 123,
                pluginSettingId = null,
                mashupId = 789,
                visible = true,
                renderedAt = null,
                rowOrder = 100,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                mirror = false,
                pluginSetting = null,
            )

        // When: Check if mashup
        val isMashup = item.isMashup()

        // Then: Returns true
        assertThat(isMashup).isTrue()
    }

    @Test
    fun `PlaylistItem isMashup returns false when mashup_id is null`() {
        // Given: Playlist item without mashup ID
        val item =
            ink.trmnl.android.buddy.api.models.PlaylistItem(
                id = 1,
                deviceId = 123,
                pluginSettingId = 456,
                mashupId = null,
                visible = true,
                renderedAt = null,
                rowOrder = 100,
                createdAt = "2026-01-01T00:00:00Z",
                updatedAt = "2026-01-01T00:00:00Z",
                mirror = false,
                pluginSetting = null,
            )

        // When: Check if mashup
        val isMashup = item.isMashup()

        // Then: Returns false
        assertThat(isMashup).isFalse()
    }
}
