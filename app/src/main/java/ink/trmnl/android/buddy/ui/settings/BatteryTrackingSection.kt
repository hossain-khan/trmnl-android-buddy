package ink.trmnl.android.buddy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Battery tracking section for battery history settings.
 */
@Composable
fun BatteryTrackingSection(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.chart_data_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSizeMedium),
            )
            Text(
                text = "Battery History",
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
                        text = "Track Battery History",
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = {
                    Text(
                        text =
                            if (isEnabled) {
                                "Automatically collect battery data weekly to track device health over time"
                            } else {
                                "Battery history tracking is disabled. No data will be collected."
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
        }
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun BatteryTrackingSectionEnabledPreview() {
    TrmnlBuddyAppTheme {
        BatteryTrackingSection(
            isEnabled = true,
            onToggle = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BatteryTrackingSectionDisabledPreview() {
    TrmnlBuddyAppTheme {
        BatteryTrackingSection(
            isEnabled = false,
            onToggle = {},
        )
    }
}
