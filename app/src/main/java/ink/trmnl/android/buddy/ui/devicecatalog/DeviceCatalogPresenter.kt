package ink.trmnl.android.buddy.ui.devicecatalog

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
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.api.util.toUserMessage
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter for DeviceCatalogScreen.
 *
 * Manages state and business logic for the device catalog screen including:
 * - Fetching device models from API
 * - Filtering devices by kind (TRMNL, Kindle, BYOD)
 * - Handling navigation events
 * - Managing loading and error states
 */
@Inject
class DeviceCatalogPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val apiService: TrmnlApiService,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<DeviceCatalogScreen.State> {
        @Composable
        override fun present(): DeviceCatalogScreen.State {
            var devices by rememberRetained { mutableStateOf<List<DeviceModel>>(emptyList()) }
            var selectedFilter by rememberRetained { mutableStateOf<DeviceKind?>(null) }
            var isLoading by rememberRetained { mutableStateOf(false) }
            var error by rememberRetained { mutableStateOf<String?>(null) }
            var selectedDevice by rememberRetained { mutableStateOf<DeviceModel?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Fetch device models on initial load
            LaunchedEffect(Unit) {
                fetchDeviceModels(
                    onLoading = { isLoading = it },
                    onSuccess = { models ->
                        devices = models
                        error = null
                    },
                    onError = { errorMessage ->
                        error = errorMessage
                    },
                )
            }

            return DeviceCatalogScreen.State(
                devices = devices,
                selectedFilter = selectedFilter,
                isLoading = isLoading,
                error = error,
                selectedDevice = selectedDevice,
            ) { event ->
                when (event) {
                    DeviceCatalogScreen.Event.BackClicked -> {
                        navigator.pop()
                    }

                    is DeviceCatalogScreen.Event.FilterSelected -> {
                        selectedFilter = event.kind
                    }

                    is DeviceCatalogScreen.Event.DeviceClicked -> {
                        selectedDevice = event.device
                    }

                    DeviceCatalogScreen.Event.DismissBottomSheet -> {
                        selectedDevice = null
                    }

                    DeviceCatalogScreen.Event.RetryClicked -> {
                        coroutineScope.launch {
                            fetchDeviceModels(
                                onLoading = { isLoading = it },
                                onSuccess = { models ->
                                    devices = models
                                    error = null
                                },
                                onError = { errorMessage ->
                                    error = errorMessage
                                },
                            )
                        }
                    }
                }
            }
        }

        /**
         * Fetches device models from the API.
         *
         * @param onLoading Callback for loading state changes
         * @param onSuccess Callback for successful fetch with device models
         * @param onError Callback for errors with error message
         */
        private suspend fun fetchDeviceModels(
            onLoading: (Boolean) -> Unit,
            onSuccess: (List<DeviceModel>) -> Unit,
            onError: (String) -> Unit,
        ) {
            onLoading(true)

            try {
                val preferences = userPreferencesRepository.userPreferencesFlow.first()
                val apiKey = preferences.apiToken

                if (apiKey.isNullOrBlank()) {
                    onError("API key not found. Please log in again.")
                    onLoading(false)
                    return
                }

                val authorization = "Bearer $apiKey"

                when (val result = apiService.getDeviceModels(authorization)) {
                    is ApiResult.Success -> {
                        onSuccess(result.value.data)
                    }

                    is ApiResult.Failure -> {
                        Timber.e("Error fetching device models: $result")
                        onError(result.toUserMessage())
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception fetching device models")
                onError("Error: ${e.message}")
            } finally {
                onLoading(false)
            }
        }

        @CircuitInject(DeviceCatalogScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): DeviceCatalogPresenter
        }
    }
