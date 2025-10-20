package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import ink.trmnl.android.buddy.data.database.BatteryHistoryRepository
import ink.trmnl.android.buddy.ui.utils.getBatteryColor
import ink.trmnl.android.buddy.ui.utils.getBatteryIcon
import ink.trmnl.android.buddy.ui.utils.getWifiColor
import ink.trmnl.android.buddy.ui.utils.getWifiIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Screen for displaying device details including battery history and health trajectory.
 */
@Parcelize
data class DeviceDetailScreen(
    val deviceId: String,
    val deviceName: String,
    val currentBattery: Double,
    val currentVoltage: Double?,
    val wifiStrength: Double,
    val rssi: Int?,
) : Screen {
    data class State(
        val deviceId: String,
        val deviceName: String,
        val currentBattery: Double,
        val currentVoltage: Double?,
        val wifiStrength: Double,
        val rssi: Int?,
        val batteryHistory: List<BatteryHistoryEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isBatteryTrackingEnabled: Boolean = true,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data class PopulateBatteryHistory(
            val minBatteryLevel: Float,
        ) : Event()

        data object ClearBatteryHistory : Event()
    }
}

/**
 * Presenter for DeviceDetailScreen.
 */
@Inject
class DeviceDetailPresenter
    constructor(
        @Assisted private val screen: DeviceDetailScreen,
        @Assisted private val navigator: Navigator,
        private val batteryHistoryRepository: BatteryHistoryRepository,
        private val userPreferencesRepository: ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository,
    ) : Presenter<DeviceDetailScreen.State> {
        @Composable
        override fun present(): DeviceDetailScreen.State {
            val batteryHistory by batteryHistoryRepository
                .getBatteryHistoryForDevice(screen.deviceId)
                .collectAsState(initial = emptyList())
            var isLoading by rememberRetained { mutableStateOf(true) }
            val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
                initial =
                    ink.trmnl.android.buddy.data.preferences
                        .UserPreferences(),
            )

            // Mark loading complete when we have data or after initial load
            LaunchedEffect(batteryHistory) {
                isLoading = false
            }

            return DeviceDetailScreen.State(
                deviceId = screen.deviceId,
                deviceName = screen.deviceName,
                currentBattery = screen.currentBattery,
                currentVoltage = screen.currentVoltage,
                wifiStrength = screen.wifiStrength,
                rssi = screen.rssi,
                batteryHistory = batteryHistory,
                isLoading = isLoading,
                isBatteryTrackingEnabled = preferences.isBatteryTrackingEnabled,
            ) { event ->
                when (event) {
                    DeviceDetailScreen.Event.BackClicked -> navigator.pop()
                    is DeviceDetailScreen.Event.PopulateBatteryHistory -> {
                        // Generate simulated battery history data
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                            val currentTime = System.currentTimeMillis()
                            val weeksToGenerate = 12 // Generate 12 weeks of history
                            val currentBattery = screen.currentBattery
                            val minBattery = event.minBatteryLevel.toDouble()

                            // Calculate battery drop per week
                            val totalDrop = currentBattery - minBattery
                            val dropPerWeek = totalDrop / weeksToGenerate

                            for (week in 0 until weeksToGenerate) {
                                val weeklyBattery = currentBattery - (dropPerWeek * week)
                                val timestamp = currentTime - TimeUnit.DAYS.toMillis((weeksToGenerate - week) * 7L)

                                batteryHistoryRepository.recordBatteryReading(
                                    deviceId = screen.deviceId,
                                    percentCharged = weeklyBattery,
                                    batteryVoltage = screen.currentVoltage,
                                    timestamp = timestamp,
                                )
                            }
                        }
                    }
                    DeviceDetailScreen.Event.ClearBatteryHistory -> {
                        // Clear all battery history for this device
                        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                            batteryHistoryRepository.deleteHistoryForDevice(screen.deviceId)
                        }
                    }
                }
            }
        }

        @CircuitInject(DeviceDetailScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: DeviceDetailScreen,
                navigator: Navigator,
            ): DeviceDetailPresenter
        }
    }

