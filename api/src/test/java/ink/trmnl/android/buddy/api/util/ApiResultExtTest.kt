package ink.trmnl.android.buddy.api.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.eithernet.ApiResult
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [ApiResultExt] extension functions.
 *
 * Tests cover:
 * - [toResult] with custom transformer across all [ApiResult] variants
 * - [toResultDirect] without transformation
 * - Null safety for exception messages in NetworkFailure and UnknownFailure
 * - Contextual vs non-contextual HTTP failure messages
 */
class ApiResultExtTest {
    // region toResult with transform

    @Test
    fun `toResult - Success maps and transforms value to Result success`() {
        val apiResult: ApiResult<String, Nothing> = ApiResult.success("hello world")

        val result = apiResult.toResult { it.length }

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(11)
    }

    @Test
    fun `toResult - HttpFailure with context formats HTTP code and context`() {
        val apiResult: ApiResult<String, Nothing> = ApiResult.httpFailure(code = 404)

        val result = apiResult.toResult(context = "Failed to load device") { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("HTTP 404: Failed to load device")
    }

    @Test
    fun `toResult - HttpFailure without context formats HTTP code only`() {
        val apiResult: ApiResult<String, Nothing> = ApiResult.httpFailure(code = 500)

        val result = apiResult.toResult { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("HTTP 500")
    }

    @Test
    fun `toResult - NetworkFailure with message formats network error`() {
        val exception = IOException("Connection timed out")
        val apiResult: ApiResult<String, Nothing> = ApiResult.networkFailure(exception)

        val result = apiResult.toResult { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error: Connection timed out")
    }

    @Test
    fun `toResult - NetworkFailure with null message uses fallback text`() {
        val exception = IOException(null as String?)
        val apiResult: ApiResult<String, Nothing> = ApiResult.networkFailure(exception)

        val result = apiResult.toResult { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error: Unknown network issue")
    }

    @Test
    fun `toResult - ApiFailure formats API error message`() {
        val apiResult: ApiResult<String, String> = ApiResult.apiFailure("Invalid payload format")

        val result = apiResult.toResult { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("API error: Invalid payload format")
    }

    @Test
    fun `toResult - UnknownFailure with message formats unexpected error`() {
        val exception = RuntimeException("Fatal state exception")
        val apiResult: ApiResult<String, Nothing> = ApiResult.unknownFailure(exception)

        val result = apiResult.toResult { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Unknown error: Fatal state exception")
    }

    @Test
    fun `toResult - UnknownFailure with null message uses fallback text`() {
        val exception = RuntimeException(null as String?)
        val apiResult: ApiResult<String, Nothing> = ApiResult.unknownFailure(exception)

        val result = apiResult.toResult { it }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Unknown error: Unexpected error occurred")
    }

    // endregion

    // region toResultDirect

    @Test
    fun `toResultDirect - Success returns original value in Result`() {
        val apiResult: ApiResult<Int, Nothing> = ApiResult.success(42)

        val result = apiResult.toResultDirect()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(42)
    }

    @Test
    fun `toResultDirect - HttpFailure with context returns mapped failure`() {
        val apiResult: ApiResult<Int, Nothing> = ApiResult.httpFailure(code = 401)

        val result = apiResult.toResultDirect("Invalid token")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("HTTP 401: Invalid token")
    }

    @Test
    fun `toResultDirect - NetworkFailure returns mapped failure`() {
        val exception = IOException("Host unreachable")
        val apiResult: ApiResult<Int, Nothing> = ApiResult.networkFailure(exception)

        val result = apiResult.toResultDirect()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Network error: Host unreachable")
    }

    // endregion
}
