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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * List item component for displaying a device model.
 *
 * Shows the device label, description, and technical specifications.
 *
 * @param device The device model to display
 * @param onClick Callback when the item is clicked
 * @param modifier Modifier for the root element
 */
@Composable
fun DeviceListItem(
    device: DeviceModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
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
                    text = device.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            overlineContent = {
                Text(
                    text = device.getSpecsSummary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.chevron_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors =
                ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

// Previews

@PreviewLightDark
@Composable
private fun DeviceListItemPreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "og_png",
                    label = "TRMNL",
                    description = "The original TRMNL device",
                    width = 800,
                    height = 480,
                    colors = 256,
                    bitDepth = 8,
                    scaleFactor = 1.0,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-01-01T00:00:00Z",
                    kind = "trmnl",
                    paletteIds = listOf("gray-256"),
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
                    name = "kindle_2024",
                    label = "Kindle (2024)",
                    description = "Latest Kindle model with enhanced display",
                    width = 758,
                    height = 1024,
                    colors = 16,
                    bitDepth = 4,
                    scaleFactor = 1.0,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-01-01T00:00:00Z",
                    kind = "kindle",
                    paletteIds = listOf("gray-16"),
                ),
            onClick = {},
        )
    }
}
