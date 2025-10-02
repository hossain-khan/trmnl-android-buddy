package ink.trmnl.android.buddy.util

import java.security.MessageDigest

/**
 * Utility functions for working with Gravatar.
 * Gravatar provides globally recognized avatars based on email addresses.
 *
 * See: https://docs.gravatar.com/
 */
object GravatarUtils {
    /**
     * Generates a Gravatar URL for the given email address.
     *
     * @param email The email address to generate a Gravatar for
     * @param size The size of the avatar image in pixels (default: 200)
     * @param defaultImage The default image to show if no Gravatar exists
     *                     Options: "404", "mp" (mystery person), "identicon", "monsterid", "wavatar", "retro", "robohash", "blank"
     *                     Default: "mp" (mystery person)
     * @return The Gravatar URL
     */
    fun getGravatarUrl(
        email: String,
        size: Int = 200,
        defaultImage: String = "mp",
    ): String {
        val hash = email.trim().lowercase().toMD5()
        return "https://www.gravatar.com/avatar/$hash?s=$size&d=$defaultImage"
    }

    /**
     * Converts a string to its MD5 hash.
     * Gravatar uses MD5 hashes of email addresses.
     */
    private fun String.toMD5(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
