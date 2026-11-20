package ink.trmnl.android.buddy.api

import com.slack.eithernet.integration.retrofit.ApiResultCallAdapterFactory
import com.slack.eithernet.integration.retrofit.ApiResultConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

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
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build()

        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(shortTimeoutClient)
                .addConverterFactory(
                    ApiResultConverterFactory,
                ).addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                ).addCallAdapterFactory(
                    ApiResultCallAdapterFactory,
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
                    ApiResultConverterFactory,
                ).addConverterFactory(
                    json.asConverterFactory("application/json".toMediaType()),
                ).addCallAdapterFactory(
                    ApiResultCallAdapterFactory,
                ).build()

        return retrofit.create(TrmnlApiService::class.java)
    }
}
