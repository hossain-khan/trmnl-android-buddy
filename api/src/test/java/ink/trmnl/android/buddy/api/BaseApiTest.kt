package ink.trmnl.android.buddy.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Base test class for TRMNL API tests that provides common setup and teardown logic.
 *
 * This base class eliminates code duplication across multiple API test files by centralizing:
 * - MockWebServer initialization and cleanup
 * - Retrofit instance creation with consistent configuration
 * - JSON serialization configuration
 * - TrmnlApiService instance creation
 *
 * Subclasses should extend this class to inherit the common test infrastructure and focus
 * on writing test cases without worrying about boilerplate setup.
 *
 * Example usage:
 * ```
 * class MyApiTest : BaseApiTest() {
 *     @Test
 *     fun `my test`() = runTest {
 *         mockWebServer.enqueue(MockResponse().setBody(...))
 *         val result = apiService.someMethod()
 *         // assertions...
 *     }
 * }
 * ```
 */
abstract class BaseApiTest {
    protected lateinit var mockWebServer: MockWebServer
    protected lateinit var apiService: TrmnlApiService

    /**
     * Shared JSON configuration used for all API tests.
     * Configured to be lenient and ignore unknown fields for robust testing.
     */
    protected val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    /**
     * Sets up the test environment before each test.
     * - Initializes and starts MockWebServer
     * - Creates Retrofit instance with test configuration
     * - Creates TrmnlApiService instance for testing
     *
     * Can be overridden by subclasses to perform additional setup.
     */
    @Before
    open fun setUp() {
        // Create and start MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Create Retrofit instance pointing to mock server
        val okHttpClient = OkHttpClient.Builder().build()

        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(okHttpClient)
                .addConverterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultConverterFactory,
                ).addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                ).addCallAdapterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory,
                ).build()

        apiService = retrofit.create(TrmnlApiService::class.java)
    }

    /**
     * Cleans up the test environment after each test.
     * - Shuts down MockWebServer to free resources
     */
    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }
}
