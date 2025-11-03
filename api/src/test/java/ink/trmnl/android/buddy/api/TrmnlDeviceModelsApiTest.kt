package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
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
 * Unit tests for TrmnlApiService device models endpoints using MockWebServer.
 *
 * These tests verify device models API endpoints:
 * - GET /models - List all supported device models
 *
 * Tests cover:
 * - Successful responses
 * - HTTP error responses (4xx, 5xx)
 * - JSON parsing
 * - Device kind filtering
 */
class TrmnlDeviceModelsApiTest {

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
    fun `getDeviceModels returns success with all device models`() = runTest {
        // Given: Mock server returns successful response with device models
        val responseBody = """
            {
              "data": [
                {
                  "name": "og_png",
                  "label": "TRMNL OG (1-bit)",
                  "description": "TRMNL OG (1-bit)",
                  "width": 800,
                  "height": 480,
                  "colors": 2,
                  "bit_depth": 1,
                  "scale_factor": 1.0,
                  "rotation": 0,
                  "mime_type": "image/png",
                  "offset_x": 0,
                  "offset_y": 0,
                  "published_at": "2024-01-01T00:00:00.000Z",
                  "kind": "trmnl",
                  "palette_ids": ["bw"]
                },
                {
                  "name": "amazon_kindle_2024",
                  "label": "Amazon Kindle 2024",
                  "description": "Amazon Kindle 2024",
                  "width": 1400,
                  "height": 840,
                  "colors": 256,
                  "bit_depth": 8,
                  "scale_factor": 1.75,
                  "rotation": 90,
                  "mime_type": "image/png",
                  "offset_x": 75,
                  "offset_y": 25,
                  "published_at": "2024-01-01T00:00:00.000Z",
                  "kind": "kindle",
                  "palette_ids": ["gray-256"]
                },
                {
                  "name": "inkplate_10",
                  "label": "Inkplate 10",
                  "description": "Inkplate 10",
                  "width": 1200,
                  "height": 820,
                  "colors": 8,
                  "bit_depth": 3,
                  "scale_factor": 1.0,
                  "rotation": 0,
                  "mime_type": "image/png",
                  "offset_x": 0,
                  "offset_y": 0,
                  "published_at": "2024-01-01T00:00:00.000Z",
                  "kind": "byod",
                  "palette_ids": ["gray-4", "bw"]
                }
              ]
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json")
        )

        // When: Calling getDeviceModels
        val result = apiService.getDeviceModels("Bearer test_token")

        // Then: Response should be successful with device models
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val success = result as ApiResult.Success
        assertThat(success.value.data).hasSize(3)

        // Verify first device model (TRMNL)
        val trmnlModel = success.value.data[0]
        assertThat(trmnlModel.name).isEqualTo("og_png")
        assertThat(trmnlModel.label).isEqualTo("TRMNL OG (1-bit)")
        assertThat(trmnlModel.width).isEqualTo(800)
        assertThat(trmnlModel.height).isEqualTo(480)
        assertThat(trmnlModel.colors).isEqualTo(2)
        assertThat(trmnlModel.bitDepth).isEqualTo(1)
        assertThat(trmnlModel.kind).isEqualTo("trmnl")
        assertThat(trmnlModel.getSpecsSummary()).isEqualTo("800×480 • 2 colors • 1-bit")

        // Verify second device model (Kindle)
        val kindleModel = success.value.data[1]
        assertThat(kindleModel.name).isEqualTo("amazon_kindle_2024")
        assertThat(kindleModel.label).isEqualTo("Amazon Kindle 2024")
        assertThat(kindleModel.kind).isEqualTo("kindle")
        assertThat(kindleModel.colors).isEqualTo(256)
        assertThat(kindleModel.bitDepth).isEqualTo(8)

        // Verify third device model (BYOD)
        val byodModel = success.value.data[2]
        assertThat(byodModel.name).isEqualTo("inkplate_10")
        assertThat(byodModel.label).isEqualTo("Inkplate 10")
        assertThat(byodModel.kind).isEqualTo("byod")
        assertThat(byodModel.colors).isEqualTo(8)
        assertThat(byodModel.bitDepth).isEqualTo(3)

        // Verify request
        val request = mockWebServer.takeRequest()
        assertThat(request.path).isEqualTo("/api/models")
        assertThat(request.method).isEqualTo("GET")
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test_token")
    }

    @Test
    fun `getDeviceModels returns 401 unauthorized for invalid token`() = runTest {
        // Given: Mock server returns 401 unauthorized
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error": "Unauthorized"}""")
                .setHeader("Content-Type", "application/json")
        )

        // When: Calling getDeviceModels with invalid token
        val result = apiService.getDeviceModels("Bearer invalid_token")

        // Then: Response should be HTTP failure with 401
        assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(401)
    }

    @Test
    fun `getDeviceModels returns empty list when no models available`() = runTest {
        // Given: Mock server returns empty list
        val responseBody = """{"data": []}"""

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json")
        )

        // When: Calling getDeviceModels
        val result = apiService.getDeviceModels("Bearer test_token")

        // Then: Response should be successful with empty list
        assertThat(result).isInstanceOf(ApiResult.Success::class)
        val success = result as ApiResult.Success
        assertThat(success.value.data).hasSize(0)
    }

    @Test
    fun `getDeviceModels handles server error`() = runTest {
        // Given: Mock server returns 500 server error
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error": "Internal Server Error"}""")
                .setHeader("Content-Type", "application/json")
        )

        // When: Calling getDeviceModels
        val result = apiService.getDeviceModels("Bearer test_token")

        // Then: Response should be HTTP failure with 500
        assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(500)
    }

    @Test
    fun `getDeviceModels returns 404 not found for invalid endpoint`() = runTest {
        // Given: Mock server returns 404 not found
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("""{"error": "Not Found"}""")
                .setHeader("Content-Type", "application/json")
        )

        // When: Calling getDeviceModels
        val result = apiService.getDeviceModels("Bearer test_token")

        // Then: Response should be HTTP failure with 404
        assertThat(result).isInstanceOf(ApiResult.Failure.HttpFailure::class)
        val failure = result as ApiResult.Failure.HttpFailure
        assertThat(failure.code).isEqualTo(404)
    }

    @Test
    fun `getDeviceModels handles malformed JSON response`() = runTest {
        // Given: Mock server returns malformed JSON
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data": [this is not valid json]}""")
                .setHeader("Content-Type", "application/json")
        )

        // When: Calling getDeviceModels
        val result = apiService.getDeviceModels("Bearer test_token")

        // Then: Response should be unknown failure (parsing error)
        assertThat(result).isInstanceOf(ApiResult.Failure.UnknownFailure::class)
    }

    @Test
    fun `getDeviceModels handles network timeout`() = runTest {
        // Given: Mock server with slow response (simulates timeout)
        mockWebServer.enqueue(
            MockResponse()
                .setBody("""{"data": []}""")
                .setBodyDelay(500, java.util.concurrent.TimeUnit.MILLISECONDS)
        )

        // When: Calling getDeviceModels with short timeout client
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
        val result = timeoutApiService.getDeviceModels("Bearer test_token")

        // Then: Response should be network failure
        assertThat(result).isInstanceOf(ApiResult.Failure.NetworkFailure::class)
    }

    @Test
    fun `DeviceModel getSpecsSummary formats correctly`() {
        // Given: Device models with different specs
        val models = listOf(
            createTestDeviceModel(width = 800, height = 480, colors = 2, bitDepth = 1),
            createTestDeviceModel(width = 1400, height = 840, colors = 256, bitDepth = 8),
            createTestDeviceModel(width = 1200, height = 820, colors = 8, bitDepth = 3),
        )

        // Then: Specs summary should be formatted correctly
        assertThat(models[0].getSpecsSummary()).isEqualTo("800×480 • 2 colors • 1-bit")
        assertThat(models[1].getSpecsSummary()).isEqualTo("1400×840 • 256 colors • 8-bit")
        assertThat(models[2].getSpecsSummary()).isEqualTo("1200×820 • 8 colors • 3-bit")
    }

    /**
     * Helper function to create test device model with customizable properties.
     */
    private fun createTestDeviceModel(
        name: String = "test_device",
        label: String = "Test Device",
        description: String = "Test Device",
        width: Int = 800,
        height: Int = 480,
        colors: Int = 2,
        bitDepth: Int = 1,
        scaleFactor: Double = 1.0,
        rotation: Int = 0,
        mimeType: String = "image/png",
        offsetX: Int = 0,
        offsetY: Int = 0,
        publishedAt: String = "2024-01-01T00:00:00.000Z",
        kind: String = "trmnl",
        paletteIds: List<String> = listOf("bw"),
    ) = ink.trmnl.android.buddy.api.models.DeviceModel(
        name = name,
        label = label,
        description = description,
        width = width,
        height = height,
        colors = colors,
        bitDepth = bitDepth,
        scaleFactor = scaleFactor,
        rotation = rotation,
        mimeType = mimeType,
        offsetX = offsetX,
        offsetY = offsetY,
        publishedAt = publishedAt,
        kind = kind,
        paletteIds = paletteIds,
    )
}
