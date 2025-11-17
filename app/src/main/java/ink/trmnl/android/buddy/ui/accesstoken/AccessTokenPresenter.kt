package ink.trmnl.android.buddy.ui.accesstoken

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import kotlinx.coroutines.launch

/**
 * Presenter for AccessTokenScreen.
 * Handles token input, validation, and saving to DataStore.
 */
@Inject
class AccessTokenPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<AccessTokenScreen.State> {
        @Composable
        override fun present(): AccessTokenScreen.State {
            var token by rememberRetained { mutableStateOf("") }
            var isLoading by rememberRetained { mutableStateOf(false) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            return AccessTokenScreen.State(
                token = token,
                isLoading = isLoading,
                errorMessage = errorMessage,
            ) { event ->
                when (event) {
                    is AccessTokenScreen.Event.TokenChanged -> {
                        token = event.token
                        errorMessage = null // Clear error when user types
                    }

                    AccessTokenScreen.Event.SaveClicked -> {
                        when {
                            token.isBlank() -> {
                                errorMessage = "Token cannot be empty"
                            }

                            token.length < 10 -> {
                                errorMessage = "Token appears to be too short"
                            }

                            else -> {
                                isLoading = true
                                errorMessage = null

                                coroutineScope.launch {
                                    try {
                                        // Save the token
                                        userPreferencesRepository.saveApiToken(token.trim())

                                        // Mark onboarding as completed
                                        userPreferencesRepository.setOnboardingCompleted()

                                        // Navigate to devices list screen (resetRoot to prevent back navigation)
                                        navigator.resetRoot(TrmnlDevicesScreen)
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to save token: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }

                    AccessTokenScreen.Event.BackClicked -> {
                        navigator.pop()
                    }
                }
            }
        }

        @CircuitInject(AccessTokenScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): AccessTokenPresenter
        }
    }
