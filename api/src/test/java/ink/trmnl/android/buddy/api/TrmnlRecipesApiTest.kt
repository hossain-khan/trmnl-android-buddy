package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Unit tests for TrmnlApiService recipe endpoints using MockWebServer.
 *
 * These tests verify recipe-related API endpoints:
 * - GET /recipes.json - List recipes with search, sort, pagination
 * - GET /recipes/{id}.json - Get single recipe details
 *
 * Tests cover:
 * - Successful responses with various parameters
 * - HTTP error responses (4xx, 5xx)
 * - Network failures
 * - JSON parsing with all fields
 * - Search functionality
 * - Sort options (newest, oldest, popularity, install, fork)
 * - Pagination metadata
 *
 * **Note**: These are public endpoints that do NOT require authentication.
 */
class TrmnlRecipesApiTest {

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
    fun `getRecipes returns success with recipe list`() = runTest {
        // Given: Mock server returns successful response with recipes
        val responseBody = """
            {
              "data": [
                {
                  "id": 1,
                  "name": "Weather Chum",
                  "icon_url": "https://example.com/weather-icon.png",
                  "screenshot_url": "https://example.com/weather-screenshot.png",
                  "stats": {
                    "installs": 1230,
                    "forks": 1
                  }
                },
                {
                  "id": 2,
                  "name": "Matrix",
                  "icon_url": "https://example.com/matrix-icon.png",
                  "screenshot_url": "https://example.com/matrix-screenshot.png",
                  "stats": {
                    "installs": 25,
                    "forks": 176
                  }
                }
              ],
              "total": 100,
              "from": 1,
              "to": 2,
              "per_page": 25,
              "current_page": 1,
              "prev_page_url": null,
              "next_page_url": "https://usetrmnl.com/recipes.json?page=2"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getRecipes
        val result = apiService.getRecipes()

        // Then: Verify success result with correct data
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val successResult = result as ApiResult.Success

        assertThat(successResult.value.data).hasSize(2)
        assertThat(successResult.value.total).isEqualTo(100)
        assertThat(successResult.value.currentPage).isEqualTo(1)
        assertThat(successResult.value.perPage).isEqualTo(25)
        assertThat(successResult.value.prevPageUrl).isNull()
        assertThat(successResult.value.nextPageUrl).isNotNull()

        val firstRecipe = successResult.value.data[0]
        assertThat(firstRecipe.id).isEqualTo(1)
        assertThat(firstRecipe.name).isEqualTo("Weather Chum")
        assertThat(firstRecipe.iconUrl).isEqualTo("https://example.com/weather-icon.png")
        assertThat(firstRecipe.stats.installs).isEqualTo(1230)
        assertThat(firstRecipe.stats.forks).isEqualTo(1)

        val secondRecipe = successResult.value.data[1]
        assertThat(secondRecipe.id).isEqualTo(2)
        assertThat(secondRecipe.name).isEqualTo("Matrix")
        assertThat(secondRecipe.stats.forks).isEqualTo(176)

        // Verify request was made correctly
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/recipes.json")
        assertThat(request.method).isEqualTo("GET")
    }

    @Test
    fun `getRecipes with search parameter sends correct query`() = runTest {
        // Given: Mock server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [], "total": 0, "from": 0, "to": 0, "per_page": 25, "current_page": 1, "prev_page_url": null, "next_page_url": null}""")
        )

        // When: Call getRecipes with search
        apiService.getRecipes(search = "weather")

        // Then: Verify search query parameter
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/recipes.json?search=weather")
    }

    @Test
    fun `getRecipes with sort parameter sends correct query`() = runTest {
        // Given: Mock server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [], "total": 0, "from": 0, "to": 0, "per_page": 25, "current_page": 1, "prev_page_url": null, "next_page_url": null}""")
        )

        // When: Call getRecipes with sort
        apiService.getRecipes(sortBy = "popularity")

        // Then: Verify sort query parameter
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/recipes.json?sort-by=popularity")
    }

    @Test
    fun `getRecipes with pagination parameters sends correct query`() = runTest {
        // Given: Mock server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [], "total": 0, "from": 0, "to": 0, "per_page": 50, "current_page": 2, "prev_page_url": null, "next_page_url": null}""")
        )

        // When: Call getRecipes with pagination
        apiService.getRecipes(page = 2, perPage = 50)

        // Then: Verify pagination query parameters
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/recipes.json?page=2&per_page=50")
    }

    @Test
    fun `getRecipes with all parameters sends correct query`() = runTest {
        // Given: Mock server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [], "total": 0, "from": 0, "to": 0, "per_page": 10, "current_page": 3, "prev_page_url": null, "next_page_url": null}""")
        )

        // When: Call getRecipes with all parameters
        apiService.getRecipes(
            search = "matrix",
            sortBy = "install",
            page = 3,
            perPage = 10
        )

        // Then: Verify all query parameters
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/recipes.json?search=matrix&sort-by=install&page=3&per_page=10")
    }

    @Test
    fun `getRecipes returns empty list when no recipes`() = runTest {
        // Given: Mock server returns empty recipe list
        val responseBody = """
            {
              "data": [],
              "total": 0,
              "from": 0,
              "to": 0,
              "per_page": 25,
              "current_page": 1,
              "prev_page_url": null,
              "next_page_url": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // When: Call getRecipes
        val result = apiService.getRecipes()

        // Then: Verify success with empty list
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val successResult = result as ApiResult.Success
        assertThat(successResult.value.data).isEmpty()
        assertThat(successResult.value.total).isEqualTo(0)
    }

    @Test
    fun `getRecipe returns success with single recipe`() = runTest {
        // Given: Mock server returns single recipe with all fields
        val responseBody = """
            {
              "data": {
                "id": 123,
                "name": "Weather Dashboard",
                "icon_url": "https://example.com/icon.png",
                "screenshot_url": "https://example.com/screenshot.png",
                "author_bio": {
                  "keyname": "author_bio",
                  "name": "Author Bio",
                  "field_type": "author_bio",
                  "description": "A passionate developer creating weather apps"
                },
                "custom_fields": [
                  {
                    "keyname": "api_key",
                    "name": "API Key",
                    "field_type": "string",
                    "description": "Your weather API key",
                    "placeholder": "Enter API key",
                    "help_text": "Get your key from weather.com",
                    "required": true,
                    "default": ""
                  },
                  {
                    "keyname": "units",
                    "name": "Temperature Units",
                    "field_type": "select",
                    "description": "Select temperature units",
                    "required": false,
                    "options": ["Celsius", "Fahrenheit"],
                    "default": "Celsius"
                  }
                ],
                "stats": {
                  "installs": 500,
                  "forks": 25
                }
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getRecipe
        val result = apiService.getRecipe(123)

        // Then: Verify success result with correct data
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val successResult = result as ApiResult.Success
        val recipe = successResult.value.data

        assertThat(recipe.id).isEqualTo(123)
        assertThat(recipe.name).isEqualTo("Weather Dashboard")
        assertThat(recipe.iconUrl).isEqualTo("https://example.com/icon.png")
        assertThat(recipe.screenshotUrl).isEqualTo("https://example.com/screenshot.png")

        // Verify author bio
        assertThat(recipe.authorBio).isNotNull()
        assertThat(recipe.authorBio?.description).isEqualTo("A passionate developer creating weather apps")

        // Verify custom fields
        assertThat(recipe.customFields).hasSize(2)
        
        val apiKeyField = recipe.customFields[0]
        assertThat(apiKeyField.keyname).isEqualTo("api_key")
        assertThat(apiKeyField.name).isEqualTo("API Key")
        assertThat(apiKeyField.fieldType).isEqualTo("string")
        assertThat(apiKeyField.required).isEqualTo(true)
        assertThat(apiKeyField.placeholder).isEqualTo("Enter API key")
        assertThat(apiKeyField.helpText).isEqualTo("Get your key from weather.com")

        val unitsField = recipe.customFields[1]
        assertThat(unitsField.keyname).isEqualTo("units")
        assertThat(unitsField.fieldType).isEqualTo("select")
        assertThat(unitsField.required).isEqualTo(false)
        assertThat(unitsField.options).isNotNull()
        assertThat(unitsField.options!!).hasSize(2)
        assertThat(unitsField.default).isEqualTo("Celsius")

        // Verify stats
        assertThat(recipe.stats.installs).isEqualTo(500)
        assertThat(recipe.stats.forks).isEqualTo(25)

        // Verify request
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/recipes/123.json")
        assertThat(request.method).isEqualTo("GET")
    }

    @Test
    fun `getRecipes handles 404 not found`() = runTest {
        // Given: Mock server returns 404
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Not found"}""")
        )

        // When: Call getRecipes
        val result = apiService.getRecipes()

        // Then: Verify HTTP failure
        assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(404)
    }

    @Test
    fun `getRecipes handles 500 server error`() = runTest {
        // Given: Mock server returns 500
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "Internal server error"}""")
        )

        // When: Call getRecipes
        val result = apiService.getRecipes()

        // Then: Verify HTTP failure
        assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(500)
    }

    @Test
    fun `getRecipe handles 404 not found`() = runTest {
        // Given: Mock server returns 404
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Recipe not found"}""")
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getRecipe
        val result = apiService.getRecipe(999)

        // Then: Verify HTTP failure
        assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(404)
    }

    @Test
    fun `getRecipes handles network timeout`() = runTest {
        // Given: Mock server with slow response (simulates timeout)
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"data": []}""")
                .setBodyDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS)
        )

        // When: Call getRecipes with short timeout client
        val shortTimeoutClient = OkHttpClient.Builder()
            .readTimeout(100, java.util.concurrent.TimeUnit.MILLISECONDS)
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
        val result = timeoutApiService.getRecipes()

        // Then: Verify network failure
        assertThat(result).isInstanceOf(ApiResult.Failure.NetworkFailure::class)
    }

    @Test
    fun `getRecipes handles malformed JSON`() = runTest {
        // Given: Mock server returns invalid JSON
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [this is not valid json]}""")
                .addHeader("Content-Type", "application/json")
        )

        // When: Call getRecipes
        val result = apiService.getRecipes()

        // Then: Verify unknown failure (parsing error)
        assertThat(result).isInstanceOf(ApiResult.Failure.UnknownFailure::class)
    }

    @Test
    fun `recipe parsing handles missing optional fields`() = runTest {
        // Given: Response with minimal fields (icon_url, screenshot_url, author_bio are null)
        val responseBody = """
            {
              "data": [{
                "id": 999,
                "name": "Minimal Recipe",
                "stats": {
                  "installs": 0,
                  "forks": 0
                }
              }],
              "total": 1,
              "from": 1,
              "to": 1,
              "per_page": 25,
              "current_page": 1,
              "prev_page_url": null,
              "next_page_url": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // When: Parse response
        val result = apiService.getRecipes()

        // Then: Verify all fields parsed correctly with defaults
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val recipe = (result as ApiResult.Success).value.data[0]

        assertThat(recipe.id).isEqualTo(999)
        assertThat(recipe.name).isEqualTo("Minimal Recipe")
        assertThat(recipe.iconUrl).isNull()
        assertThat(recipe.screenshotUrl).isNull()
        assertThat(recipe.authorBio).isNull()
        assertThat(recipe.customFields).isEmpty()
        assertThat(recipe.stats.installs).isEqualTo(0)
        assertThat(recipe.stats.forks).isEqualTo(0)
    }

    @Test
    fun `pagination metadata is correctly parsed for last page`() = runTest {
        // Given: Last page response (no next page)
        val responseBody = """
            {
              "data": [],
              "total": 50,
              "from": 26,
              "to": 50,
              "per_page": 25,
              "current_page": 2,
              "prev_page_url": "https://usetrmnl.com/recipes.json?page=1",
              "next_page_url": null
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
        )

        // When: Get page 2
        val result = apiService.getRecipes(page = 2)

        // Then: Verify pagination metadata
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val response = (result as ApiResult.Success).value

        assertThat(response.total).isEqualTo(50)
        assertThat(response.from).isEqualTo(26)
        assertThat(response.to).isEqualTo(50)
        assertThat(response.currentPage).isEqualTo(2)
        assertThat(response.prevPageUrl).isNotNull()
        assertThat(response.nextPageUrl).isNull()
    }

    @Test
    fun `getRecipes does not send authorization header`() = runTest {
        // Given: Mock server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [], "total": 0, "from": 0, "to": 0, "per_page": 25, "current_page": 1, "prev_page_url": null, "next_page_url": null}""")
        )

        // When: Call getRecipes (public endpoint)
        apiService.getRecipes()

        // Then: Verify no authorization header is sent
        val request = mockWebServer.takeRequest()
        assertThat(request.getHeader("Authorization")).isNull()
    }
}
