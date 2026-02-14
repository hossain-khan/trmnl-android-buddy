package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
 * and a brand logo as trailing content (if available).
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
    // Determine which logo to show based on device kind - memoized
    val logoResource =
        remember(device.deviceKind) {
            when (device.deviceKind) {
                DeviceKind.TRMNL -> R.drawable.trmnl_logo_brand_orange
                DeviceKind.KINDLE -> R.drawable.amazon_kindle_logo
                DeviceKind.SEEED_STUDIO -> R.drawable.seed_studio_color_logo
                DeviceKind.KOBO -> R.drawable.kobo_logo
                DeviceKind.BOOX -> R.drawable.boox_logo_vector
                DeviceKind.BYOD -> null
            }
        }

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
                Column {
                    Text(
                        text = device.getSpecsSummary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ColorCapabilityBadge(capability = device.colorCapability)
                }
            },
            trailingContent =
                if (logoResource != null) {
                    {
                        Image(
                            painter = painterResource(logoResource),
                            contentDescription = "Brand logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.widthIn(max = 90.dp),
                        )
                    }
                } else {
                    null
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

@PreviewLightDark
@Composable
private fun DeviceListItemSeeedStudioPreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "seeed_studio_7in5",
                    label = "Seeed Studio 7.5\" Display",
                    description = "Seeed Studio 7.5\" e-Paper Display",
                    width = 800,
                    height = 480,
                    colors = 2,
                    bitDepth = 1,
                    scaleFactor = 1.0,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-07-21T00:00:00.000Z",
                    kind = "byod",
                    paletteIds = listOf("bw"),
                ),
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceListItemKoboPreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "kobo_clara_hd",
                    label = "Kobo Clara HD",
                    description = "Kobo Clara HD e-reader",
                    width = 1448,
                    height = 1072,
                    colors = 16,
                    bitDepth = 4,
                    scaleFactor = 1.0,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-01-01T00:00:00.000Z",
                    kind = "byod",
                    paletteIds = listOf("gray-16"),
                ),
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceListItemBooxPreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "boox_palma",
                    label = "Palma",
                    description = "Palma",
                    width = 1648,
                    height = 824,
                    colors = 256,
                    bitDepth = 8,
                    scaleFactor = 2.06,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-01-01T00:00:00.000Z",
                    kind = "byod",
                    paletteIds = listOf("gray-256"),
                ),
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceListItemByodPreview() {
    TrmnlBuddyAppTheme {
        DeviceListItem(
            device =
                DeviceModel(
                    name = "inkplate_10",
                    label = "Inkplate 10",
                    description = "Inkplate 10 - Generic BYOD device",
                    width = 1200,
                    height = 820,
                    colors = 8,
                    bitDepth = 3,
                    scaleFactor = 1.0,
                    rotation = 0,
                    mimeType = "image/png",
                    offsetX = 0,
                    offsetY = 0,
                    publishedAt = "2024-01-01T00:00:00.000Z",
                    kind = "byod",
                    paletteIds = listOf("gray-8"),
                ),
            onClick = {},
        )
    }
}
