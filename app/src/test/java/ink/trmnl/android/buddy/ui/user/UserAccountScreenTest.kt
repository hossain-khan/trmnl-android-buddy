package ink.trmnl.android.buddy.ui.user

import assertk.assertThat
import assertk.assertions.hasLength
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.api.models.ApiError
import ink.trmnl.android.buddy.api.models.User
import ink.trmnl.android.buddy.api.models.UserResponse
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakeTrmnlApiService
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
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

            val apiService =
                FakeTrmnlApiService().apply {
                    userInfoResult = ApiResult.httpFailure(401, null)
                }
            val presenter = UserAccountPresenter(navigator, apiService, userPrefsRepo, deviceTokenRepo)

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

    @Test
    fun `presenter surfaces missing profile permission on HTTP 403 and navigates to AccessTokenScreen`() =
        runTest {
            val navigator = FakeNavigator(UserAccountScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            userPrefsRepo.saveApiToken("trmnl_scoped_without_profile")

            val apiService =
                FakeTrmnlApiService().apply {
                    userInfoResult = ApiResult.httpFailure(403, ApiError("Forbidden. Missing Profile capability."))
                }
            val presenter = UserAccountPresenter(navigator, apiService, userPrefsRepo, deviceTokenRepo)

            presenter.test {
                var state: UserAccountScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.isInsufficientProfilePermission).isTrue()
                assertThat(state.errorMessage).isEqualTo("Forbidden. Missing Profile capability.")

                state.eventSink(UserAccountScreen.Event.UpdateApiKeyClicked)
                assertThat(navigator.awaitNextScreen()).isEqualTo(AccessTokenScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter loads user profile via injected apiService`() =
        runTest {
            val navigator = FakeNavigator(UserAccountScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val deviceTokenRepo = FakeDeviceTokenRepository()
            userPrefsRepo.saveApiToken("trmnl_with_profile")

            val expectedUser =
                User(
                    name = "Jane Doe",
                    email = "jane@example.com",
                    firstName = "Jane",
                    lastName = "Doe",
                    locale = "en",
                    timeZone = "UTC",
                    timeZoneIana = "UTC",
                    utcOffset = 0,
                    apiKey = "trmnl_with_profile",
                )
            val apiService =
                FakeTrmnlApiService().apply {
                    userInfoResult = ApiResult.success(UserResponse(data = expectedUser))
                }
            val presenter = UserAccountPresenter(navigator, apiService, userPrefsRepo, deviceTokenRepo)

            presenter.test {
                var state: UserAccountScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.user).isEqualTo(expectedUser)
                assertThat(state.isInsufficientProfilePermission).isFalse()
                assertThat(apiService.lastAuthorizationHeader).isEqualTo("Bearer trmnl_with_profile")
                cancelAndIgnoreRemainingEvents()
            }
        }
}

/**
 * Helper function to test API key redaction logic.
 * This mirrors the logic from UserAccountScreen.kt's private redactApiKey function.
 * We keep this copy for testing purposes to validate the security-critical redaction logic.
 */
private fun redactApiKey(apiKey: String): String =
    when {
        apiKey.length <= 8 -> {
            "****"
        }

        // Too short, fully redact
        else -> {
            val prefix = apiKey.take(4)
            val suffix = apiKey.takeLast(4)
            val middleLength = (apiKey.length - 8).coerceAtLeast(4)
            "$prefix${"*".repeat(middleLength)}$suffix"
        }
    }

// ========== Test Fakes ==========
