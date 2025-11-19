package ink.trmnl.android.buddy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.Dimens
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Security section composable.
 * Uses device's native biometric/credential authentication (no custom PIN).
 */
@Composable
fun SecuritySection(
    isSecurityEnabled: Boolean,
    isAuthenticationAvailable: Boolean,
    onSecurityToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDisableDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.fingerprint_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint =
                    if (!isAuthenticationAvailable) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                modifier = Modifier.size(Dimens.iconSizeMedium),
            )
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.paddingSmall),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.elevationSmall),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Dashboard Authentication",
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = {
                    Text(
                        text =
                            if (!isAuthenticationAvailable) {
                                "No device lock detected. Please set up a screen lock (PIN, pattern, password, or biometric) in your device settings."
                            } else if (isSecurityEnabled) {
                                "Require device authentication to access the dashboard with TRMNL images (fingerprint, face, PIN, pattern, or password)"
                            } else {
                                "No authentication required to see TRMNL images in the dashboard. Enable to secure your dashboard with device authentication."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isSecurityEnabled,
                        enabled = isAuthenticationAvailable,
                        onCheckedChange = { enabled ->
                            if (!enabled && isSecurityEnabled) {
                                showDisableDialog = true
                            } else {
                                onSecurityToggle(enabled)
                            }
                        },
                    )
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        }

        // Confirmation dialog when disabling security
        if (showDisableDialog) {
            AlertDialog(
                onDismissRequest = { showDisableDialog = false },
                title = { Text("Disable Security?") },
                text = { Text("This will disable authentication requirement for accessing your dashboard.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDisableDialog = false
                            onSecurityToggle(false)
                        },
                    ) {
                        Text("Disable")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisableDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun SecuritySectionEnabledPreview() {
    TrmnlBuddyAppTheme {
        SecuritySection(
            isSecurityEnabled = true,
            isAuthenticationAvailable = true,
            onSecurityToggle = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SecuritySectionDisabledPreview() {
    TrmnlBuddyAppTheme {
        SecuritySection(
            isSecurityEnabled = false,
            isAuthenticationAvailable = true,
            onSecurityToggle = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SecuritySectionUnavailablePreview() {
    TrmnlBuddyAppTheme {
        SecuritySection(
            isSecurityEnabled = false,
            isAuthenticationAvailable = false,
            onSecurityToggle = {},
        )
    }
}
