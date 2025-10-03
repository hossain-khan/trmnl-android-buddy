package ink.trmnl.android.buddy.ui.user

import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.welcome.WelcomeScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for UserAccountScreen including presenter tests.
 * Tests the logout functionality, device token clearing, and API key redaction logic.
 */
class UserAccountScreenTest {
    @Test
    fun `redactApiKey handles normal API key correctly`() {
        // Given: A typical API key
        val apiKey = "user_abc123xyz789"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should show first 4 and last 4 with asterisks in middle
        assertThat(redacted).hasLength(apiKey.length)
        assertThat(redacted.take(4)).isEqualTo("user")
        assertThat(redacted.takeLast(4)).isEqualTo("z789")
        assertThat(redacted.substring(4, redacted.length - 4).all { it == '*' }).isTrue()
    }

    @Test
    fun `redactApiKey handles short API key`() {
        // Given: A short API key (8 characters or less)
        val apiKey = "short123"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should fully redact short keys
        assertThat(redacted).isEqualTo("****")
    }

    @Test
    fun `redactApiKey handles very short API key`() {
        // Given: A very short API key
        val apiKey = "test"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should fully redact
        assertThat(redacted).isEqualTo("****")
    }

    @Test
    fun `redactApiKey handles exactly 9 character key`() {
        // Given: 9 character API key (just long enough to show prefix/suffix)
        val apiKey = "user_1234"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should show first 4, last 4, with at least 4 asterisks
        assertThat(redacted).isEqualTo("user****1234")
    }

    @Test
    fun `redactApiKey handles long API key`() {
        // Given: A long API key
        val apiKey = "user_abcdefghijklmnopqrstuvwxyz123456789"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: First 4 and last 4 should be visible
        assertThat(redacted).hasLength(apiKey.length) // Length should be preserved
        assertThat(redacted.take(4)).isEqualTo("user")
        assertThat(redacted.takeLast(4)).isEqualTo("6789")
    }

    @Test
    fun `redactApiKey preserves total length`() {
        // Given: Various API keys
        val keys =
            listOf(
                "user_abc123xyz789",
                "api_key_test_123456",
                "super_long_api_key_with_many_characters_12345",
            )

        keys.forEach { key ->
            // When: Redact each key
            val redacted = redactApiKey(key)

            // Then: Length should be preserved (or replaced with ****)
            if (key.length > 8) {
                assertThat(redacted)
                    .hasLength(key.length)
            }
        }
    }

    @Test
    fun `redactApiKey shows correct prefix and suffix`() {
        // Given: Known API key
        val apiKey = "user_abc123xyz789"

        // When: Redact
        val redacted = redactApiKey(apiKey)

        // Then: Verify prefix and suffix
        assertThat(redacted.take(4)).isEqualTo("user")
        assertThat(redacted.takeLast(4)).isEqualTo("z789")
    }

    @Test
    fun `redactApiKey handles special characters`() {
        // Given: API key with special characters
        val apiKey = "user-key_123!@#"

        // When: Redact
        val redacted = redactApiKey(apiKey)

        // Then: Should preserve special characters at start and end
        assertThat(redacted).hasLength(apiKey.length)
        assertThat(redacted.take(4)).isEqualTo("user")
        assertThat(redacted.takeLast(4)).isEqualTo("3!@#") // Last 4 chars
    }

    // ========== Presenter Tests ==========
    // These tests verify the fix for clearing device tokens on logout

