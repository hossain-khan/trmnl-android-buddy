package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.Test

/**
 * Unit tests for TrmnlApiService categories endpoint using MockWebServer.
 *
 * These tests verify the categories API endpoint:
 * - GET /categories - Get all valid plugin categories
 *
 * Tests cover:
 * - Successful response with category list
 * - HTTP error responses (4xx, 5xx)
 * - JSON parsing
 * - All 27 expected categories
 *
 * **Note**: This is a public endpoint that does NOT require authentication.
 */
class TrmnlCategoriesApiTest : BaseApiTest() {
    @Test
    fun `getCategories returns success with all categories`() =
        runTest {
            // Given: Mock server returns successful response from categories.json
            val responseBody = readTestResource("categories.json")

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getCategories
            val result = apiService.getCategories()

            // Then: Verify success result with correct category data
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val successResult = result as ApiResult.Success

            val categories = successResult.value.data
            assertThat(categories).hasSize(27)

            // Verify some expected categories
            assertThat(categories).contains("analytics")
            assertThat(categories).contains("calendar")
            assertThat(categories).contains("sports")
            assertThat(categories).contains("games")
            assertThat(categories).contains("programming")

            // Verify request was made correctly
            val request = mockWebServer.takeRequest()
            assertThat(request.path).isEqualTo("/categories")
            assertThat(request.method).isEqualTo("GET")
        }

    @Test
    fun `getCategories parses all 27 categories correctly`() =
        runTest {
            // Given: Mock server returns successful response with all categories
            val responseBody = readTestResource("categories.json")

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody),
            )

            // When: Call getCategories
            val result = apiService.getCategories()

            // Then: Verify all expected categories are present
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val categories = (result as ApiResult.Success).value.data

            val expectedCategories =
                listOf(
                    "analytics",
                    "art",
                    "calendar",
                    "comics",
                    "crm",
                    "custom",
                    "discovery",
                    "ecommerce",
                    "education",
                    "email",
                    "entertainment",
                    "environment",
                    "finance",
                    "games",
                    "humor",
                    "images",
                    "kpi",
                    "life",
                    "marketing",
                    "nature",
                    "news",
                    "personal",
                    "productivity",
                    "programming",
                    "sales",
                    "sports",
                    "travel",
                )

            assertThat(categories).isEqualTo(expectedCategories)
        }

    @Test
    fun `getCategories returns 500 server error`() =
        runTest {
            // Given: Mock server returns 500 Internal Server Error
            val errorBody = """{"error": "Internal Server Error"}"""

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(500)
                    .setBody(errorBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getCategories
            val result = apiService.getCategories()

            // Then: Verify HttpFailure with 500 status
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failureResult = result as ApiResult.Failure.HttpFailure
            assertThat(failureResult.code).isEqualTo(500)
        }

    @Test
    fun `getCategories returns 404 not found`() =
        runTest {
            // Given: Mock server returns 404 Not Found
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("""{"error": "Not Found"}""")
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getCategories
            val result = apiService.getCategories()

            // Then: Verify HttpFailure with 404 status
            assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
            val failureResult = result as ApiResult.Failure.HttpFailure
            assertThat(failureResult.code).isEqualTo(404)
        }

    @Test
    fun `getCategories handles empty category list`() =
        runTest {
            // Given: Mock server returns empty category list
            val responseBody =
                """
                {
                  "data": []
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json"),
            )

            // When: Call getCategories
            val result = apiService.getCategories()

            // Then: Verify success with empty list
            assertThat(result).isInstanceOf(ApiResult.Success::class)
            val categories = (result as ApiResult.Success).value.data
            assertThat(categories).hasSize(0)
        }

    /**
     * Helper function to read test resource files.
     */
    private fun readTestResource(fileName: String): String =
        javaClass.classLoader
            ?.getResource(fileName)
            ?.readText()
            ?: throw IllegalStateException("Test resource not found: $fileName")
}
