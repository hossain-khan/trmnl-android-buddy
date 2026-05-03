package ink.trmnl.android.buddy.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.slack.eithernet.ApiResult
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.TrmnlBuddyApp
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Configuration activity displayed when the user adds a new TRMNL Device Widget
 * to their home screen.
 *
 * Shows the list of devices in the user's account and lets them pick one.
 * After selection the activity:
 *  1. Stores the chosen device info in the widget's Glance state
 *  2. Schedules the first [TrmnlWidgetRefreshWorker] run
 *  3. Returns [Activity.RESULT_OK] to the system so the widget is added
 *
 * If the activity is cancelled (back press / no token) RESULT_CANCELED is
 * returned and the widget is NOT added.
 */
@OptIn(ExperimentalMaterial3Api::class)
class WidgetConfigurationActivity : ComponentActivity() {
    private val appGraph by lazy {
        checkNotNull(application as? TrmnlBuddyApp) {
            "Application must be TrmnlBuddyApp to use WidgetConfigurationActivity"
        }.appGraph()
    }
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result = CANCELED so pressing Back removes the widget
        setResult(Activity.RESULT_CANCELED)

        appWidgetId =
            intent?.extras?.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TrmnlBuddyAppTheme {
                ConfigurationScreen(
                    onDeviceSelected = { device -> onDeviceSelected(device) },
                    onCancelled = { finish() },
                )
            }
        }
    }

    @Composable
    private fun ConfigurationScreen(
        onDeviceSelected: (Device) -> Unit,
        onCancelled: () -> Unit,
    ) {
        var devices by remember { mutableStateOf<List<Device>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                val prefs = appGraph.userPreferencesRepository.userPreferencesFlow.first()
                val apiToken = prefs.apiToken
                if (apiToken.isNullOrBlank()) {
                    errorMessage =
                        "Sign in required. Open the TRMNL Buddy app to authenticate before adding a widget."
                    isLoading = false
                    return@LaunchedEffect
                }
                when (val result = appGraph.trmnlApiService.getDevices("Bearer $apiToken")) {
                    is ApiResult.Success -> {
                        devices = result.value.data
                        isLoading = false
                    }

                    is ApiResult.Failure.HttpFailure -> {
                        errorMessage =
                            if (result.code == 401) {
                                "Authentication failed (HTTP 401). Re-open the app and check your API token."
                            } else {
                                "Server error (HTTP ${result.code}). Please try again later."
                            }
                        isLoading = false
                    }

                    is ApiResult.Failure.NetworkFailure -> {
                        errorMessage = "No network connection. Check your internet and try again."
                        isLoading = false
                    }

                    else -> {
                        errorMessage = "Failed to load devices. Please try again."
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "[WidgetConfig] Error loading devices")
                errorMessage = "Error loading devices: ${e.message}"
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.widget_configure_title)) })
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
                    isLoading -> CircularProgressIndicator()
                    errorMessage != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.deviceinfo_thin_outline),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            OutlinedButton(onClick = onCancelled) {
                                Text("Cancel")
                            }
                        }
                    }

                    devices.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.devices_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = "No devices found in your account.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = onCancelled) {
                                Text("Cancel")
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(devices) { device ->
                                Card(
                                    onClick = { onDeviceSelected(device) },
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        ),
                                ) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                text = device.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                text = "ID: ${device.friendlyId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.devices_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        },
                                        trailingContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        R.drawable.chevron_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                                                    ),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        },
                                        colors =
                                            ListItemDefaults.colors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onDeviceSelected(device: Device) {
        lifecycleScope.launch {
            try {
                val manager = GlanceAppWidgetManager(applicationContext)
                val glanceIds = manager.getGlanceIds(TrmnlDeviceWidget::class.java)
                val glanceId =
                    glanceIds.firstOrNull { manager.getAppWidgetId(it) == appWidgetId }
                        ?: run {
                            Timber.e("[WidgetConfig] GlanceId not found for widget $appWidgetId")
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                            return@launch
                        }

                updateAppWidgetState(applicationContext, glanceId) { mutablePrefs ->
                    mutablePrefs[TrmnlDeviceWidget.APP_WIDGET_ID_KEY] = appWidgetId
                    mutablePrefs[TrmnlDeviceWidget.DEVICE_FRIENDLY_ID_KEY] = device.friendlyId
                    mutablePrefs[TrmnlDeviceWidget.DEVICE_NAME_KEY] = device.name
                }

                // Trigger initial render (shows loading state)
                TrmnlDeviceWidget().update(applicationContext, glanceId)

                // Schedule immediate first refresh
                TrmnlWidgetRefreshWorker.enqueue(
                    context = applicationContext,
                    appWidgetId = appWidgetId,
                    initialDelayMinutes = 0,
                )

                val resultValue =
                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                setResult(Activity.RESULT_OK, resultValue)
                finish()
            } catch (e: Exception) {
                Timber.e(e, "[WidgetConfig] Error configuring widget")
            }
        }
    }
}
