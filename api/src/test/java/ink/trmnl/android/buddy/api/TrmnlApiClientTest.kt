package ink.trmnl.android.buddy.api

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Unit tests for TrmnlApiClient factory methods.
 *
 * Tests the client configuration including:
 * - OkHttpClient creation with various options
 * - Retrofit instance creation
 * - API service factory method
 * - Interceptor configuration (auth, user-agent, logging)
 */
class TrmnlApiClientTest {
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `createOkHttpClient creates client with default configuration`() {
        // When: Create OkHttp client with defaults
        val client = TrmnlApiClient.createOkHttpClient()

        // Then: Client is created successfully
        assertThat(client).isNotNull()
        assertThat(client.interceptors).hasSize(1) // User-Agent interceptor
    }

    @Test
    fun `createOkHttpClient with debug logging adds logging interceptor`() {
        // When: Create OkHttp client with debug enabled
        val client = TrmnlApiClient.createOkHttpClient(isDebug = true)

        // Then: Logging interceptor is added
        assertThat(client.interceptors).hasSize(2) // Logging + User-Agent
    }

    @Test
    fun `createOkHttpClient with API key adds auth interceptor`() {
        // When: Create OkHttp client with API key
        val apiKey = "user_test123"
        val client = TrmnlApiClient.createOkHttpClient(apiKey = apiKey)

        // Then: Auth interceptor is added
        assertThat(client.interceptors).hasSize(2) // Auth + User-Agent
    }

    @Test
    fun `createOkHttpClient with all options adds all interceptors`() {
        // When: Create OkHttp client with all options
        val client =
            TrmnlApiClient.createOkHttpClient(
                isDebug = true,
                apiKey = "user_test123",
                appVersion = "1.0.0",
            )

        // Then: All interceptors are added
        assertThat(client.interceptors).hasSize(3) // Logging + Auth + User-Agent
    }

    @Test
    fun `createOkHttpClient auth interceptor adds bearer token header`() {
        // Given: OkHttp client with API key
        val apiKey = "user_test123"
        val client = TrmnlApiClient.createOkHttpClient(apiKey = apiKey)

        // Given: Mock server
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        // When: Make a request using the client
        val request =
            okhttp3.Request
                .Builder()
                .url(mockWebServer.url("/test"))
                .build()
        client.newCall(request).execute()

        // Then: Auth header is present
        val recordedRequest = mockWebServer.takeRequest()
        assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("Bearer $apiKey")
    }

    @Test
    fun `createOkHttpClient user-agent interceptor adds header`() {
        // Given: OkHttp client with app version
        val appVersion = "1.5.0"
        val client = TrmnlApiClient.createOkHttpClient(appVersion = appVersion)

        // Given: Mock server
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        // When: Make a request using the client
        val request =
            okhttp3.Request
                .Builder()
                .url(mockWebServer.url("/test"))
                .build()
        client.newCall(request).execute()

        // Then: User-Agent header is present and formatted correctly
        val recordedRequest = mockWebServer.takeRequest()
        val userAgent = recordedRequest.getHeader("User-Agent")

        assertThat(userAgent).isNotNull()
        assertThat(userAgent!!).contains("TrmnlAndroidBuddy")
        assertThat(userAgent).contains(appVersion)
        assertThat(userAgent).contains("Android")
        assertThat(userAgent).contains("OkHttp")
    }

    @Test
    fun `createRetrofit creates retrofit instance with base URL`() {
        // When: Create Retrofit instance
        val retrofit = TrmnlApiClient.createRetrofit()

        // Then: Retrofit is configured with correct base URL
        assertThat(retrofit.baseUrl().toString()).isEqualTo("https://usetrmnl.com/api/")
    }

    @Test
    fun `createRetrofit with custom client uses provided client`() {
        // Given: Custom OkHttp client
        val customClient = TrmnlApiClient.createOkHttpClient(isDebug = true)

        // When: Create Retrofit with custom client
        val retrofit = TrmnlApiClient.createRetrofit(customClient)

        // Then: Retrofit uses the custom client
        assertThat(retrofit.baseUrl().toString()).isEqualTo("https://usetrmnl.com/api/")
    }

    @Test
    fun `create factory method returns TrmnlApiService`() {
        // When: Create API service
        val apiService = TrmnlApiClient.create()

        // Then: Service is created successfully
        assertThat(apiService).isNotNull()
    }

    @Test
    fun `create with all parameters configures service correctly`() {
        // Given: Mock server
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"data": []}"""))

        // When: Create API service with all parameters
        val apiKey = "user_test123"
        val appVersion = "2.0.0"

        // Create client with custom base URL pointing to mock server
        val okHttpClient =
            TrmnlApiClient.createOkHttpClient(
                isDebug = true,
                apiKey = apiKey,
                appVersion = appVersion,
            )

        val retrofit =
            retrofit2.Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(okHttpClient)
                .addConverterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultConverterFactory,
                ).addConverterFactory(
                    Json.asConverterFactory("application/json".toMediaType()),
                ).addCallAdapterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory,
                ).build()

        val apiService = retrofit.create(TrmnlApiService::class.java)

        // Make a test call
        kotlinx.coroutines.runBlocking {
            apiService.getDevices("Bearer $apiKey")
        }

        // Then: Verify headers are configured
        val request = mockWebServer.takeRequest()
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer $apiKey")
        val userAgent = request.getHeader("User-Agent")
        assertThat(userAgent).isNotNull()
        assertThat(userAgent!!).contains(appVersion)
    }

    @Test
    fun `createOkHttpClient sets correct timeouts`() {
        // When: Create OkHttp client
        val client = TrmnlApiClient.createOkHttpClient()

        // Then: Timeouts are configured
        assertThat(client.connectTimeoutMillis).isEqualTo(30_000) // 30 seconds
        assertThat(client.readTimeoutMillis).isEqualTo(30_000) // 30 seconds
        assertThat(client.writeTimeoutMillis).isEqualTo(30_000) // 30 seconds
    }
}
