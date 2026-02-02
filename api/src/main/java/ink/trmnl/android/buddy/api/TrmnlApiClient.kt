package ink.trmnl.android.buddy.api

import com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
import com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * TRMNL API Client factory and configuration.
 *
 * Provides configured instances of the TRMNL API service with proper
 * JSON serialization, logging, and error handling.
 */
object TrmnlApiClient {
    private const val BASE_URL = "https://trmnl.com/api/"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L

    /**
     * JSON configuration for kotlinx.serialization
     */
    private val json =
        Json {
            ignoreUnknownKeys = true // Ignore unknown fields in API responses
            isLenient = true // Be lenient with non-standard JSON
            coerceInputValues = true // Coerce null to default values
            encodeDefaults = true // Include default values in serialization
            prettyPrint = false // Compact JSON for network efficiency
        }

    /**
     * Creates a configured OkHttpClient with logging and timeouts.
     *
     * @param isDebug Enable debug logging (default: false for production)
     * @param apiKey Optional API key for authentication
     * @param appVersion Application version for User-Agent header (default: "unknown")
     * @return Configured OkHttpClient instance
     */
    fun createOkHttpClient(
        isDebug: Boolean = false,
        apiKey: String? = null,
        appVersion: String = "unknown",
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .apply {
                // Timeouts
                connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                // Logging (only in debug builds)
                if (isDebug) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }

                // Authentication interceptor
                if (apiKey != null) {
                    addInterceptor { chain ->
                        val request =
                            chain
                                .request()
                                .newBuilder()
                                .addHeader("Authorization", "Bearer $apiKey")
                                .build()
                        chain.proceed(request)
                    }
                }

                // User-Agent header
                addInterceptor { chain ->
                    val request =
                        chain
                            .request()
                            .newBuilder()
                            .addHeader("User-Agent", UserAgentProvider.getUserAgent(appVersion))
                            .build()
                    chain.proceed(request)
                }
            }.build()

    /**
     * Creates a configured Retrofit instance for TRMNL API.
     *
     * Includes EitherNet's ApiResultCallAdapterFactory and ApiResultConverterFactory
     * for type-safe API result handling.
     *
     * @param okHttpClient Optional custom OkHttpClient
     * @return Configured Retrofit instance
     */
    fun createRetrofit(okHttpClient: OkHttpClient = createOkHttpClient()): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ApiResultConverterFactory)
            .addConverterFactory(json.asConverterFactory(contentType))
            .addCallAdapterFactory(ApiResultCallAdapterFactory)
            .build()
    }

    /**
     * Creates a TRMNL API service instance.
     *
     * @param apiKey Optional API key for authentication
     * @param isDebug Enable debug logging (default: false for production)
     * @param appVersion Application version for User-Agent header (default: "unknown")
     * @return TrmnlApiService instance
     */
    fun create(
        apiKey: String? = null,
        isDebug: Boolean = false,
        appVersion: String = "unknown",
    ): TrmnlApiService {
        val okHttpClient = createOkHttpClient(isDebug, apiKey, appVersion)
        val retrofit = createRetrofit(okHttpClient)
        return retrofit.create(TrmnlApiService::class.java)
    }
}
