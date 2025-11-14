package ink.trmnl.android.buddy.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.api.TrmnlApiClient
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Configuration activity for the device widget.
 * Shown when user adds the widget to their home screen to select which device to display.
 */
@Inject
class DeviceWidgetConfigActivity(
    private val userPrefsRepository: UserPreferencesRepository,
    private val widgetConfigRepository: WidgetConfigRepository,
    private val apiClient: TrmnlApiClient,
) : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get the widget ID from the intent
        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If this activity was started with an invalid widget ID, finish with an error
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Set the result to CANCELED initially. This will cause the system
        // to remove the widget if the user backs out of the config activity.
        setResult(RESULT_CANCELED)

        setContent {
            TrmnlBuddyAppTheme {
                DeviceSelectionScreen(
                    onDeviceSelected = { device ->
                        configureWidget(device)
                    },
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DeviceSelectionScreen(onDeviceSelected: (Device) -> Unit) {
        var devices by remember { mutableStateOf<List<Device>?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            scope.launch {
                try {
                    val userPrefs = userPrefsRepository.userPreferencesFlow.first()
                    val apiKey = userPrefs.apiToken

                    if (apiKey.isNullOrEmpty()) {
                        errorMessage = "No API key found. Please set up your account first."
                        isLoading = false
                        return@launch
                    }

                    // Fetch devices
                    val apiService = apiClient.create(apiKey)
                    val repository =
                        ink.trmnl.android.buddy.api.TrmnlDeviceRepository(
                            apiService,
                            apiKey,
                        )

                    when (val result = repository.getDevices()) {
                        is ApiResult.Success -> {
                            devices = result.value
                            isLoading = false
                        }

                        is ApiResult.Failure -> {
                            errorMessage =
                                when (result) {
                                    is ApiResult.Failure.NetworkFailure -> "Network error. Please check your connection."
                                    is ApiResult.Failure.HttpFailure -> "HTTP error ${result.code}"
                                    else -> "Failed to load devices"
                                }
                            isLoading = false
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    isLoading = false
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Select Device") },
                )
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator()
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "⚠️",
                                style = MaterialTheme.typography.displayMedium,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    devices.isNullOrEmpty() -> {
                        Text(
                            text = "No devices found",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(devices!!) { device ->
                                DeviceListItem(
                                    device = device,
                                    onClick = { onDeviceSelected(device) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DeviceListItem(
        device: Device,
        onClick: () -> Unit,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            ListItem(
                headlineContent = { Text(device.name) },
                supportingContent = {
                    Column {
                        Text("ID: ${device.friendlyId}")
                        Text("Battery: ${device.percentCharged.toInt()}%")
                        Text("WiFi: ${device.wifiStrength.toInt()}%")
                    }
                },
            )
        }
    }

    private fun configureWidget(device: Device) {
        // Launch coroutine on main scope
        kotlinx.coroutines.MainScope().launch {
            try {
                // Save the widget configuration
                widgetConfigRepository.saveWidgetDevice(appWidgetId, device.id)

                // Get the GlanceId for the widget
                val glanceId =
                    GlanceAppWidgetManager(this@DeviceWidgetConfigActivity)
                        .getGlanceIdBy(appWidgetId)

                // Update the widget state with initial loading state
                updateAppWidgetState(
                    this@DeviceWidgetConfigActivity,
                    glanceId,
                ) {
                    WidgetState(
                        deviceId = device.id,
                        deviceName = device.name,
                        deviceFriendlyId = device.friendlyId,
                        isLoading = true,
                    )
                }

                // Update the widget
                DeviceWidget().update(this@DeviceWidgetConfigActivity, glanceId)

                // Return success
                val resultValue =
                    Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                setResult(RESULT_OK, resultValue)
                finish()
            } catch (e: Exception) {
                // Handle error - could show a toast or dialog
                finish()
            }
        }
    }
}
