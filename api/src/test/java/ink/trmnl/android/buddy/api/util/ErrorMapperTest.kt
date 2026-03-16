package ink.trmnl.android.buddy.api.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.slack.eithernet.ApiResult
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [ErrorMapper] utility.
 *
 * These tests verify that API failures are correctly mapped to user-friendly error messages
 * for display in the UI, ensuring consistent error messaging across the app.
 */
class ErrorMapperTest {
    // region mapToUserMessage

    @Test
    fun `mapToUserMessage - HttpFailure 401 returns unauthorized message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 401)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Unauthorized. Please check your API token.")
    }

    @Test
    fun `mapToUserMessage - HttpFailure 403 returns forbidden message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 403)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Forbidden. You don't have access to this resource.")
    }

    @Test
    fun `mapToUserMessage - HttpFailure 404 returns not found message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 404)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Resource not found.")
    }

    @Test
    fun `mapToUserMessage - HttpFailure 429 returns too many requests message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 429)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Too many requests. Please try again later.")
    }

    @Test
    fun `mapToUserMessage - HttpFailure 500 returns server error message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 500)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Server error. Please try again later.")
    }

    @Test
    fun `mapToUserMessage - HttpFailure 503 returns server error message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 503)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Server error. Please try again later.")
    }

    @Test
    fun `mapToUserMessage - HttpFailure unknown code returns generic HTTP error message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 418)

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("HTTP Error 418.")
    }

    @Test
    fun `mapToUserMessage - NetworkFailure returns connection error message`() {
        val failure = ApiResult.networkFailure(IOException("Connection refused"))

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Network error. Please check your connection.")
    }

    @Test
    fun `mapToUserMessage - ApiFailure with error returns error string`() {
        val failure = ApiResult.apiFailure("Bad request body")

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Bad request body")
    }

    @Test
    fun `mapToUserMessage - ApiFailure with null error returns fallback message`() {
        val failure = ApiResult.apiFailure<Nothing>()

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("An error occurred.")
    }

    @Test
    fun `mapToUserMessage - UnknownFailure returns unexpected error message`() {
        val failure = ApiResult.unknownFailure(RuntimeException("Something went wrong"))

        val message = ErrorMapper.mapToUserMessage(failure)

        assertThat(message).isEqualTo("Unexpected error. Please try again.")
    }

    // endregion

    // region mapHttpError

    @Test
    fun `mapHttpError - 401 returns unauthorized message`() {
        val message = ErrorMapper.mapHttpError(401)

        assertThat(message).isEqualTo("Unauthorized. Please check your API token.")
    }

    @Test
    fun `mapHttpError - 403 returns forbidden message`() {
        val message = ErrorMapper.mapHttpError(403)

        assertThat(message).isEqualTo("Forbidden. You don't have access to this resource.")
    }

    @Test
    fun `mapHttpError - 404 returns not found message`() {
        val message = ErrorMapper.mapHttpError(404)

        assertThat(message).isEqualTo("Resource not found.")
    }

    @Test
    fun `mapHttpError - 429 returns too many requests message`() {
        val message = ErrorMapper.mapHttpError(429)

        assertThat(message).isEqualTo("Too many requests. Please try again later.")
    }

    @Test
    fun `mapHttpError - 500 returns server error message`() {
        val message = ErrorMapper.mapHttpError(500)

        assertThat(message).isEqualTo("Server error. Please try again later.")
    }

    @Test
    fun `mapHttpError - 599 returns server error message`() {
        val message = ErrorMapper.mapHttpError(599)

        assertThat(message).isEqualTo("Server error. Please try again later.")
    }

    @Test
    fun `mapHttpError - unknown code returns generic HTTP error with code`() {
        val message = ErrorMapper.mapHttpError(302)

        assertThat(message).isEqualTo("HTTP Error 302.")
    }

    // endregion

    // region toUserMessage extension function

    @Test
    fun `toUserMessage extension - HttpFailure 401 returns unauthorized message`() {
        val failure = ApiResult.httpFailure<Nothing>(code = 401)

        val message = failure.toUserMessage()

        assertThat(message).isEqualTo("Unauthorized. Please check your API token.")
    }

    @Test
    fun `toUserMessage extension - NetworkFailure returns connection error message`() {
        val failure = ApiResult.networkFailure(IOException("Timeout"))

        val message = failure.toUserMessage()

        assertThat(message).isEqualTo("Network error. Please check your connection.")
    }

    @Test
    fun `toUserMessage extension - UnknownFailure returns unexpected error message`() {
        val failure = ApiResult.unknownFailure(RuntimeException("Oops"))

        val message = failure.toUserMessage()

        assertThat(message).isEqualTo("Unexpected error. Please try again.")
    }

    // endregion
}
