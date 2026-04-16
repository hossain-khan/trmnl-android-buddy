package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import ink.trmnl.android.buddy.api.models.RecipesAnalyticsResponse
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * Unit tests for serialization and deserialization of recipes analytics responses.
 *
 * Tests verify that the RecipesAnalytics model and related data classes can correctly
 * parse real-world analytics JSON responses from the TRMNL API.
 *
 * Two scenarios are tested:
 * - Authenticated response: with full plugin data
 * - Unauthenticated response: with empty data
 *
 * This ensures the custom GrowthDataPointSerializer works correctly and all fields
 * deserialize properly.
 */
class RecipesAnalyticsJsonParsingTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Test
    fun `parse authenticated recipes analytics json successfully`() {
        // Given: Authenticated response with full plugin data
        val jsonContent =
            """{
              "data": {
                "plugins": [
                  {"name": "Calendar XL", "state": "healthy", "installs": 0, "forks": 43},
                  {"name": "Durham Waste Collection", "state": "healthy", "installs": 9, "forks": 7},
                  {"name": "Element of the Day", "state": "healthy", "installs": 9, "forks": 73},
                  {"name": "Google Photos", "state": "healthy", "installs": 1, "forks": 124},
                  {"name": "Google Photos Canvas", "state": "healthy", "installs": 0, "forks": 65},
                  {"name": "GO Transit Schedule", "state": "healthy", "installs": 0, "forks": 14},
                  {"name": "Islamic Prayer Times", "state": "healthy", "installs": 5, "forks": 17},
                  {"name": "Kung Fu Panda Quotes", "state": "healthy", "installs": 5, "forks": 23},
                  {"name": "Max Payne Quotes", "state": "healthy", "installs": 27, "forks": 1}
                ],
                "stats": {"plugins": 9, "connections": 423, "pageviews": 28},
                "health": {
                  "healthy": {"percent": 121.3333333333333},
                  "degraded": {"percent": 0.6666666666666666},
                  "erroring": {"percent": 0.3333333333333333}
                },
                "growth": [
                  ["2026-04-09", 0],
                  ["2026-04-10", 0],
                  ["2026-04-11", 0],
                  ["2026-04-12", 0],
                  ["2026-04-13", 2],
                  ["2026-04-14", 1],
                  ["2026-04-15", 3],
                  ["2026-04-16", 0]
                ]
              }
            }"""

        // When: Parse the JSON
        val response = json.decodeFromString<RecipesAnalyticsResponse>(jsonContent)

        // Then: Verify plugins list
        assertThat(response.data.plugins).hasSize(9)

        // Verify first plugin
        val firstPlugin = response.data.plugins[0]
        assertThat(firstPlugin.name).isEqualTo("Calendar XL")
        assertThat(firstPlugin.state).isEqualTo("healthy")
        assertThat(firstPlugin.installs).isEqualTo(0)
        assertThat(firstPlugin.forks).isEqualTo(43)

        // Verify plugin with non-zero installs
        val googlePhotosPlugin = response.data.plugins[3]
        assertThat(googlePhotosPlugin.name).isEqualTo("Google Photos")
        assertThat(googlePhotosPlugin.installs).isEqualTo(1)
        assertThat(googlePhotosPlugin.forks).isEqualTo(124)

        // Verify plugin with high fork count
        val maxPaynePlugin = response.data.plugins[8]
        assertThat(maxPaynePlugin.name).isEqualTo("Max Payne Quotes")
        assertThat(maxPaynePlugin.installs).isEqualTo(27)
        assertThat(maxPaynePlugin.forks).isEqualTo(1)

        // Verify stats
        assertThat(response.data.stats.plugins).isEqualTo(9)
        assertThat(response.data.stats.connections).isEqualTo(423)
        assertThat(response.data.stats.pageviews).isEqualTo(28)

        // Verify health status percentages
        assertThat(response.data.health.healthy.percent).isNotNull()
        assertThat(response.data.health.healthy.percent!!).isGreaterThan(100.0)
        assertThat(response.data.health.degraded.percent).isNotNull()
        assertThat(response.data.health.degraded.percent!!).isGreaterThan(0.0)
        assertThat(response.data.health.erroring.percent).isNotNull()
        assertThat(response.data.health.erroring.percent!!).isGreaterThan(0.0)

        // Verify growth data with custom serializer
        assertThat(response.data.growth).hasSize(8)

        // First growth data point
        val firstGrowth = response.data.growth[0]
        assertThat(firstGrowth.date).isEqualTo("2026-04-09")
        assertThat(firstGrowth.value).isEqualTo(0)

        // Growth data point with non-zero value
        val growthWithValue = response.data.growth[4]
        assertThat(growthWithValue.date).isEqualTo("2026-04-13")
        assertThat(growthWithValue.value).isEqualTo(2)

        // Last growth data point
        val lastGrowth = response.data.growth[7]
        assertThat(lastGrowth.date).isEqualTo("2026-04-16")
        assertThat(lastGrowth.value).isEqualTo(0)
    }

    @Test
    fun `parse unauthenticated recipes analytics json with empty data`() {
        // Given: Unauthenticated response with empty plugins list
        val jsonContent =
            """{
              "data": {
                "plugins": [],
                "stats": {"plugins": 0, "connections": 0, "pageviews": 0},
                "health": {
                  "healthy": {"percent": null},
                  "degraded": {"percent": null},
                  "erroring": {"percent": null}
                },
                "growth": [
                  ["2026-04-09", 0],
                  ["2026-04-10", 0],
                  ["2026-04-11", 0],
                  ["2026-04-12", 0],
                  ["2026-04-13", 0],
                  ["2026-04-14", 0],
                  ["2026-04-15", 0],
                  ["2026-04-16", 0]
                ]
              }
            }"""

        // When: Parse the JSON
        val response = json.decodeFromString<RecipesAnalyticsResponse>(jsonContent)

        // Then: Verify empty plugins list
        assertThat(response.data.plugins).hasSize(0)

        // Verify zero stats
        assertThat(response.data.stats.plugins).isEqualTo(0)
        assertThat(response.data.stats.connections).isEqualTo(0)
        assertThat(response.data.stats.pageviews).isEqualTo(0)

        // Verify health status with null percentages
        assertThat(response.data.health.healthy.percent).isNull()
        assertThat(response.data.health.degraded.percent).isNull()
        assertThat(response.data.health.erroring.percent).isNull()

        // Verify growth data (should still be present even without auth)
        assertThat(response.data.growth).hasSize(8)

        // All growth values should be 0 for unauthenticated
        val firstGrowth = response.data.growth[0]
        assertThat(firstGrowth.date).isEqualTo("2026-04-09")
        assertThat(firstGrowth.value).isEqualTo(0)

        val lastGrowth = response.data.growth[7]
        assertThat(lastGrowth.date).isEqualTo("2026-04-16")
        assertThat(lastGrowth.value).isEqualTo(0)
    }

    @Test
    fun `growth data points deserialize from array format correctly`() {
        // Given: Simple JSON with just growth data to test the custom serializer
        val jsonContent =
            """{
              "data": {
                "plugins": [],
                "stats": {"plugins": 0, "connections": 0, "pageviews": 0},
                "health": {"healthy": {"percent": null}, "degraded": {"percent": null}, "erroring": {"percent": null}},
                "growth": [
                  ["2026-04-13", 5],
                  ["2026-04-14", 10],
                  ["2026-04-15", 15],
                  ["2026-04-16", 8]
                ]
              }
            }"""

        // When: Parse the JSON
        val response = json.decodeFromString<RecipesAnalyticsResponse>(jsonContent)

        // Then: Verify growth data points are deserialized correctly from array format
        assertThat(response.data.growth).hasSize(4)

        // Verify first growth point
        val firstPoint = response.data.growth[0]
        assertThat(firstPoint.date).isEqualTo("2026-04-13")
        assertThat(firstPoint.value).isEqualTo(5)

        // Verify second growth point
        val secondPoint = response.data.growth[1]
        assertThat(secondPoint.date).isEqualTo("2026-04-14")
        assertThat(secondPoint.value).isEqualTo(10)

        // Verify third growth point
        val thirdPoint = response.data.growth[2]
        assertThat(thirdPoint.date).isEqualTo("2026-04-15")
        assertThat(thirdPoint.value).isEqualTo(15)

        // Verify fourth growth point
        val fourthPoint = response.data.growth[3]
        assertThat(fourthPoint.date).isEqualTo("2026-04-16")
        assertThat(fourthPoint.value).isEqualTo(8)
    }

    @Test
    fun `parse analytics with varying plugin health states`() {
        // Given: JSON with plugins having different health states
        val jsonContent =
            """{
              "data": {
                "plugins": [
                  {"name": "Healthy Plugin", "state": "healthy", "installs": 100, "forks": 50},
                  {"name": "Degraded Plugin", "state": "degraded", "installs": 10, "forks": 5},
                  {"name": "Erroring Plugin", "state": "erroring", "installs": 1, "forks": 0}
                ],
                "stats": {"plugins": 3, "connections": 111, "pageviews": 20},
                "health": {
                  "healthy": {"percent": 85.5},
                  "degraded": {"percent": 10.2},
                  "erroring": {"percent": 4.3}
                },
                "growth": [["2026-04-16", 100]]
              }
            }"""

        // When: Parse the JSON
        val response = json.decodeFromString<RecipesAnalyticsResponse>(jsonContent)

        // Then: Verify plugins with different states are parsed correctly
        assertThat(response.data.plugins).hasSize(3)

        val healthyPlugin = response.data.plugins[0]
        assertThat(healthyPlugin.state).isEqualTo("healthy")
        assertThat(healthyPlugin.installs).isEqualTo(100)

        val degradedPlugin = response.data.plugins[1]
        assertThat(degradedPlugin.state).isEqualTo("degraded")
        assertThat(degradedPlugin.installs).isEqualTo(10)

        val erroringPlugin = response.data.plugins[2]
        assertThat(erroringPlugin.state).isEqualTo("erroring")
        assertThat(erroringPlugin.installs).isEqualTo(1)

        // Verify health percentages sum correctly (approximately)
        val totalHealth =
            (response.data.health.healthy.percent ?: 0.0) +
                (response.data.health.degraded.percent ?: 0.0) +
                (response.data.health.erroring.percent ?: 0.0)
        assertThat(totalHealth).isGreaterThan(99.0) // Account for floating point precision
    }
}
