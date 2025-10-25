package ink.trmnl.android.buddy.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import org.junit.Test

/**
 * Unit tests for SecurityHelper.
 */
class SecurityHelperTest {
    @Test
    fun `hashPin returns consistent hash for same PIN`() {
        val pin = "1234"
        val hash1 = SecurityHelper.hashPin(pin)
        val hash2 = SecurityHelper.hashPin(pin)

        assertThat(hash1).isNotEqualTo("")
        assertThat(hash1).isNotEqualTo(pin)
        assertThat(hash1).isEqualTo(hash2)
    }

    @Test
    fun `hashPin returns different hashes for different PINs`() {
        val pin1 = "1234"
        val pin2 = "5678"
        val hash1 = SecurityHelper.hashPin(pin1)
        val hash2 = SecurityHelper.hashPin(pin2)

        assertThat(hash1).isNotEqualTo(hash2)
    }

    @Test
    fun `verifyPin returns true for correct PIN`() {
        val pin = "1234"
        val hash = SecurityHelper.hashPin(pin)

        val result = SecurityHelper.verifyPin(pin, hash)

        assertThat(result).isTrue()
    }

    @Test
    fun `verifyPin returns false for incorrect PIN`() {
        val correctPin = "1234"
        val incorrectPin = "5678"
        val hash = SecurityHelper.hashPin(correctPin)

        val result = SecurityHelper.verifyPin(incorrectPin, hash)

        assertThat(result).isFalse()
    }

    @Test
    fun `isValidPin returns true for valid 4-digit PIN`() {
        val result = SecurityHelper.isValidPin("1234")

        assertThat(result).isTrue()
    }

    @Test
    fun `isValidPin returns true for valid 6-digit PIN`() {
        val result = SecurityHelper.isValidPin("123456")

        assertThat(result).isTrue()
    }

    @Test
    fun `isValidPin returns false for PIN shorter than minimum length`() {
        val result = SecurityHelper.isValidPin("123")

        assertThat(result).isFalse()
    }

    @Test
    fun `isValidPin returns false for PIN with non-digit characters`() {
        val result = SecurityHelper.isValidPin("12a4")

        assertThat(result).isFalse()
    }

    @Test
    fun `isValidPin returns false for empty PIN`() {
        val result = SecurityHelper.isValidPin("")

        assertThat(result).isFalse()
    }

    @Test
    fun `isValidPin returns false for PIN with special characters`() {
        val result = SecurityHelper.isValidPin("12@4")

        assertThat(result).isFalse()
    }

    @Test
    fun `minimum PIN length is 4`() {
        assertThat(SecurityHelper.MIN_PIN_LENGTH).isEqualTo(4)
    }
}
