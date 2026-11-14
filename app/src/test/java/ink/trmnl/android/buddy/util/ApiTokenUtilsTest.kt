package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import org.junit.Test

/**
 * Unit tests for [isLegacyApiToken] and [UserPreferences.isLegacyApiToken].
 */
class ApiTokenUtilsTest {
    @Test
    fun `isLegacyApiToken returns true for user_ prefix`() {
        assertThat("user_abc123".isLegacyApiToken()).isTrue()
        assertThat("  user_abc123  ".isLegacyApiToken()).isTrue()
        assertThat(UserPreferences(apiToken = "user_abc123").isLegacyApiToken).isTrue()
    }

    @Test
    fun `isLegacyApiToken returns false for scoped trmnl_ prefix and blank or null`() {
        assertThat("trmnl_abc123".isLegacyApiToken()).isFalse()
        assertThat("".isLegacyApiToken()).isFalse()
        assertThat((null as String?).isLegacyApiToken()).isFalse()
        assertThat(UserPreferences(apiToken = "trmnl_abc123").isLegacyApiToken).isFalse()
        assertThat(UserPreferences(apiToken = null).isLegacyApiToken).isFalse()
    }
}
