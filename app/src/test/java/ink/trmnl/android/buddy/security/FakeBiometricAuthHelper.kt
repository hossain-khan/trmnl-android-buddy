package ink.trmnl.android.buddy.security

import androidx.fragment.app.FragmentActivity

/**
 * Fake implementation of BiometricAuthHelper for testing.
 * Always reports biometric as available for test simplicity.
 */
class FakeBiometricAuthHelper(
    private val isAvailable: Boolean = true,
) : BiometricAuthHelper {
    override fun isBiometricAvailable(): Boolean = isAvailable

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUserCancelled: () -> Unit,
    ) {
        // Fake implementation - does nothing in tests
        // Real authentication is tested at integration level
    }
}
