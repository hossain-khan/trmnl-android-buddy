package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * UI content for [BatteryChartScreen].
 *
 * Renders all battery-related UI components:
 * - Battery history line chart (via Vico library)
 * - Battery depletion prediction card
 * - Disclaimer for prediction accuracy
 * - Manual battery recording card
 * - Clear battery history card (shown when anomalies detected)
 * - Debug panel for test data generation (debug builds only)
 */
@CircuitInject(BatteryChartScreen::class, AppScope::class)
@Composable
fun BatteryChartContent(
    state: BatteryChartScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Battery History Chart
        BatteryHistoryChart(
            batteryHistory = state.batteryHistory,
            isLoading = state.isLoading,
            isBatteryTrackingEnabled = state.isBatteryTrackingEnabled,
        )

        // Battery Prediction (shown when ≥3 data points available)
        BatteryPredictionCard(batteryHistory = state.batteryHistory)

        // Disclaimer (only shown if battery prediction is available)
        if (BatteryHistoryAnalyzer.predictBatteryDepletion(state.batteryHistory) != null) {
            DisclaimerCard()
        }

        // Manual Battery Recording
        ManualBatteryRecordingCard(
            hasRecordedToday = state.hasRecordedToday,
            onRecordBattery = { state.eventSink(BatteryChartScreen.Event.RecordBatteryManually) },
        )

        // Clear Battery History Card (shown when charging or stale data detected)
        state.clearHistoryReason?.let { reason ->
            ClearBatteryHistoryCard(
                clearReason = reason,
                onClearHistory = { state.eventSink(BatteryChartScreen.Event.ClearBatteryHistory) },
            )
        }

        // Debug Panel (only in debug builds)
        if (BuildConfig.DEBUG) {
            DebugBatteryDataPanel(
                currentBattery = state.currentBattery,
                onPopulateData = { minBattery ->
                    state.eventSink(BatteryChartScreen.Event.PopulateBatteryHistory(minBattery))
                },
                onClearData = {
                    state.eventSink(BatteryChartScreen.Event.ClearBatteryHistory)
                },
            )
        }
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

// ========== Previews ==========

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
                        TimeUnit.DAYS.toMillis(21),
            ),
            BatteryHistoryEntity(
                deviceId = "ABC-123",
                percentCharged = 78.0,
                batteryVoltage = 3.70,
                timestamp =
                    currentTime -
                        TimeUnit.DAYS.toMillis(14),
            ),
            BatteryHistoryEntity(
                deviceId = "ABC-123",
                percentCharged = 71.0,
                batteryVoltage = 3.65,
                timestamp =
                    currentTime -
                        TimeUnit.DAYS.toMillis(7),
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
