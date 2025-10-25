package ink.trmnl.android.buddy.security

import java.security.MessageDigest

/**
 * Helper object for security-related operations like PIN hashing.
 */
object SecurityHelper {
    /**
     * Hash a PIN using SHA-256.
     * @param pin The PIN to hash
     * @return The hashed PIN as a hex string
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verify if a PIN matches the stored hash.
     * @param pin The PIN to verify
     * @param hash The stored hash to compare against
     * @return true if the PIN matches the hash, false otherwise
     */
    fun verifyPin(
        pin: String,
        hash: String,
    ): Boolean = hashPin(pin) == hash

    /**
     * Validate if a PIN meets the minimum requirements.
     * @param pin The PIN to validate
     * @return true if the PIN is valid, false otherwise
     */
    fun isValidPin(pin: String): Boolean = pin.length >= MIN_PIN_LENGTH && pin.all { it.isDigit() }

    const val MIN_PIN_LENGTH = 4
}