    @Test
    fun `confirm logout clears both user preferences and device tokens`() =
        runTest {
            // Given: Setup repositories with data
            val navigator = FakeNavigator(UserAccountScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()

            userPrefsRepo.saveApiToken("test_api_token")
            deviceTokenRepo.saveDeviceToken("DEV-001", "token_001")
            deviceTokenRepo.saveDeviceToken("DEV-002", "token_002")

            val presenter = UserAccountPresenter(navigator, userPrefsRepo, deviceTokenRepo)

            // When: Trigger logout via presenter
            presenter.test {
                val state = awaitItem()
                state.eventSink(UserAccountScreen.Event.LogoutClicked)
                val dialogState = awaitItem()
                dialogState.eventSink(UserAccountScreen.Event.ConfirmLogout)

                // Then: Both clearAll() methods should be called
                assertThat(userPrefsRepo.wasCleared).isTrue()
                assertThat(deviceTokenRepo.wasCleared).isTrue()
                assertThat(deviceTokenRepo.getAllTokens()).isEqualTo(emptyMap())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `device token repository clearAll removes all tokens`() =
        runTest {
            // Given: Device token repository with multiple tokens
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("DEV-001", "token_001")
            repository.saveDeviceToken("DEV-002", "token_002")
            repository.saveDeviceToken("DEV-003", "token_003")

            // Verify tokens exist
            assertThat(repository.getAllTokens().size).isEqualTo(3)

            // When: clearAll() is called
            repository.clearAll()

            // Then: All tokens should be removed and flag should be set
            assertThat(repository.wasCleared).isTrue()
            assertThat(repository.getAllTokens()).isEqualTo(emptyMap())
        }
}

/**
 * Helper function to test API key redaction logic.
 * This mirrors the logic from UserAccountScreen.kt's private redactApiKey function.
 * We keep this copy for testing purposes to validate the security-critical redaction logic.
 */
private fun redactApiKey(apiKey: String): String =
    when {
        apiKey.length <= 8 -> "****" // Too short, fully redact
        else -> {
            val prefix = apiKey.take(4)
            val suffix = apiKey.takeLast(4)
            val middleLength = (apiKey.length - 8).coerceAtLeast(4)
            "$prefix${"*".repeat(middleLength)}$suffix"
        }
    }

// ========== Test Fakes ==========

/**
 * Fake implementation of UserPreferencesRepository for testing.
 * Tracks whether clearAll() was called to verify logout behavior.
 */
private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val preferences = mutableMapOf<String, Any>()
    var wasCleared = false
        private set

    override val userPreferencesFlow =
        kotlinx.coroutines.flow.MutableStateFlow(
            ink.trmnl.android.buddy.data.preferences.UserPreferences(
                apiToken = preferences["api_token"] as? String,
                isOnboardingCompleted = preferences["is_onboarding_completed"] as? Boolean ?: false,
            ),
        )

    override suspend fun saveApiToken(token: String) {
        preferences["api_token"] = token
        userPreferencesFlow.value =
            userPreferencesFlow.value.copy(apiToken = token)
    }

    override suspend fun clearApiToken() {
        preferences.remove("api_token")
        userPreferencesFlow.value =
            userPreferencesFlow.value.copy(apiToken = null)
    }

    override suspend fun setOnboardingCompleted() {
        preferences["is_onboarding_completed"] = true
        userPreferencesFlow.value =
            userPreferencesFlow.value.copy(isOnboardingCompleted = true)
    }

    override suspend fun clearAll() {
        wasCleared = true
        preferences.clear()
        userPreferencesFlow.value =
            ink.trmnl.android.buddy.data.preferences.UserPreferences(
                apiToken = null,
                isOnboardingCompleted = false,
            )
    }
}

/**
 * Fake implementation of DeviceTokenRepository for testing.
 * This is a proper fake that implements the interface, following the project's testing guidelines.
 * Tracks whether clearAll() was called to verify the fix for clearing device tokens on logout.
 */
private class FakeDeviceTokenRepository : DeviceTokenRepository {
    private val deviceTokens = mutableMapOf<String, String>()
    var wasCleared = false
        private set

    override suspend fun saveDeviceToken(
        deviceFriendlyId: String,
        token: String,
    ) {
        deviceTokens[deviceFriendlyId] = token
    }

    override suspend fun getDeviceToken(deviceFriendlyId: String): String? = deviceTokens[deviceFriendlyId]

    override fun getDeviceTokenFlow(deviceFriendlyId: String): kotlinx.coroutines.flow.Flow<String?> =
        kotlinx.coroutines.flow.flowOf(deviceTokens[deviceFriendlyId])

    override suspend fun clearDeviceToken(deviceFriendlyId: String) {
        deviceTokens.remove(deviceFriendlyId)
    }

    override suspend fun hasDeviceToken(deviceFriendlyId: String): Boolean = deviceTokens.containsKey(deviceFriendlyId)

    /**
     * Clear all device tokens. This is the fix that was added.
     */
    override suspend fun clearAll() {
        wasCleared = true
        deviceTokens.clear()
    }

    /**
     * Helper method for testing - get all stored tokens.
     */
    fun getAllTokens(): Map<String, String> = deviceTokens.toMap()
}
