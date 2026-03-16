package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.utils.getBatteryColor
import ink.trmnl.android.buddy.ui.utils.getBatteryIcon
import ink.trmnl.android.buddy.ui.utils.getWifiColor
import ink.trmnl.android.buddy.ui.utils.getWifiIcon
import ink.trmnl.android.buddy.ui.utils.isLowBatteryAlert
import kotlin.math.abs

/**
 * UI content for DeviceDetailScreen.
 *
 * Renders device status information and composes the battery chart section
 * via an embedded [BatteryChartScreen] using [CircuitContent]. This keeps the
 * battery history concerns isolated in [BatteryChartPresenter] while allowing
 * them to be displayed inline within the device detail layout.
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
                upNextItem = state.upNextItem,
            )

            // Battery Chart Section — managed by BatteryChartPresenter
            CircuitContent(
                screen =
                    BatteryChartScreen(
                        deviceId = state.deviceId,
                        deviceName = state.deviceName,
                        currentBattery = state.currentBattery,
                        currentVoltage = state.currentVoltage,
                    ),
            )
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
                                        contentDescription = "Next refresh time",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Next refresh in",
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
private fun PlaylistItemsCard(
    onViewPlaylist: () -> Unit,
    isLoading: Boolean = false,
    totalItems: Int = 0,
    nowPlayingItem: String = "",
    upNextItem: String = "",
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text =
                        if (totalItems > 0) {
                            "Playlist Items ($totalItems)"
                        } else {
                            "Playlist Items"
                        },
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
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (nowPlayingItem.isNotEmpty()) {
                                Text(
                                    text = "• Now playing: $nowPlayingItem",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (upNextItem.isNotEmpty()) {
                                Text(
                                    text = "• Up next: $upNextItem",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
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
        5 to "5 mins",
        10 to "10 mins",
        15 to "15 mins",
        30 to "30 mins",
        45 to "45 mins",
        60 to "1 hour",
        90 to "90 mins",
        120 to "2 hrs",
        240 to "4 hrs",
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

// ========== Previews ==========

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
            upNextItem = "Weather Display",
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
