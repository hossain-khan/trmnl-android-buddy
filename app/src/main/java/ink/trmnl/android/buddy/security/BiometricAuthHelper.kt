package ink.trmnl.android.buddy.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper class for biometric authentication operations.
 * Follows Android best practices from:
 * - https://developer.android.com/identity/sign-in/biometric-auth
 * - https://medium.com/androiddevelopers/migrating-from-fingerprintmanager-to-biometricprompt-4bc5f570dccd
 */
class BiometricAuthHelper(
    private val context: Context,
) {
    /**
     * Check if biometric authentication (strong biometric or device credential) is available.
     * This includes fingerprint, face unlock, and device PIN/pattern/password.
     * @return true if any form of authentication is available, false otherwise
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> false
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> false
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false
            else -> false
        }
    }

    /**
     * Show biometric authentication prompt using device credentials.
     * This will show the device's native authentication UI (fingerprint, face, PIN, pattern, or password).
     *
     * @param activity The activity to show the prompt in
     * @param title Title for the biometric prompt
     * @param subtitle Subtitle for the biometric prompt (optional)
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when authentication fails or is cancelled
     * @param onUserCancelled Callback when user explicitly cancels authentication
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUserCancelled: () -> Unit = {},
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        super.onAuthenticationError(errorCode, errString)
                        // User cancelled authentication
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            onUserCancelled()
                        } else {
                            onError(errString.toString())
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        // This is called when biometric is recognized but not verified
                        // Don't show error here, let user retry
                    }
                },
            )

        // Use BIOMETRIC_STRONG | DEVICE_CREDENTIAL to allow both biometric and device PIN/pattern/password
        // This follows Android best practices and provides better UX
        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .apply {
                    if (subtitle.isNotEmpty()) {
                        setSubtitle(subtitle)
                    }
                }.setAllowedAuthenticators(AUTHENTICATORS)
                .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        /**
         * Use BIOMETRIC_STRONG (fingerprint, face, iris) OR DEVICE_CREDENTIAL (PIN, pattern, password).
         * This allows users to authenticate using either strong biometric or their device credential.
         * Following Android best practices: https://developer.android.com/identity/sign-in/biometric-auth
         */
        private const val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
