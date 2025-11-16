package ink.trmnl.android.buddy.api

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Test helper utility for creating TrmnlApiService instances with custom configurations.
 *
 * This helper centralizes common API service creation patterns used across multiple test files,
 * reducing code duplication and improving maintainability.
 */
object ApiServiceTestHelper {
    /**
     * Creates a TrmnlApiService with a custom timeout configuration.
     *
     * @param mockWebServer The MockWebServer instance to use as the base URL
     * @param json The Json configuration for serialization
     * @param timeoutSeconds The timeout duration in seconds for read operations
     * @return A configured TrmnlApiService instance
     */
    fun createApiServiceWithTimeout(
        mockWebServer: MockWebServer,
        json: Json,
        timeoutSeconds: Long,
    ): TrmnlApiService {
        val shortTimeoutClient =
            OkHttpClient
                .Builder()
                .readTimeout(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
                .build()

        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(shortTimeoutClient)
                .addConverterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultConverterFactory,
                ).addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                ).addCallAdapterFactory(
                    com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory,
                ).build()

        return retrofit.create(TrmnlApiService::class.java)
    }

    /**
     * Creates a TrmnlApiService with a custom app version for User-Agent header.
     *
     * @param mockWebServer The MockWebServer instance to use as the base URL
     * @param json The Json configuration for serialization
     * @param appVersion The app version string to include in the User-Agent header
     * @return A configured TrmnlApiService instance
     */
    fun createApiServiceWithAppVersion(
        mockWebServer: MockWebServer,
        json: Json,
        appVersion: String,
    ): TrmnlApiService {
        val okHttpClient = TrmnlApiClient.createOkHttpClient(appVersion = appVersion)

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

        return retrofit.create(TrmnlApiService::class.java)
    }
}
