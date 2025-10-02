package ink.trmnl.android.buddy.ui.user

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for UserAccountScreen utility functions.
 */
class UserAccountScreenTest {
    @Test
    fun `redactApiKey handles normal API key correctly`() {
        // Given: A typical API key like "user_abc123xyz789"
        val apiKey = "user_abc123xyz789"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should show first 4 and last 4 with asterisks in middle
        // "user_abc123xyz789" is 18 chars, so middle is 18-8=10 chars
        println("Original: $apiKey (${apiKey.length} chars)")
        println("Redacted: $redacted (${redacted.length} chars)")
        assertEquals(apiKey.length, redacted.length)
        assertEquals("user", redacted.take(4))
        assertEquals("z789", redacted.takeLast(4))
    }

    @Test
    fun `redactApiKey handles short API key`() {
        // Given: A short API key (8 characters or less)
        val apiKey = "short123"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should fully redact short keys
        assertEquals("****", redacted)
    }

    @Test
    fun `redactApiKey handles very short API key`() {
        // Given: A very short API key
        val apiKey = "test"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should fully redact
        assertEquals("****", redacted)
    }

    @Test
    fun `redactApiKey handles exactly 9 character key`() {
        // Given: 9 character API key (just long enough to show prefix/suffix)
        val apiKey = "user_1234"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: Should show first 4, last 4, with at least 4 asterisks
        assertEquals("user****1234", redacted)
    }

    @Test
    fun `redactApiKey handles long API key`() {
        // Given: A long API key
        val apiKey = "user_abcdefghijklmnopqrstuvwxyz123456789"

        // When: Redact the API key
        val redacted = redactApiKey(apiKey)

        // Then: First 4 and last 4 should be visible
        // Length is 41, so middle is 41-8=33 chars
        println("Long Original: $apiKey (${apiKey.length} chars)")
        println("Long Redacted: $redacted (${redacted.length} chars)")
        assertEquals(apiKey.length, redacted.length) // Length should be preserved
        assertEquals("user", redacted.take(4))
        assertEquals("6789", redacted.takeLast(4))
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
                assertEquals(
                    "Length mismatch for key: $key",
                    key.length,
                    redacted.length,
                )
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
        assertEquals("user", redacted.take(4))
        assertEquals("z789", redacted.takeLast(4))
    }
}

/**
 * Helper function copied from UserAccountScreen.kt for testing.
 * This is a workaround since the actual function is private.
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
