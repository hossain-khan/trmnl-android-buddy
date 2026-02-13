package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.core.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.data.database.BatteryHistoryEntity
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.utils.getBatteryColor
import ink.trmnl.android.buddy.ui.utils.getBatteryIcon
import ink.trmnl.android.buddy.ui.utils.getWifiColor
import ink.trmnl.android.buddy.ui.utils.getWifiIcon
import ink.trmnl.android.buddy.ui.utils.isLowBatteryAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
                title = { TrmnlTitle(state.deviceName) },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeviceDetailScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { state.eventSink(DeviceDetailScreen.Event.SettingsClicked) },
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (state.hasDeviceToken) {
                                        R.drawable.settings_check_24dp_e8eaed_fill1_wght400_grad0_opsz24
                                    } else {
                                        R.drawable.tv_options_input_settings_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    },
                                ),
                            contentDescription =
                                if (state.hasDeviceToken) {
                                    "Device settings - configured"
                                } else {
                                    "Device settings - not configured"
                                },
                            modifier = Modifier.size(18.dp),
                            tint =
                                if (state.hasDeviceToken) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Settings")
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
            // Low Battery Banner
            if (
                isLowBatteryAlert(
                    percentCharged = state.currentBattery,
                    thresholdPercent = state.lowBatteryThresholdPercent,
                    isNotificationEnabled = state.isLowBatteryNotificationEnabled,
                )
            ) {
                LowBatteryBanner(
                    currentBattery = state.currentBattery,
                    thresholdPercent = state.lowBatteryThresholdPercent,
                )
            }

            // Current Status Card
            CurrentStatusCard(
                currentBattery = state.currentBattery,
                currentVoltage = state.currentVoltage,
                wifiStrength = state.wifiStrength,
                rssi = state.rssi,
                refreshRate = state.refreshRate,
            )

            // View Playlist Items Card (shown when device numeric ID is available)
            PlaylistItemsCard(
                onViewPlaylist = { state.eventSink(DeviceDetailScreen.Event.ViewPlaylistItems) },
                isLoading = state.isPlaylistItemsLoading,
                totalItems = state.playlistItemsCount,
                nowPlayingItem = state.nowPlayingItem,
            )

            // Battery History Chart
            BatteryHistoryChart(
                batteryHistory = state.batteryHistory,
                isLoading = state.isLoading,
                isBatteryTrackingEnabled = state.isBatteryTrackingEnabled,
            )

            // Battery Prediction (shown when ≥3 data points available)
            BatteryPredictionCard(
                batteryHistory = state.batteryHistory,
            )

            // Disclaimer
            DisclaimerCard()

            // Manual Battery Recording
            ManualBatteryRecordingCard(
                hasRecordedToday = state.hasRecordedToday,
                onRecordBattery = { state.eventSink(DeviceDetailScreen.Event.RecordBatteryManually) },
            )

            // Clear Battery History Card (shown when charging or stale data detected)
            state.clearHistoryReason?.let { reason ->
                ClearBatteryHistoryCard(
                    clearReason = reason,
                    onClearHistory = { state.eventSink(DeviceDetailScreen.Event.ClearBatteryHistory) },
                )
            }

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
private fun LowBatteryBanner(
    currentBattery: Double,
    thresholdPercent: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.battery_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "Low battery alert",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Low Battery Alert",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text =
                        "Battery level (${currentBattery.toInt()}%) is below your threshold of $thresholdPercent%. " +
                            "Consider charging the device soon to ensure continuous operation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
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
    refreshRate: Int?,
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

                    // Refresh Rate (if available)
                    refreshRate?.let { rate ->
                        val refreshProgress = getRefreshRateProgress(rate)
                        val refreshLabel = getRefreshRateLabel(rate)

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
                                        painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                        contentDescription = "Refresh Rate",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Refresh Rate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = refreshLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { refreshProgress / 100f },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
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

    // Pre-sort battery history once for use in chart and formatter
    val sortedHistory = remember(batteryHistory) { batteryHistory.sortedBy { it.timestamp } }

    // Calculate chart width based on number of data points (50dp per data point minimum)
    val chartWidth =
        remember(batteryHistory.size) {
            maxOf(300, batteryHistory.size * 50)
        }

    LaunchedEffect(sortedHistory) {
        withContext(Dispatchers.Default) {
            if (sortedHistory.isNotEmpty()) {
                val yValues = sortedHistory.map { it.percentCharged.toFloat() }

                modelProducer.runTransaction {
                    lineSeries {
                        series(yValues)
                    }
                }
            }
        }
    }

    if (sortedHistory.isNotEmpty()) {
        val primaryColor = MaterialTheme.colorScheme.primary

        // Create a custom line layer with points
        val lineLayer =
            rememberLineCartesianLayer(
                LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill =
                            LineCartesianLayer.LineFill.single(
                                com.patrykandpatrick.vico.core.common
                                    .Fill(primaryColor.hashCode()),
                            ),
                        pointProvider =
                            LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.Point(
                                    component =
                                        rememberShapeComponent(
                                            fill = Fill(primaryColor.hashCode()),
                                            shape = CorneredShape.Pill,
                                        ),
                                    sizeDp = 8f,
                                ),
                            ),
                    ),
                ),
            )

        // Memoize the date formatter to avoid recreating SimpleDateFormat
        val dateFormatter = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    lineLayer,
                    startAxis = VerticalAxis.rememberStart(title = "Battery %"),
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            title = "Time",
                            valueFormatter = { _, value, _ ->
                                // Convert index to date using pre-sorted history
                                val index = value.toInt()
                                if (index >= 0 && index < sortedHistory.size) {
                                    val timestamp = sortedHistory[index].timestamp
                                    dateFormatter.format(Date(timestamp))
                                } else {
                                    ""
                                }
                            },
                        ),
                ),
            modelProducer = modelProducer,
            modifier = modifier.width(chartWidth.dp).height(200.dp),
            scrollState =
                rememberVicoScrollState(
                    scrollEnabled = true,
                    initialScroll = Scroll.Absolute.End,
                    autoScrollCondition = AutoScrollCondition.OnModelGrowth,
                ),
            zoomState = rememberVicoZoomState(zoomEnabled = false),
        )
    }
}

