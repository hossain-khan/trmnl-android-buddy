package ink.trmnl.android.buddy.ui.devices

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import com.slack.circuit.sharedelements.SharedElementTransitionScope.AnimatedScope.Navigation
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.Device
import ink.trmnl.android.buddy.ui.sharedelements.DevicePreviewImageKey
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.utils.getBatteryColor
import ink.trmnl.android.buddy.ui.utils.getBatteryIcon
import ink.trmnl.android.buddy.ui.utils.getWifiColor
import ink.trmnl.android.buddy.ui.utils.getWifiIcon
import ink.trmnl.android.buddy.ui.utils.isLowBatteryAlert
import ink.trmnl.android.buddy.util.PrivacyUtils
import ink.trmnl.android.buddy.util.formatRefreshRate

/**
 * Device card composable.
 * Shows device information with battery and WiFi indicators.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DeviceCard(
    device: Device,
    hasToken: Boolean,
    previewInfo: DevicePreviewInfo?,
    isPrivacyEnabled: Boolean,
    isLowBatteryNotificationEnabled: Boolean,
    lowBatteryThresholdPercent: Int,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPreviewClick: () -> Unit,
    eventSink: (TrmnlDevicesScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track if this is the first composition to trigger animation
    var isInitialized by remember { mutableStateOf(false) }

    // Animate progress indicators from 0 to actual value on first load
    val batteryProgress by animateFloatAsState(
        targetValue = if (isInitialized) (device.percentCharged / 100).toFloat() else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 100),
        label = "battery_progress",
    )
    val wifiProgress by animateFloatAsState(
        targetValue = if (isInitialized) (device.wifiStrength / 100).toFloat() else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 200),
        label = "wifi_progress",
    )

    // Trigger animation on first composition
    LaunchedEffect(Unit) {
        isInitialized = true
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    // Show battery alert icon if enabled and battery is below threshold
                    if (
                        isLowBatteryAlert(
                            percentCharged = device.percentCharged,
                            thresholdPercent = lowBatteryThresholdPercent,
                            isNotificationEnabled = isLowBatteryNotificationEnabled,
                        )
                    ) {
                        IconButton(
                            onClick = {
                                eventSink(
                                    TrmnlDevicesScreen.Event.BatteryAlertClicked(
                                        device = device,
                                        thresholdPercent = lowBatteryThresholdPercent,
                                    ),
                                )
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.battery_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Low battery alert",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            painter =
                                painterResource(
                                    if (hasToken) {
                                        R.drawable.settings_check_24dp_e8eaed_fill1_wght400_grad0_opsz24
                                    } else {
                                        R.drawable.tv_options_input_settings_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    },
                                ),
                            contentDescription = if (hasToken) "Display configured" else "Configure device token",
                            tint =
                                if (hasToken) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            },
            supportingContent = {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Device ID (obfuscated for privacy)
                    DeviceInfoRow(
                        icon = R.drawable.outline_tag_24,
                        label = "ID: ",
                        value =
                            if (isPrivacyEnabled) {
                                PrivacyUtils.obfuscateDeviceId(device.friendlyId)
                            } else {
                                device.friendlyId
                            },
                        contentDescription = "Device ID",
                    )

                    // MAC Address (obfuscated for privacy)
                    DeviceInfoRow(
                        icon = R.drawable.outline_barcode_24,
                        label = "MAC: ",
                        value =
                            if (isPrivacyEnabled) {
                                PrivacyUtils.obfuscateMacAddress(device.macAddress)
                            } else {
                                device.macAddress
                            },
                        contentDescription = "MAC Address",
                    )

                    // Battery Level
                    BatteryIndicator(
                        percentCharged = device.percentCharged,
                        batteryVoltage = device.batteryVoltage,
                        batteryProgress = batteryProgress,
                    )

                    // WiFi Strength
                    WifiIndicator(
                        wifiStrength = device.wifiStrength,
                        rssi = device.rssi,
                        wifiProgress = wifiProgress,
                    )
                }
            },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.trmnl_device_frame),
                    contentDescription = "Device",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
        )

        // Display preview image if available
        DevicePreviewImage(
            hasToken = hasToken,
            previewInfo = previewInfo,
            deviceName = device.name,
            deviceId = device.friendlyId,
            onPreviewClick = onPreviewClick,
            eventSink = eventSink,
        )
    }
}

/**
 * Device information row composable.
 * Shows an icon, label, and value in a horizontal row.
 */