/**
 * UI content for DeviceDetailScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DeviceDetailScreen::class, AppScope::class)
@Composable
fun DeviceDetailContent(
    state: DeviceDetailScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(state.deviceName) },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeviceDetailScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Current Status Card
            CurrentStatusCard(
                currentBattery = state.currentBattery,
                currentVoltage = state.currentVoltage,
                wifiStrength = state.wifiStrength,
                rssi = state.rssi,
            )

            // Battery History Chart
            BatteryHistoryChart(
                batteryHistory = state.batteryHistory,
                isLoading = state.isLoading,
                isBatteryTrackingEnabled = state.isBatteryTrackingEnabled,
            )

            // Disclaimer
            DisclaimerCard()

            // Debug Panel (only in debug builds)
            if (BuildConfig.DEBUG) {
                DebugBatteryDataPanel(
                    currentBattery = state.currentBattery,
                    onPopulateData = { minBattery ->
                        state.eventSink(DeviceDetailScreen.Event.PopulateBatteryHistory(minBattery))
                    },
                    onClearData = {
                        state.eventSink(DeviceDetailScreen.Event.ClearBatteryHistory)
                    },
                )
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(
    currentBattery: Double,
    currentVoltage: Double?,
    wifiStrength: Double,
    rssi: Int?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = "Current Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            supportingContent = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Battery Level
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(getBatteryIcon(currentBattery)),
                                    contentDescription = "Battery",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Battery",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${currentBattery.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = getBatteryColor(currentBattery),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (currentBattery / 100).toFloat() },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            color = getBatteryColor(currentBattery),
                        )
                        currentVoltage?.let { voltage ->
                            Text(
                                text = "%.2fV".format(voltage),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // WiFi Strength
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    painter = painterResource(getWifiIcon(wifiStrength)),
                                    contentDescription = "WiFi Signal",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "WiFi Signal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "${wifiStrength.toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = getWifiColor(wifiStrength),
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (wifiStrength / 100).toFloat() },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            color = getWifiColor(wifiStrength),
                        )
                        rssi?.let { rssiValue ->
                            Text(
                                text = "$rssiValue dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        )
    }
}

@Composable
private fun BatteryHistoryChart(
    batteryHistory: List<BatteryHistoryEntity>,
    isLoading: Boolean,
    isBatteryTrackingEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var isInitialized by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Battery History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            when {
                !isBatteryTrackingEnabled -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_barcode_24),
                                contentDescription = "Tracking disabled",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Battery history tracking is disabled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Enable it in Settings to start collecting data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                batteryHistory.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.graph_trend_up),
                                contentDescription = "No data",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Not enough battery data available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Battery data is collected weekly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> {
                    AnimatedVisibility(
                        visible = isInitialized,
                        enter = fadeIn() + slideInVertically(),
                    ) {
                        BatteryChart(batteryHistory = batteryHistory)
                    }

                    LaunchedEffect(Unit) {
                        isInitialized = true
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryChart(
    batteryHistory: List<BatteryHistoryEntity>,
    modifier: Modifier = Modifier,
) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(batteryHistory) {
        withContext(Dispatchers.Default) {
            if (batteryHistory.isNotEmpty()) {
                // Sort by timestamp ascending for chart
                val sortedHistory = batteryHistory.sortedBy { it.timestamp }
                val yValues = sortedHistory.map { it.percentCharged.toFloat() }

                modelProducer.runTransaction {
                    lineSeries {
                        series(yValues)
                    }
                }
            }
        }
    }

    if (batteryHistory.isNotEmpty()) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = rememberStartAxis(title = "Battery %"),
                    bottomAxis =
                        rememberBottomAxis(
                            title = "Time",
                            valueFormatter = { value, _, _ ->
                                // Convert index to date
                                val index = value.toInt()
                                if (index >= 0 && index < batteryHistory.size) {
                                    val sortedHistory = batteryHistory.sortedBy { it.timestamp }
                                    val timestamp = sortedHistory[index].timestamp
                                    SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
                                } else {
                                    ""
                                }
                            },
                        ),
                ),
            modelProducer = modelProducer,
            modifier = modifier.fillMaxWidth().height(200.dp),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
        )
    }
}

@Composable
private fun DisclaimerCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_info_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    "Note: Battery trajectory predictions are based on historical drain data. " +
                        "Actual battery life may vary depending on usage patterns, " +
                        "environmental conditions, and device settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DebugBatteryDataPanel(
    currentBattery: Double,
    onPopulateData: (Float) -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var minBatteryLevel by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.tools_outline),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "DEBUG: Simulate Battery History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Text(
                text = "Generate 12 weeks of battery drain data from ${currentBattery.toInt()}% down to ${minBatteryLevel.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Minimum Battery Level: ${minBatteryLevel.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Slider(
                    value = minBatteryLevel,
                    onValueChange = { minBatteryLevel = it },
                    valueRange = 0f..currentBattery.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = { onPopulateData(minBatteryLevel) },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.graph_trend_up),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Populate Battery History Data")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clear history button
            OutlinedButton(
                onClick = { onClearData() },
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        brush = SolidColor(MaterialTheme.colorScheme.error),
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Battery History")
            }
        }
    }
}
