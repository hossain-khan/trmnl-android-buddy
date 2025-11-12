package ink.trmnl.android.buddy.api

import android.os.Build
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotEmpty
import assertk.assertions.matches
import assertk.assertions.startsWith
import okhttp3.OkHttp
import org.junit.Test

/**
 * Unit tests for UserAgentProvider.
 *
 * Verifies that the user agent string is properly formatted following
 * industry best practices.
 */
class UserAgentProviderTest {
    @Test
    fun `getUserAgent returns non-empty string`() {
        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent("1.6.0")

        // Then: String is not empty
        assertThat(userAgent).isNotEmpty()
    }

    @Test
    fun `getUserAgent includes app name and version`() {
        // Given: App version
        val appVersion = "1.6.0"

        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent(appVersion)

        // Then: Contains app name and version
        assertThat(userAgent).startsWith("TrmnlAndroidBuddy/")
        assertThat(userAgent).contains(appVersion)
    }

    @Test
    fun `getUserAgent includes Android version`() {
        // Given: App version
        val appVersion = "1.6.0"

        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent(appVersion)

        // Then: Contains Android API level
        assertThat(userAgent).contains("Android ${Build.VERSION.SDK_INT}")
    }

    @Test
    fun `getUserAgent includes device model`() {
        // Given: App version
        val appVersion = "1.6.0"

        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent(appVersion)

        // Then: Contains device model or "Unknown" in test environment
        // In unit tests, Build.MODEL is null, so it should be "Unknown"
        // In real environment, it would be the actual device model
        val expectedModel = Build.MODEL ?: "Unknown"
        assertThat(userAgent).contains(expectedModel)
    }

    @Test
    fun `getUserAgent includes OkHttp version`() {
        // Given: App version
        val appVersion = "1.6.0"

        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent(appVersion)

        // Then: Contains OkHttp version
        assertThat(userAgent).contains("OkHttp/${OkHttp.VERSION}")
    }

    @Test
    fun `getUserAgent follows standard format`() {
        // Given: App version
        val appVersion = "1.6.0"

        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent(appVersion)

        // Then: Follows format: AppName/Version (Android APILevel; DeviceModel) OkHttp/Version
        // Using a flexible regex to match the expected format
        val pattern = Regex("^TrmnlAndroidBuddy/.+ \\(Android \\d+; .+\\) OkHttp/.+$")
        assertThat(userAgent).matches(pattern)
    }

    @Test
    fun `getUserAgent handles different version formats`() {
        // Test with various version formats
        val versions = listOf("1.0.0", "2.5.1", "10.20.30", "1.0.0-beta", "2.0")

        versions.forEach { version ->
            // When: Get user agent with different version
            val userAgent = UserAgentProvider.getUserAgent(version)

            // Then: Contains the version
            assertThat(userAgent).contains(version)
        }
    }

    @Test
    fun `getUserAgent with unknown version`() {
        // Given: Unknown version
        val appVersion = "unknown"

        // When: Get user agent
        val userAgent = UserAgentProvider.getUserAgent(appVersion)

        // Then: Still creates valid user agent
        assertThat(userAgent).contains("unknown")
        assertThat(userAgent).contains("Android")
        assertThat(userAgent).contains("OkHttp")
    }
}
