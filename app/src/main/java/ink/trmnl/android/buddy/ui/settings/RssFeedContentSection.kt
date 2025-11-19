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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import ink.trmnl.android.buddy.ui.theme.Dimens
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * RSS Feed Content section for TRMNL News Updates (Blog Posts & Announcements).
 * Shows content sync toggle and optional notification settings.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RssFeedContentSection(
    isEnabled: Boolean,
    isNotificationEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permission state for Android 13+ (API 33+)
    // For older Android versions, this permission is automatically granted
    val notificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) { isGranted ->
                // Callback when permission result is received
                if (isGranted) {
                    // Permission granted, enable notifications
                    onNotificationToggle(true)
                } else {
                    // Permission denied, show explanation dialog
                    showPermissionDialog = true
                }
            }
        } else {
            null // No permission needed for Android 12 and below
        }

    // Handle notification toggle with permission check
    fun handleNotificationToggle(enabled: Boolean) {
        if (enabled) {
            // User wants to enable notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionState = notificationPermissionState
                if (permissionState != null) {
                    if (permissionState.status.isGranted) {
                        // Permission already granted
                        onNotificationToggle(true)
                    } else {
                        // Request permission (result handled in callback)
                        permissionState.launchPermissionRequest()
                    }
                }
            } else {
                // No permission needed for older Android versions
                onNotificationToggle(true)
            }
        } else {
            // User wants to disable notifications, no permission check needed
            onNotificationToggle(false)
        }
    }

    // Permission denied dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Notification Permission Required") },
            text = {
                Text(
                    "To receive content notifications, please grant notification permission in your device settings.\n\nSettings > Apps > TRMNL Buddy > Notifications",
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
                painter = painterResource(R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSizeMedium),
            )
            Text(
                text = "TRMNL News Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.paddingSmall),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.elevationSmall),
        ) {
            Column {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Enable Blog Posts & Announcements",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text =
                                if (isEnabled) {
                                    "Show TRMNL blog posts and announcements. Sync automatically in the background."
                                } else {
                                    "Syncing blog posts and announcements content is disabled. No blog posts or announcements will be shown or synced."
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onToggle,
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )

                AnimatedVisibility(
                    visible = isEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface),
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = Dimens.paddingMedium),
                        )
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "Get notified for new content",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            supportingContent = {
                                Text(
                                    text =
                                        if (isNotificationEnabled) {
                                            "Get notified when new blog posts or announcements are published"
                                        } else {
                                            "Enable to receive notifications for new blog posts or announcements content"
                                        },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = isNotificationEnabled,
                                    onCheckedChange = { handleNotificationToggle(it) },
                                )
                            },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            modifier = Modifier.padding(start = Dimens.paddingMedium),
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
private fun RssFeedContentSectionEnabledPreview() {
    TrmnlBuddyAppTheme {
        RssFeedContentSection(
            isEnabled = true,
            isNotificationEnabled = true,
            onToggle = {},
            onNotificationToggle = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RssFeedContentSectionDisabledPreview() {
    TrmnlBuddyAppTheme {
        RssFeedContentSection(
            isEnabled = false,
            isNotificationEnabled = false,
            onToggle = {},
            onNotificationToggle = {},
        )
    }
}
