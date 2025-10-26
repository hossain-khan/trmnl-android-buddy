package ink.trmnl.android.buddy.ui.settings

import android.Manifest
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Low battery notification section for setting up low battery alerts.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LowBatteryNotificationSection(
    isEnabled: Boolean,
    thresholdPercent: Int,
    onToggle: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Battery threshold range constants
    val minThreshold = 5
    val maxThreshold = 50

    // Permission state for Android 13+ (API 33+)
    // For older Android versions, this permission is automatically granted
    val notificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
                // Callback when permission result is received
                if (isGranted) {
                    // Permission granted, enable notifications
                    onToggle(true)
                } else {
                    // Permission denied, show explanation dialog
                    showPermissionDialog = true
                }
            }
        } else {
            null // No permission needed for Android 12 and below
        }

    // Handle toggle with permission check
    fun handleToggle(enabled: Boolean) {
        if (enabled) {
            // User wants to enable notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionState = notificationPermissionState
                if (permissionState != null) {
                    if (permissionState.status.isGranted) {
                        // Permission already granted
                        onToggle(true)
                    } else {
                        // Request permission (result handled in callback)
                        permissionState.launchPermissionRequest()
                    }
                }
            } else {
                // No permission needed for older Android versions
                onToggle(true)
            }
        } else {
            // User wants to disable notifications, no permission check needed
            onToggle(false)
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Notification Permission Required") },
            text = {
                Text(
                    "To receive low battery alerts, please grant notification permission in your device settings.\n\nSettings > Apps > TRMNL Buddy > Notifications",
                )
            },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("OK")
                }
            },
        )
    }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.battery_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Low Battery Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Low Battery Notifications",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text =
                                if (isEnabled) {
                                    "Get notified when device battery falls below threshold"
                                } else {
                                    "Enable to receive low battery alerts"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { handleToggle(it) },
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
                if (isEnabled) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                AnimatedVisibility(
                    visible = isEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp, top = 16.dp, start = 32.dp, end = 16.dp),
                    ) {
                        Text(
                            text = "Alert Threshold: $thresholdPercent%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "$minThreshold%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Slider(
                                value = thresholdPercent.toFloat(),
                                onValueChange = { onThresholdChange(it.toInt()) },
                                valueRange = minThreshold.toFloat()..maxThreshold.toFloat(),
                                // Calculate steps to create discrete 1% increments
                                // Formula: steps = range - 1 (e.g., 5-50 requires 44 steps for 45 values)
                                steps = maxThreshold - minThreshold - 1,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )

                            Text(
                                text = "$maxThreshold%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            text = "Check battery levels weekly and notify when below threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun LowBatteryNotificationSectionEnabledPreview() {
    TrmnlBuddyAppTheme {
        LowBatteryNotificationSection(
            isEnabled = true,
            thresholdPercent = 25,
            onToggle = {},
            onThresholdChange = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LowBatteryNotificationSectionDisabledPreview() {
    TrmnlBuddyAppTheme {
        LowBatteryNotificationSection(
            isEnabled = false,
            thresholdPercent = 20,
            onToggle = {},
            onThresholdChange = {},
        )
    }
}
