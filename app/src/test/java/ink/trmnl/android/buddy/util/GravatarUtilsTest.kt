package ink.trmnl.android.buddy.util

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import org.junit.Test

/**
 * Unit tests for GravatarUtils.
 * Tests Gravatar URL generation and MD5 hashing.
 */
class GravatarUtilsTest {
    @Test
    fun `getGravatarUrl generates correct URL for email`() {
        // Given: A known email
        val email = "test@example.com"

        // When: Generate Gravatar URL
        val url = GravatarUtils.getGravatarUrl(email)

        // Then: URL should have correct format
        assertThat(url).startsWith("https://www.gravatar.com/avatar/")
        assertThat(url).contains("?s=200")
        assertThat(url).contains("&d=mp")
    }

    @Test
    fun `getGravatarUrl uses MD5 hash of lowercase trimmed email`() {
        // Given: Email with uppercase and spaces
        val email = "  Test@Example.COM  "

        // When: Generate Gravatar URL
        val url = GravatarUtils.getGravatarUrl(email)

        // Then: Should use MD5 hash of "test@example.com"
        // MD5 hash of "test@example.com" is "55502f40dc8b7c769880b10874abc9d0"
        assertThat(url).contains("55502f40dc8b7c769880b10874abc9d0")
    }

    @Test
    fun `getGravatarUrl with custom size`() {
        // Given: Email and custom size
        val email = "user@example.com"
        val size = 400

        // When: Generate Gravatar URL with custom size
        val url = GravatarUtils.getGravatarUrl(email, size = size)

        // Then: URL should include custom size
        assertThat(url).contains("?s=400")
    }

    @Test
    fun `getGravatarUrl with custom default image`() {
        // Given: Email and custom default image
        val email = "user@example.com"
        val defaultImage = "identicon"

        // When: Generate Gravatar URL with custom default
        val url = GravatarUtils.getGravatarUrl(email, defaultImage = defaultImage)

        // Then: URL should include custom default image
        assertThat(url).contains("&d=identicon")
    }

    @Test
    fun `getGravatarUrl with all custom parameters`() {
        // Given: Email with all custom parameters
        val email = "custom@example.com"
        val size = 512
        val defaultImage = "robohash"

        // When: Generate Gravatar URL
        val url = GravatarUtils.getGravatarUrl(email, size = size, defaultImage = defaultImage)

        // Then: URL should include all parameters
        assertThat(url).contains("?s=512")
        assertThat(url).contains("&d=robohash")
    }

    @Test
    fun `getGravatarUrl generates consistent hash for same email`() {
        // Given: Same email used twice
        val email = "consistent@example.com"

        // When: Generate URLs multiple times
        val url1 = GravatarUtils.getGravatarUrl(email)
        val url2 = GravatarUtils.getGravatarUrl(email)

        // Then: URLs should be identical
        assertThat(url1).isEqualTo(url2)
    }

    @Test
    fun `getGravatarUrl handles email case insensitivity`() {
        // Given: Same email in different cases
        val emailLower = "user@example.com"
        val emailUpper = "USER@EXAMPLE.COM"
        val emailMixed = "User@Example.Com"

        // When: Generate URLs for each
        val urlLower = GravatarUtils.getGravatarUrl(emailLower)
        val urlUpper = GravatarUtils.getGravatarUrl(emailUpper)
        val urlMixed = GravatarUtils.getGravatarUrl(emailMixed)

        // Then: All should produce the same URL
        assertThat(urlLower).isEqualTo(urlUpper)
        assertThat(urlLower).isEqualTo(urlMixed)
    }

    @Test
    fun `getGravatarUrl trims whitespace from email`() {
        // Given: Email with various whitespace
        val emailWithSpaces = "  user@example.com  "
        val emailClean = "user@example.com"

        // When: Generate URLs
        val urlWithSpaces = GravatarUtils.getGravatarUrl(emailWithSpaces)
        val urlClean = GravatarUtils.getGravatarUrl(emailClean)

        // Then: Should produce same URL
        assertThat(urlWithSpaces).isEqualTo(urlClean)
    }
}
