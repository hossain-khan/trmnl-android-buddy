package ink.trmnl.android.buddy.security

import androidx.fragment.app.FragmentActivity

/**
 * Fake implementation of BiometricAuthHelper for testing.
 */
class FakeBiometricAuthHelper(
    var isAvailable: Boolean = true,
    var authBehavior: AuthBehavior = AuthBehavior.ImmediateSuccess,
) : BiometricAuthHelper {
    enum class AuthBehavior {
        ImmediateSuccess,
        ImmediateError,
        ImmediateUserCancelled,
        Manual,
    }

    var lastActivity: FragmentActivity? = null
    var lastTitle: String? = null
    var lastSubtitle: String? = null
    var onSuccessCallback: (() -> Unit)? = null
    var onErrorCallback: ((String) -> Unit)? = null
    var onUserCancelledCallback: (() -> Unit)? = null
    var authenticateCallCount: Int = 0

    override fun isBiometricAvailable(): Boolean = isAvailable

    override fun authenticate(
        activity: FragmentActivity?,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onUserCancelled: () -> Unit,
    ) {
        authenticateCallCount++
        lastActivity = activity
        lastTitle = title
        lastSubtitle = subtitle
        onSuccessCallback = onSuccess
        onErrorCallback = onError
        onUserCancelledCallback = onUserCancelled

        when (authBehavior) {
            AuthBehavior.ImmediateSuccess -> onSuccess()
            AuthBehavior.ImmediateError -> onError("Authentication failed")
            AuthBehavior.ImmediateUserCancelled -> onUserCancelled()
            AuthBehavior.Manual -> Unit
        }
    }
}
