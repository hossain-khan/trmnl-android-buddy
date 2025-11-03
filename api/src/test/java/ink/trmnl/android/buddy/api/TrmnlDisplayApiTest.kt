package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
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
 * Unit tests for TRMNL Display API endpoints.
 *
 * Tests the `/display/current` Device API endpoint using MockWebServer to simulate
 * various API responses and error conditions.
 *
 * Note: This is a Device API endpoint that uses Access-Token header (device API key),
 * not the user Account API key with Bearer token.
 *
 * Test Coverage:
 * - Success response with full display data
 * - Success response with optional fields null
 * - 401 Unauthorized (invalid/missing token)
 * - 500 Internal Server Error
 * - Network failures
 * - JSON field parsing and deserialization
 */
class TrmnlDisplayApiTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TrmnlApiService
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    @Before
    fun setUp() {
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
    fun tearDown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `getDisplayCurrent returns success with full display data`() = runTest {
        // Given
        val deviceApiKey = "abc-123"
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "status": 200,
                  "refresh_rate": 300,
                  "image_url": "https://usetrmnl.com/images/setup/setup-logo.bmp",
                  "filename": "setup-logo.bmp",
                  "rendered_at": "2023-01-01T00:00:00Z"
                }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
        
        // When
        val result = apiService.getDisplayCurrent(deviceApiKey)
        
        // Then
        assertThat(result).isInstanceOf<ApiResult.Success<*>>()
        val successResult = result as ApiResult.Success
        val display = successResult.value
        
        assertThat(display.status).isEqualTo(200)
        assertThat(display.refreshRate).isEqualTo(300)
        assertThat(display.imageUrl).isEqualTo("https://usetrmnl.com/images/setup/setup-logo.bmp")
        assertThat(display.filename).isEqualTo("setup-logo.bmp")
        assertThat(display.renderedAt).isEqualTo("2023-01-01T00:00:00Z")
        
        // Verify request
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/api/display/current")
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.getHeader("Access-Token")).isEqualTo(deviceApiKey)
    }
    
    @Test
    fun `getDisplayCurrent returns success with optional fields null`() = runTest {
        // Given
        val deviceApiKey = "abc-123"
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "status": 200,
                  "refresh_rate": 600
                }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
        
        // When
        val result = apiService.getDisplayCurrent(deviceApiKey)
        
        // Then
        assertThat(result).isInstanceOf<ApiResult.Success<*>>()
        val successResult = result as ApiResult.Success
        val display = successResult.value
        
        assertThat(display.status).isEqualTo(200)
        assertThat(display.refreshRate).isEqualTo(600)
        assertThat(display.imageUrl).isNull()
        assertThat(display.filename).isNull()
        assertThat(display.renderedAt).isNull()
    }
    
    @Test
    fun `getDisplayCurrent returns 401 Unauthorized`() = runTest {
        // Given
        val deviceApiKey = "invalid-key"
        val mockResponse = MockResponse()
            .setResponseCode(401)
            .setBody(
                """
                {
                  "error": "Unauthorized",
                  "message": "Invalid or missing device API token"
                }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
        
        // When
        val result = apiService.getDisplayCurrent(deviceApiKey)
        
        // Then
        assertThat(result).isInstanceOf<ApiResult.Failure.HttpFailure<*>>()
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(401)
    }
    
    @Test
    fun `getDisplayCurrent returns 500 Internal Server Error`() = runTest {
        // Given
        val deviceApiKey = "abc-123"
        val mockResponse = MockResponse()
            .setResponseCode(500)
            .setBody(
                """
                {
                  "error": "Internal Server Error",
                  "message": "An unexpected error occurred"
                }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
        
        // When
        val result = apiService.getDisplayCurrent(deviceApiKey)
        
        // Then
        assertThat(result).isInstanceOf<ApiResult.Failure.HttpFailure<*>>()
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(500)
    }
    
    @Test
    fun `getDisplayCurrent handles network failure`() = runTest {
        // Given
        mockWebServer.shutdown() // Force network failure
        
        // When
        val result = apiService.getDisplayCurrent("abc-123")
        
        // Then
        assertThat(result).isInstanceOf<ApiResult.Failure.NetworkFailure>()
    }
    
    @Test
    fun `getDisplayCurrent parses all JSON fields correctly`() = runTest {
        // Given
        val deviceApiKey = "test-device-key"
        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody(
                """
                {
                  "status": 200,
                  "refresh_rate": 1800,
                  "image_url": "https://example.com/custom-display.bmp",
                  "filename": "custom-display.bmp",
                  "rendered_at": "2024-12-25T18:30:00Z"
                }
                """.trimIndent()
            )
        mockWebServer.enqueue(mockResponse)
        
        // When
        val result = apiService.getDisplayCurrent(deviceApiKey)
        
        // Then
        assertThat(result).isInstanceOf<ApiResult.Success<*>>()
        val successResult = result as ApiResult.Success
        val display = successResult.value
        
        // Verify each field is correctly parsed
        assertThat(display.status).isEqualTo(200)
        assertThat(display.refreshRate).isEqualTo(1800)
        assertThat(display.imageUrl).isNotNull()
        assertThat(display.imageUrl).isEqualTo("https://example.com/custom-display.bmp")
        assertThat(display.filename).isNotNull()
        assertThat(display.filename).isEqualTo("custom-display.bmp")
        assertThat(display.renderedAt).isNotNull()
        assertThat(display.renderedAt).isEqualTo("2024-12-25T18:30:00Z")
    }
}
