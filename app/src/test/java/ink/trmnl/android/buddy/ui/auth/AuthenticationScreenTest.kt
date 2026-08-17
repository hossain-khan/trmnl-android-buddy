package ink.trmnl.android.buddy.ui.auth

import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.fakes.FakeUserPreferencesRepository
import ink.trmnl.android.buddy.security.BiometricAuthHelperImpl
import ink.trmnl.android.buddy.security.FakeBiometricAuthHelper
import ink.trmnl.android.buddy.security.FakeBiometricAuthHelper.AuthBehavior
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Unit tests for [AuthenticationPresenter] and [AuthenticationScreen].
 *
 * Tests the complete authentication flow, biometric availability checks,
 * retry states, security preference cancellation, and navigation logic.
 */
@RunWith(RobolectricTestRunner::class)
class AuthenticationScreenTest {
    // ========== Presenter State & Event Tests ==========

    @Test
    fun `initial state checks and enables biometric availability when supported`() =
        runTest {
            val navigator = FakeNavigator(AuthenticationScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val biometricHelper = FakeBiometricAuthHelper(isAvailable = true)
            val presenter = AuthenticationPresenter(navigator, userPrefsRepo, biometricHelper)

            presenter.test {
                // Initial state before LaunchedEffect runs
                val initialState = awaitItem()
                assertThat(initialState.isAuthenticationAvailable).isFalse()
                assertThat(initialState.showRetryPrompt).isFalse()

                // State after LaunchedEffect checks availability
                val availableState = awaitItem()
                assertThat(availableState.isAuthenticationAvailable).isTrue()
                assertThat(availableState.showRetryPrompt).isFalse()
            }
        }

    @Test
    fun `initial state keeps biometric availability false when not supported`() =
        runTest {
            val navigator = FakeNavigator(AuthenticationScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val biometricHelper = FakeBiometricAuthHelper(isAvailable = false)
            val presenter = AuthenticationPresenter(navigator, userPrefsRepo, biometricHelper)

            presenter.test {
                val state = awaitItem()
                assertThat(state.isAuthenticationAvailable).isFalse()
                assertThat(state.showRetryPrompt).isFalse()
                expectNoEvents()
            }
        }

    @Test
    fun `AuthenticateRequested triggers biometric prompt and resets root to TrmnlDevicesScreen on success`() =
        runTest {
            val navigator = FakeNavigator(AuthenticationScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val biometricHelper =
                FakeBiometricAuthHelper(
                    isAvailable = true,
                    authBehavior = AuthBehavior.ImmediateSuccess,
                )
            val presenter = AuthenticationPresenter(navigator, userPrefsRepo, biometricHelper)

            presenter.test {
                awaitItem() // Initial frame
                val state = awaitItem()
                assertThat(state.isAuthenticationAvailable).isTrue()

                // Trigger authentication
                state.eventSink(AuthenticationScreen.Event.AuthenticateRequested)

                // Verify navigation to main screen
                assertThat(navigator.awaitResetRoot().newRoot).isEqualTo(TrmnlDevicesScreen)
                assertThat(biometricHelper.authenticateCallCount).isEqualTo(1)
            }
        }

    @Test
    fun `AuthenticateRequested sets showRetryPrompt to true when authentication fails with error`() =
        runTest {
            val navigator = FakeNavigator(AuthenticationScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val biometricHelper =
                FakeBiometricAuthHelper(
                    isAvailable = true,
                    authBehavior = AuthBehavior.ImmediateError,
                )
            val presenter = AuthenticationPresenter(navigator, userPrefsRepo, biometricHelper)

            presenter.test {
                awaitItem() // Initial frame
                val state = awaitItem()
                assertThat(state.showRetryPrompt).isFalse()

                // Trigger authentication that fails
                state.eventSink(AuthenticationScreen.Event.AuthenticateRequested)

                val retryState = awaitItem()
                assertThat(retryState.showRetryPrompt).isTrue()
                assertThat(biometricHelper.authenticateCallCount).isEqualTo(1)
            }
        }

    @Test
    fun `AuthenticateRequested sets showRetryPrompt to true when user cancels biometric prompt`() =
        runTest {
            val navigator = FakeNavigator(AuthenticationScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val biometricHelper =
                FakeBiometricAuthHelper(
                    isAvailable = true,
                    authBehavior = AuthBehavior.ImmediateUserCancelled,
                )
            val presenter = AuthenticationPresenter(navigator, userPrefsRepo, biometricHelper)

            presenter.test {
                awaitItem() // Initial frame
                val state = awaitItem()
                assertThat(state.showRetryPrompt).isFalse()

                // Trigger authentication that is cancelled by user
                state.eventSink(AuthenticationScreen.Event.AuthenticateRequested)

                val retryState = awaitItem()
                assertThat(retryState.showRetryPrompt).isTrue()
                assertThat(biometricHelper.authenticateCallCount).isEqualTo(1)
            }
        }

    @Test
    fun `CancelAuthentication disables security in repository and navigates to TrmnlDevicesScreen`() =
        runTest {
            val navigator = FakeNavigator(AuthenticationScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = true),
                )
            val biometricHelper = FakeBiometricAuthHelper(isAvailable = true)
            val presenter = AuthenticationPresenter(navigator, userPrefsRepo, biometricHelper)

            presenter.test {
                awaitItem() // Initial frame
                val state = awaitItem()

                // User cancels authentication
                state.eventSink(AuthenticationScreen.Event.CancelAuthentication)

                // Verify security is disabled in preferences
                assertThat(userPrefsRepo.securityEnabled).isFalse()

                // Verify navigation to main screen
                assertThat(navigator.awaitResetRoot().newRoot).isEqualTo(TrmnlDevicesScreen)
            }
        }

    // ========== BiometricAuthHelperImpl Tests ==========

    @Test
    fun `BiometricAuthHelperImpl authenticate with null activity invokes error callback`() {
        val helper = BiometricAuthHelperImpl(ApplicationProvider.getApplicationContext())
        var errorCalled = false
        helper.authenticate(
            activity = null,
            title = "Test Auth",
            onSuccess = {},
            onError = { errorCalled = true },
            onUserCancelled = {},
        )
        assertThat(errorCalled).isTrue()
    }

    // ========== Repository Integration Tests ==========

    @Test
    fun `setSecurityEnabled updates repository correctly when enabled`() =
        runTest {
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = false),
                )

            userPrefsRepo.setSecurityEnabled(true)

            assertThat(userPrefsRepo.securityEnabled).isTrue()
        }

    @Test
    fun `setSecurityEnabled updates repository correctly when disabled`() =
        runTest {
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = true),
                )

            userPrefsRepo.setSecurityEnabled(false)

            assertThat(userPrefsRepo.securityEnabled).isFalse()
        }

