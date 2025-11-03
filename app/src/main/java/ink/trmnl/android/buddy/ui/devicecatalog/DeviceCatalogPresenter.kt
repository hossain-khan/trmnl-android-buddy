package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiService
import ink.trmnl.android.buddy.api.models.DeviceKind
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter for the Device Catalog screen.
 *
 * Handles fetching device models from the API and managing filter state.
 */
@Inject
class DeviceCatalogPresenter(
    @Assisted private val navigator: Navigator,
    private val apiService: TrmnlApiService,
    private val userPreferencesRepository: UserPreferencesRepository,
) : Presenter<DeviceCatalogScreen.State> {
    @Composable
    override fun present(): DeviceCatalogScreen.State {
        var devices by remember { mutableStateOf(emptyList<DeviceModel>()) }
        var selectedFilter by remember { mutableStateOf<DeviceKind?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        var retryTrigger by remember { mutableStateOf(0) }
        val coroutineScope = rememberCoroutineScope()

        // Load devices on initial composition and when retry is triggered
        LaunchedEffect(retryTrigger) {
            isLoading = true
            error = null
            loadDevices(
                onSuccess = { loadedDevices ->
                    devices = loadedDevices
                    isLoading = false
                    error = null
                },
                onError = { errorMessage ->
                    error = errorMessage
                    isLoading = false
                },
            )
        }

        // Calculate filtered devices based on selected filter
        val filteredDevices =
            remember(devices, selectedFilter) {
                val filter = selectedFilter
                when (filter) {
                    null -> devices
                    else -> devices.filter { it.kind == filter.name.lowercase() }
                }
            }

        return DeviceCatalogScreen.State(
            devices = devices,
            filteredDevices = filteredDevices,
            selectedFilter = selectedFilter,
            isLoading = isLoading,
            error = error,
            eventSink = { event ->
                when (event) {
                    DeviceCatalogScreen.Event.BackClicked -> navigator.pop()
                    is DeviceCatalogScreen.Event.FilterSelected -> {
                        selectedFilter = event.kind
                    }
                    is DeviceCatalogScreen.Event.DeviceClicked -> {
                        Timber.d("Device clicked: ${event.device.label}")
                        // TODO: Navigate to device details screen when implemented
                    }
                    DeviceCatalogScreen.Event.RetryClicked -> {
                        retryTrigger++
                    }
                }
            },
        )
    }

    private suspend fun loadDevices(
        onSuccess: (List<DeviceModel>) -> Unit,
        onError: (String) -> Unit,
    ) {
        // Get API token from preferences
        val apiToken = userPreferencesRepository.userPreferencesFlow.first().apiToken
        if (apiToken.isNullOrBlank()) {
            onError("API token not found")
            return
        }

        when (val result = apiService.getDeviceModels("Bearer $apiToken")) {
            is ApiResult.Success -> {
                Timber.d("Loaded ${result.value.data.size} device models")
                onSuccess(result.value.data)
            }
            is ApiResult.Failure.HttpFailure -> {
                val message = "Failed to load devices: HTTP ${result.code}"
                Timber.e(message)
                onError(message)
            }
            is ApiResult.Failure.NetworkFailure -> {
                val message = "Network error: ${result.error.message}"
                Timber.e(result.error, message)
                onError(message)
            }
            is ApiResult.Failure.ApiFailure -> {
                val message = "API error: ${result.error}"
                Timber.e(message)
                onError(message)
            }
            is ApiResult.Failure.UnknownFailure -> {
                val message = "Unknown error: ${result.error.message}"
                Timber.e(result.error, message)
                onError(message)
            }
        }
    }

    @CircuitInject(DeviceCatalogScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): DeviceCatalogPresenter
    }
}