@Composable
private fun BatteryPredictionCard(
    batteryHistory: List<BatteryHistoryEntity>,
    modifier: Modifier = Modifier,
) {
    // Calculate prediction
    val prediction =
        remember(batteryHistory) {
            BatteryHistoryAnalyzer.predictBatteryDepletion(batteryHistory)
        }

    // Only show if prediction is available
    if (prediction != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_battery_android_3_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Predicted Battery Depletion",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = prediction.formatTimeRemaining(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text =
                            "Based on ${prediction.dataPointsUsed} data points " +
                                "(${String.format(java.util.Locale.getDefault(), "%.2f", prediction.drainageRatePercentPerDay)}% per day)",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                alpha = 0.8f,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemsCard(
    onViewPlaylist: () -> Unit,
    isLoading: Boolean = false,
    totalItems: Int = 0,
    nowPlayingItem: String = "",
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = "Playlist Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            supportingContent = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "View playlist items and manage visibility",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Show additional info when playlist is loaded
                    if (!isLoading && totalItems > 0) {
                        Text(
                            text =
                                buildString {
                                    append("Total items: $totalItems")
                                    if (nowPlayingItem.isNotEmpty()) {
                                        append(" • Now playing: $nowPlayingItem")
                                    }
                                },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.list_alt_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = "Playlist",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    OutlinedButton(
                        onClick = onViewPlaylist,
                    ) {
                        Text("View")
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

@Preview(
    name = "Playlist Items Card - Ready",
    showBackground = true,
)
@Composable
private fun PlaylistItemsCardReadyPreview() {
    TrmnlBuddyAppTheme {
        PlaylistItemsCard(
            onViewPlaylist = {},
            isLoading = false,
            totalItems = 8,
            nowPlayingItem = "Traffic Dashboard",
        )
    }
}

@Preview(
    name = "Playlist Items Card - Loading",
    showBackground = true,
)
@Composable
private fun PlaylistItemsCardLoadingPreview() {
    TrmnlBuddyAppTheme {
        PlaylistItemsCard(
            onViewPlaylist = {},
            isLoading = true,
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
private fun ManualBatteryRecordingCard(
    hasRecordedToday: Boolean,
    onRecordBattery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Manual Battery Recording",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text =
                    "Record the current battery level to track battery health over time. " +
                        "Battery recordings are periodically taken weekly if the user preference is turned on.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = onRecordBattery,
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasRecordedToday,
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_battery_android_6_24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasRecordedToday) "Battery Recorded Today" else "Record Battery Level",
                )
            }

            if (hasRecordedToday) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check_24dp_e8eaed_fill1_wght400_grad0_opsz24),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Battery level already logged for today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * TRMNL refresh rate options (in minutes) with their display labels.
 *
 * To add new options in the future, simply add entries to this list.
 * The progress bar will automatically recalculate based on the total number of options.
 *
 * Format: Pair(minutes, label)
 */
private val TRMNL_REFRESH_RATE_OPTIONS =
    listOf(
        5 to "Every 5 mins",
        10 to "Every 10 mins",
        15 to "Every 15 mins",
        30 to "Every 30 mins",
        45 to "Every 45 mins",
        60 to "Hourly",
        90 to "Every 90 mins",
        120 to "Every 2 hrs",
        240 to "Every 4 hrs",
        360 to "4x/day",
        480 to "3x/day",
        720 to "2x/day",
        1440 to "1x/day",
    )

/**
 * Map refresh rate in seconds to the closest predefined TRMNL option and calculate progress percentage.
 *
 * Progress is calculated as: position / (options.size - 1) * 100
 * This ensures equal visual weight for each option regardless of how many options exist.
 *
 * Example with 11 options: position 0 = 0%, position 5 = 50%, position 10 = 100%
 * If a 12th option is added: position 0 = 0%, position 5.5 = 50%, position 11 = 100%
 */
private fun getRefreshRateProgress(refreshRateSeconds: Int): Float {
    val refreshRateMinutes = refreshRateSeconds / 60

    // Find the closest predefined option
    val closestIndex =
        TRMNL_REFRESH_RATE_OPTIONS
            .mapIndexed { index, (minutes, _) ->
                index to abs(minutes - refreshRateMinutes)
            }.minByOrNull { it.second }
            ?.first ?: 0

    // Calculate progress with equal weight for each option
    val maxIndex = TRMNL_REFRESH_RATE_OPTIONS.size - 1
    return if (maxIndex > 0) {
        (closestIndex.toFloat() / maxIndex) * 100f
    } else {
        0f
    }
}

/**
 * Get user-friendly label for refresh rate matching TRMNL's UI labels.
 */
private fun getRefreshRateLabel(refreshRateSeconds: Int): String {
    val refreshRateMinutes = refreshRateSeconds / 60

    // Find the closest predefined option
    val closestOption =
        TRMNL_REFRESH_RATE_OPTIONS
            .minByOrNull { (minutes, _) ->
                abs(minutes - refreshRateMinutes)
            }

    return closestOption?.second ?: "Unknown"
}

// Preview Composables
@PreviewLightDark
@Preview(
    name = "Low Battery Banner",
    showBackground = true,
)
@Composable
private fun LowBatteryBannerPreview() {
    TrmnlBuddyAppTheme {
        LowBatteryBanner(
            currentBattery = 15.0,
            thresholdPercent = 20,
        )
    }
}

@Preview(
    name = "Current Status Card - Full Battery",
    showBackground = true,
)
@Composable
private fun CurrentStatusCardFullPreview() {
    TrmnlBuddyAppTheme {
        CurrentStatusCard(
            currentBattery = 98.0,
            currentVoltage = 3.7,
            wifiStrength = 85.0,
            rssi = -45,
            refreshRate = 300, // 5 minutes
        )
    }
}

@Preview(
    name = "Current Status Card - Low Battery",
    showBackground = true,
)
@Composable
private fun CurrentStatusCardLowPreview() {
    TrmnlBuddyAppTheme {
        CurrentStatusCard(
            currentBattery = 15.0,
            currentVoltage = 3.2,
            wifiStrength = 50.0,
            rssi = -70,
            refreshRate = 600, // 10 minutes
        )
    }
}

@Preview(
    name = "Current Status Card - No Voltage/RSSI",
    showBackground = true,
)
@Composable
private fun CurrentStatusCardNoDataPreview() {
    TrmnlBuddyAppTheme {
        CurrentStatusCard(
            currentBattery = 67.0,
            currentVoltage = null,
            wifiStrength = 75.0,
            rssi = null,
            refreshRate = null, // No refresh rate available
        )
    }
}

@Preview(
    name = "Current Status Card - With Refresh Rate",
    showBackground = true,
)
@Composable
private fun CurrentStatusCardWithRefreshRatePreview() {
    TrmnlBuddyAppTheme {
        CurrentStatusCard(
            currentBattery = 45.0,
            currentVoltage = 3.55,
            wifiStrength = 40.0,
            rssi = -69,
            refreshRate = 900, // 15 minutes
        )
    }
}

@Preview(
    name = "Battery History Chart - No Data",
    showBackground = true,
)
@Composable
private fun BatteryHistoryChartEmptyPreview() {
    TrmnlBuddyAppTheme {
        BatteryHistoryChart(
            batteryHistory = emptyList(),
            isLoading = false,
            isBatteryTrackingEnabled = true,
        )
    }
}

@Preview(
    name = "Battery History Chart - Loading",
    showBackground = true,
)
@Composable
private fun BatteryHistoryChartLoadingPreview() {
    TrmnlBuddyAppTheme {
        BatteryHistoryChart(
            batteryHistory = emptyList(),
            isLoading = true,
            isBatteryTrackingEnabled = true,
        )
    }
}

@Preview(
    name = "Battery History Chart - Tracking Disabled",
    showBackground = true,
)
@Composable
private fun BatteryHistoryChartDisabledPreview() {
    TrmnlBuddyAppTheme {
        BatteryHistoryChart(
            batteryHistory = emptyList(),
            isLoading = false,
            isBatteryTrackingEnabled = false,
        )
    }
}

@Preview(
    name = "Disclaimer Card",
    showBackground = true,
)
@Composable
private fun DisclaimerCardPreview() {
    TrmnlBuddyAppTheme {
        DisclaimerCard()
    }
}

@PreviewLightDark
@Preview(
    name = "Battery Prediction Card",
    showBackground = true,
)
@Composable
private fun BatteryPredictionCardPreview() {
    // Create sample battery history data with realistic drainage
    val currentTime = System.currentTimeMillis()
    val sampleData =
        listOf(
            BatteryHistoryEntity(
                deviceId = "ABC-123",
                percentCharged = 85.0,
                batteryVoltage = 3.75,
                timestamp =
                    currentTime -
                        java.util.concurrent.TimeUnit.DAYS
                            .toMillis(21),
            ),
            BatteryHistoryEntity(
                deviceId = "ABC-123",
                percentCharged = 78.0,
                batteryVoltage = 3.70,
                timestamp =
                    currentTime -
                        java.util.concurrent.TimeUnit.DAYS
                            .toMillis(14),
            ),
            BatteryHistoryEntity(
                deviceId = "ABC-123",
                percentCharged = 71.0,
                batteryVoltage = 3.65,
                timestamp =
                    currentTime -
                        java.util.concurrent.TimeUnit.DAYS
                            .toMillis(7),
            ),
            BatteryHistoryEntity(
                deviceId = "ABC-123",
                percentCharged = 64.0,
                batteryVoltage = 3.60,
                timestamp = currentTime,
            ),
        )

    TrmnlBuddyAppTheme {
        BatteryPredictionCard(batteryHistory = sampleData)
    }
}

@PreviewLightDark
@Preview(
    name = "Manual Battery Recording Card - Not Recorded",
    showBackground = true,
)
@Composable
private fun ManualBatteryRecordingCardPreview() {
    TrmnlBuddyAppTheme {
        ManualBatteryRecordingCard(
            hasRecordedToday = false,
            onRecordBattery = {},
        )
    }
}

@PreviewLightDark
@Preview(
    name = "Manual Battery Recording Card - Already Recorded",
    showBackground = true,
)
@Composable
private fun ManualBatteryRecordingCardRecordedPreview() {
    TrmnlBuddyAppTheme {
        ManualBatteryRecordingCard(
            hasRecordedToday = true,
            onRecordBattery = {},
        )
    }
}
