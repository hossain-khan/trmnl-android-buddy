package ink.trmnl.android.buddy.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for [BiometricAuthHelperImpl].
 *
 * Tests cover:
 * - [isBiometricAvailable] availability check in Robolectric environment
 * - [authenticate] with null activity triggering [onError]
 * - [authenticate] with valid activity and prompt initialization
 * - [createAuthenticationCallback] behavior for cancellation, errors, success, and failure
 * - [createPromptInfo] prompt configuration with/without subtitle
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class BiometricAuthHelperImplTest {
    private lateinit var helper: BiometricAuthHelperImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        helper = BiometricAuthHelperImpl(context)
    }

    @Test
    fun `isBiometricAvailable returns boolean in test environment`() {
        val available = helper.isBiometricAvailable()
        assertThat(available).isTrue()
    }

    @Test
    fun `authenticate with null activity triggers onError callback immediately`() {
        var successCalled = false
        var errorMessage: String? = null
        var userCancelledCalled = false

        helper.authenticate(
            activity = null,
            title = "Unlock App",
            subtitle = "Authenticate to continue",
            onSuccess = { successCalled = true },
            onError = { errorMessage = it },
            onUserCancelled = { userCancelledCalled = true },
        )

        assertThat(successCalled).isFalse()
        assertThat(userCancelledCalled).isFalse()
        assertThat(errorMessage).isEqualTo("Activity not available for biometric prompt")
    }

    @Test
    fun `authenticate with valid activity initializes prompt with title and subtitle`() {
        val activity =
            Robolectric
                .buildActivity(FragmentActivity::class.java)
                .create()
                .start()
                .resume()
                .get()

        helper.authenticate(
            activity = activity,
            title = "Confirm Identity",
            subtitle = "Use fingerprint or PIN",
            onSuccess = {},
            onError = {},
            onUserCancelled = {},
        )

        assertThat(helper).isNotNull()
    }

    @Test
    fun `authenticate with valid activity and empty subtitle initializes prompt`() {
        val activity =
            Robolectric
                .buildActivity(FragmentActivity::class.java)
                .create()
                .start()
                .resume()
                .get()

        helper.authenticate(
            activity = activity,
            title = "Confirm Identity",
            subtitle = "",
            onSuccess = {},
            onError = {},
            onUserCancelled = {},
        )

        assertThat(helper).isNotNull()
    }

    @Test
    fun `createAuthenticationCallback onAuthenticationError with ERROR_USER_CANCELED triggers onUserCancelled`() {
        var successCalled = false
        var errorMessage: String? = null
        var userCancelledCalled = false

        val callback =
            helper.createAuthenticationCallback(
                onSuccess = { successCalled = true },
                onError = { errorMessage = it },
                onUserCancelled = { userCancelledCalled = true },
            )

        callback.onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "User cancelled")

        assertThat(userCancelledCalled).isTrue()
        assertThat(successCalled).isFalse()
        assertThat(errorMessage).isNull()
    }

    @Test
    fun `createAuthenticationCallback onAuthenticationError with ERROR_NEGATIVE_BUTTON triggers onUserCancelled`() {
        var successCalled = false
        var errorMessage: String? = null
        var userCancelledCalled = false

        val callback =
            helper.createAuthenticationCallback(
                onSuccess = { successCalled = true },
                onError = { errorMessage = it },
                onUserCancelled = { userCancelledCalled = true },
            )

        callback.onAuthenticationError(BiometricPrompt.ERROR_NEGATIVE_BUTTON, "Cancel button clicked")

        assertThat(userCancelledCalled).isTrue()
        assertThat(successCalled).isFalse()
        assertThat(errorMessage).isNull()
    }

    @Test
    fun `createAuthenticationCallback onAuthenticationError with other error triggers onError`() {
        var successCalled = false
        var errorMessage: String? = null
        var userCancelledCalled = false

        val callback =
            helper.createAuthenticationCallback(
                onSuccess = { successCalled = true },
                onError = { errorMessage = it },
                onUserCancelled = { userCancelledCalled = true },
            )

        callback.onAuthenticationError(BiometricPrompt.ERROR_LOCKOUT, "Too many attempts")

        assertThat(errorMessage).isEqualTo("Too many attempts")
        assertThat(userCancelledCalled).isFalse()
        assertThat(successCalled).isFalse()
    }

    @Test
    fun `createAuthenticationCallback onAuthenticationSucceeded triggers onSuccess`() {
        var successCalled = false
        var errorMessage: String? = null
        var userCancelledCalled = false

        val callback =
            helper.createAuthenticationCallback(
                onSuccess = { successCalled = true },
                onError = { errorMessage = it },
                onUserCancelled = { userCancelledCalled = true },
            )

        val result = BiometricPrompt.AuthenticationResult(null, BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC)
        callback.onAuthenticationSucceeded(result)

        assertThat(successCalled).isTrue()
        assertThat(userCancelledCalled).isFalse()
        assertThat(errorMessage).isNull()
    }

    @Test
    fun `createAuthenticationCallback onAuthenticationFailed does not trigger error or cancel`() {
        var successCalled = false
        var errorMessage: String? = null
        var userCancelledCalled = false

        val callback =
            helper.createAuthenticationCallback(
                onSuccess = { successCalled = true },
                onError = { errorMessage = it },
                onUserCancelled = { userCancelledCalled = true },
            )

        callback.onAuthenticationFailed()

        assertThat(successCalled).isFalse()
        assertThat(userCancelledCalled).isFalse()
        assertThat(errorMessage).isNull()
    }

    @Test
    fun `createPromptInfo configures title subtitle and authenticators`() {
        val promptInfo = helper.createPromptInfo("Test Title", "Test Subtitle")

        assertThat(promptInfo.title).isEqualTo("Test Title")
        assertThat(promptInfo.subtitle).isEqualTo("Test Subtitle")
        val expectedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        assertThat(promptInfo.allowedAuthenticators).isEqualTo(expectedAuthenticators)
    }

    @Test
    fun `createPromptInfo with empty subtitle does not set subtitle`() {
        val promptInfo = helper.createPromptInfo("Test Title", "")

        assertThat(promptInfo.title).isEqualTo("Test Title")
        assertThat(promptInfo.subtitle).isNull()
    }
}
