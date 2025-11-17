package ink.trmnl.android.buddy.ui.devicetoken

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import kotlinx.coroutines.launch

/**
 * Presenter for DeviceTokenScreen.
 */
@Inject
class DeviceTokenPresenter
    constructor(
        @Assisted private val screen: DeviceTokenScreen,
        @Assisted private val navigator: Navigator,
        private val deviceTokenRepository: DeviceTokenRepository,
    ) : Presenter<DeviceTokenScreen.State> {
        @Composable
        override fun present(): DeviceTokenScreen.State {
            var currentToken by rememberRetained { mutableStateOf("") }
            var tokenInput by rememberRetained { mutableStateOf("") }
            var isSaving by rememberRetained { mutableStateOf(false) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Load existing token on initial load
            LaunchedEffect(screen.deviceFriendlyId) {
                val token = deviceTokenRepository.getDeviceToken(screen.deviceFriendlyId)
                if (token != null) {
                    currentToken = token
                    tokenInput = token
                }
            }

            return DeviceTokenScreen.State(
                deviceFriendlyId = screen.deviceFriendlyId,
                deviceName = screen.deviceName,
                currentToken = currentToken,
                tokenInput = tokenInput,
                isSaving = isSaving,
                errorMessage = errorMessage,
            ) { event ->
                when (event) {
                    is DeviceTokenScreen.Event.TokenChanged -> {
                        tokenInput = event.token
                        errorMessage = null
                    }

                    DeviceTokenScreen.Event.SaveToken -> {
                        val trimmedToken = tokenInput.trim()
                        if (trimmedToken.isBlank()) {
                            errorMessage = "Token cannot be empty"
                            return@State
                        }
                        if (trimmedToken.length < 20) {
                            errorMessage = "Token must be at least 20 characters long"
                            return@State
                        }

                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                deviceTokenRepository.saveDeviceToken(screen.deviceFriendlyId, trimmedToken)
                                // Navigate back to devices list
                                navigator.pop()
                            } catch (e: Exception) {
                                errorMessage = "Failed to save token: ${e.message}"
                                isSaving = false
                            }
                        }
                    }

                    DeviceTokenScreen.Event.ClearToken -> {
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                deviceTokenRepository.clearDeviceToken(screen.deviceFriendlyId)
                                currentToken = ""
                                tokenInput = ""
                                isSaving = false
                            } catch (e: Exception) {
                                errorMessage = "Failed to clear token: ${e.message}"
                                isSaving = false
                            }
                        }
                    }

                    DeviceTokenScreen.Event.BackClicked -> {
                        navigator.pop()
                    }
                }
            }
        }

        @CircuitInject(DeviceTokenScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: DeviceTokenScreen,
                navigator: Navigator,
            ): DeviceTokenPresenter
        }
    }
