package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Reusable list item component for displaying a device model.
 *
 * Shows device label as headline, specs summary as supporting text,
 * and a trailing arrow icon for navigation.
 *
 * @param device The device model to display
 * @param onClick Callback when the item is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun DeviceListItem(
    device: DeviceModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = device.label,
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            supportingContent = {
                Text(
                    text = device.getSpecsSummary(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun DeviceListItemPreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "og_png",
                    label = "TRMNL OG (1-bit)",
                    description = "TRMNL OG (1-bit)",
                    width = 800,
                    height = 480,
                    colors = 2,
                    bitDepth = 1,
                    scaleFactor = 1.0,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-01-01T00:00:00.000Z",
                    kind = "trmnl",
                    paletteIds = listOf("bw"),
                ),
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceListItemKindlePreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "amazon_kindle_2024",
                    label = "Amazon Kindle 2024",
                    description = "Amazon Kindle 2024",
                    width = 1400,
                    height = 840,
                    colors = 256,
                    bitDepth = 8,
                    scaleFactor = 1.75,
                    rotation = 90,
                    mimeType = "image/png",
                    offsetX = 75,
                    offsetY = 25,
                    publishedAt = "2024-01-01T00:00:00.000Z",
                    kind = "kindle",
                    paletteIds = listOf("gray-256"),
                ),
            onClick = {},
        )
    }
}