@Composable
internal fun DeviceInfoRow(
    icon: Int,
    label: String,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Battery indicator composable.
 * Shows battery percentage, progress bar, and optional voltage.
 */
@Composable
internal fun BatteryIndicator(
    percentCharged: Double,
    batteryVoltage: Double?,
    batteryProgress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
                    painter = painterResource(getBatteryIcon(percentCharged)),
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
                text = "${percentCharged.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = getBatteryColor(percentCharged),
            )
        }
        LinearProgressIndicator(
            progress = { batteryProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            color = getBatteryColor(percentCharged),
        )
        batteryVoltage?.let { voltage ->
            Text(
                text = "%.2fV".format(voltage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * WiFi indicator composable.
 * Shows WiFi signal strength percentage, progress bar, and optional RSSI.
 */
@Composable
internal fun WifiIndicator(
    wifiStrength: Double,
    rssi: Int?,
    wifiProgress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
            progress = { wifiProgress },
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

/**
 * Device preview image composable.
 * Shows the device's current screen display if available.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun DevicePreviewImage(
    hasToken: Boolean,
    previewInfo: DevicePreviewInfo?,
    deviceName: String,
    deviceId: String,
    onPreviewClick: () -> Unit,
    eventSink: (TrmnlDevicesScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasToken && previewInfo != null) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically(),
        ) {
            SharedElementTransitionScope {
                Box(
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .aspectRatio(800f / 480f) // TRMNL device aspect ratio
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    SubcomposeAsyncImage(
                        model = previewInfo.imageUrl,
                        contentDescription = "Device screen preview for $deviceName",
                        modifier =
                            Modifier
                                .sharedElement(
                                    sharedContentState =
                                        rememberSharedContentState(
                                            key = DevicePreviewImageKey(deviceId = deviceId),
                                        ),
                                    animatedVisibilityScope = requireAnimatedScope(Navigation),
                                ).fillMaxSize()
                                .clickable(onClick = onPreviewClick),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        error = {
                            // Silently fail - don't show error for preview images
                        },
                    )

                    // Refresh rate indicator overlay
                    RefreshRateIndicator(
                        refreshRate = previewInfo.refreshRate,
                        onInfoClick = { rate ->
                            eventSink(TrmnlDevicesScreen.Event.RefreshRateInfoClicked(rate))
                        },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

/**
 * Refresh rate indicator composable.
 * Shows a semi-transparent overlay with refresh rate information.
 */
@Composable
internal fun RefreshRateIndicator(
    refreshRate: Int,
    onInfoClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = { onInfoClick(refreshRate) },
        modifier =
            modifier
                .padding(8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            ),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "Refresh rate",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = formatRefreshRate(refreshRate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// ========== Previews ==========

/**
 * Sample device data for previews.
 */
private val sampleDevice1 =
    Device(
        id = 1,
        name = "Living Room Display",
        friendlyId = "ABC-123",
        macAddress = "12:34:56:78:9A:BC",
        batteryVoltage = 3.7,
        rssi = -45,
        percentCharged = 85.0,
        wifiStrength = 90.0,
    )

private val sampleDevice2 =
    Device(
        id = 2,
        name = "Kitchen Dashboard",
        friendlyId = "DEF-456",
        macAddress = "12:34:56:78:9A:BD",
        batteryVoltage = 3.5,
        rssi = -65,
        percentCharged = 45.0,
        wifiStrength = 60.0,
    )

private val sampleDevice3 =
    Device(
        id = 3,
        name = "Office Monitor",
        friendlyId = "GHI-789",
        macAddress = "12:34:56:78:9A:BE",
        batteryVoltage = 3.2,
        rssi = -80,
        percentCharged = 15.0,
        wifiStrength = 25.0,
    )

@PreviewLightDark
@Composable
private fun DeviceCardHighBatteryPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice1,
            hasToken = true,
            previewInfo = null,
            isPrivacyEnabled = false,
            isLowBatteryNotificationEnabled = false,
            lowBatteryThresholdPercent = 20,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCardMediumBatteryPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice2,
            hasToken = true,
            previewInfo = null,
            isPrivacyEnabled = false,
            isLowBatteryNotificationEnabled = false,
            lowBatteryThresholdPercent = 20,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCardLowBatteryPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice3,
            hasToken = false,
            previewInfo = null,
            isPrivacyEnabled = false,
            isLowBatteryNotificationEnabled = true,
            lowBatteryThresholdPercent = 20,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceCardPrivacyEnabledPreview() {
    TrmnlBuddyAppTheme {
        DeviceCard(
            device = sampleDevice1,
            hasToken = true,
            previewInfo = null,
            isPrivacyEnabled = true,
            isLowBatteryNotificationEnabled = false,
            lowBatteryThresholdPercent = 20,
            onClick = {},
            onSettingsClick = {},
            onPreviewClick = {},
            eventSink = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceInfoRowPreview() {
    TrmnlBuddyAppTheme {
        DeviceInfoRow(
            icon = R.drawable.outline_tag_24,
            label = "ID: ",
            value = "ABC-123",
            contentDescription = "Device ID",
        )
    }
}

@PreviewLightDark
@Composable
private fun BatteryIndicatorHighPreview() {
    TrmnlBuddyAppTheme {
        BatteryIndicator(
            percentCharged = 85.0,
            batteryVoltage = 3.7,
            batteryProgress = 0.85f,
        )
    }
}

@PreviewLightDark
@Composable
private fun BatteryIndicatorLowPreview() {
    TrmnlBuddyAppTheme {
        BatteryIndicator(
            percentCharged = 15.0,
            batteryVoltage = 3.2,
            batteryProgress = 0.15f,
        )
    }
}

@PreviewLightDark
@Composable
private fun WifiIndicatorHighPreview() {
    TrmnlBuddyAppTheme {
        WifiIndicator(
            wifiStrength = 90.0,
            rssi = -45,
            wifiProgress = 0.90f,
        )
    }
}

@PreviewLightDark
@Composable
private fun WifiIndicatorLowPreview() {
    TrmnlBuddyAppTheme {
        WifiIndicator(
            wifiStrength = 25.0,
            rssi = -80,
            wifiProgress = 0.25f,
        )
    }
}

@PreviewLightDark
@Composable
private fun RefreshRateIndicatorPreview() {
    TrmnlBuddyAppTheme {
        RefreshRateIndicator(
            refreshRate = 900,
            onInfoClick = {},
        )
    }
}