    @Test
    fun `biometric helper reports availability correctly`() {
        val biometricAuthHelperAvailable = FakeBiometricAuthHelper(isAvailable = true)
        val biometricAuthHelperNotAvailable = FakeBiometricAuthHelper(isAvailable = false)

        assertThat(biometricAuthHelperAvailable.isBiometricAvailable()).isTrue()
        assertThat(biometricAuthHelperNotAvailable.isBiometricAvailable()).isFalse()
    }

    @Test
    fun `security preference persists across repository updates`() =
        runTest {
            val userPrefsRepo = FakeUserPreferencesRepository()

            // Initially false (default)
            assertThat(userPrefsRepo.securityEnabled).isFalse()

            // Enable security
            userPrefsRepo.setSecurityEnabled(true)
            assertThat(userPrefsRepo.securityEnabled).isTrue()

            // Disable security
            userPrefsRepo.setSecurityEnabled(false)
            assertThat(userPrefsRepo.securityEnabled).isFalse()
        }

    @Test
    fun `clearAll resets security to default`() =
        runTest {
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isSecurityEnabled = true),
                )

            assertThat(userPrefsRepo.securityEnabled).isTrue()

            userPrefsRepo.clearAll()

            assertThat(userPrefsRepo.securityEnabled).isFalse()
        }
}
