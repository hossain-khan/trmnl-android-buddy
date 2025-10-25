package ink.trmnl.android.buddy.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Helper class for biometric authentication operations.
 */
class BiometricAuthHelper(
    private val context: Context,
) {
    /**
     * Check if biometric authentication is available on the device.
     * @return true if biometric authentication is available, false otherwise
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Show biometric authentication prompt.
     * @param activity The activity to show the prompt in
     * @param title Title for the biometric prompt
     * @param subtitle Subtitle for the biometric prompt
     * @param description Description for the biometric prompt
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when authentication fails or encounters an error
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String = "",
        description: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
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
                        onError(errString.toString())
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Authentication failed")
                    }
                },
            )

        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setNegativeButtonText("Use PIN")
                .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
