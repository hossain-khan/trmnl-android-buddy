package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceModel
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Bottom sheet showing device details with copy functionality.
 *
 * Displays comprehensive device specifications including resolution, colors,
 * bit depth, scale factor, rotation, MIME type, offsets, color palettes,
 * and publish date. Content is scrollable for landscape orientation.
 *
 * @param device Device model to display details for
 * @param onDismiss Callback when bottom sheet should be dismissed
 * @param modifier Optional modifier for the component
 */
@Composable
fun DeviceDetailsBottomSheet(
    device: DeviceModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
    ) {
        // Scrollable content area
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
        ) {
            // Header with logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = device.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = device.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Device logo
                val logoResource =
                    when (device.deviceKind) {
                        DeviceKind.TRMNL -> R.drawable.trmnl_logo_brand_orange
                        DeviceKind.KINDLE -> R.drawable.amazon_kindle_logo
                        DeviceKind.SEEED_STUDIO -> R.drawable.seed_studio_color_logo
                        DeviceKind.KOBO -> R.drawable.kobo_logo
                        DeviceKind.BYOD -> null
                    }

                logoResource?.let {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = "${device.deviceKind.name} logo",
                        modifier = Modifier.widthIn(max = 100.dp).align(Alignment.CenterVertically),
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Specifications Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    HorizontalDivider()

                    DetailRow(label = "Model Name", value = device.name)
                    DetailRow(label = "Resolution", value = "${device.width} × ${device.height} px")
                    DetailRow(label = "Colors", value = device.colors.toString())
                    DetailRow(label = "Bit Depth", value = "${device.bitDepth}-bit")
                    DetailRow(label = "Scale Factor", value = device.scaleFactor.toString())
                    DetailRow(label = "Rotation", value = "${device.rotation}°")
                    DetailRow(label = "MIME Type", value = device.mimeType)
                    DetailRow(label = "Offset", value = "(${device.offsetX}, ${device.offsetY})")
                    DetailRow(label = "Device Kind", value = device.kind.uppercase())
                    DetailRow(
                        label = "Color Palettes",
                        value = device.paletteIds.joinToString(", "),
                    )
                    DetailRow(label = "Published", value = device.publishedAt?.take(10) ?: "Unknown")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action buttons
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    val details = buildDeviceDetailsText(device)
                    clipboardManager.setText(AnnotatedString(details))
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Copy Details")
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Close")
            }
        }
    }
}

/**
 * Detail row showing label and value.
 *
 * @param label Property label
 * @param value Property value
 * @param modifier Optional modifier for the component
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Build formatted text of device details for clipboard.
 *
 * @param device Device model to format
 * @return Formatted text with all device specifications
 */
private fun buildDeviceDetailsText(device: DeviceModel): String =
    buildString {
        appendLine(device.label)
        appendLine("=".repeat(device.label.length))
        appendLine()
        appendLine("Model Name: ${device.name}")
        appendLine("Description: ${device.description}")
        appendLine("Resolution: ${device.width} × ${device.height} px")
        appendLine("Colors: ${device.colors}")
        appendLine("Bit Depth: ${device.bitDepth}-bit")
        appendLine("Scale Factor: ${device.scaleFactor}")
        appendLine("Rotation: ${device.rotation}°")
        appendLine("MIME Type: ${device.mimeType}")
        appendLine("Offset: (${device.offsetX}, ${device.offsetY})")
        appendLine("Device Kind: ${device.kind.uppercase()}")
        appendLine("Color Palettes: ${device.paletteIds.joinToString(", ")}")
        appendLine("Published: ${device.publishedAt ?: "Unknown"}")
    }

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun DeviceDetailsBottomSheetPreview() {
    TrmnlBuddyAppTheme {
        DeviceDetailsBottomSheet(
            device = previewDeviceTrmnl,
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceDetailsBottomSheetKindlePreview() {
    TrmnlBuddyAppTheme {
        DeviceDetailsBottomSheet(
            device = previewDeviceKindle,
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceDetailsBottomSheetByodPreview() {
    TrmnlBuddyAppTheme {
        DeviceDetailsBottomSheet(
            device = previewDeviceByod,
            onDismiss = {},
        )
    }
}

/**
 * Preview device models for previews.
 */
private val previewDeviceTrmnl =
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
    )

private val previewDeviceKindle =
    DeviceModel(
        name = "amazon_kindle_2024",
        label = "Amazon Kindle 2024",
        description = "Amazon Kindle 2024 (12th generation) with 300 PPI high-resolution display",
        width = 1400,
        height = 840,
        colors = 256,
        bitDepth = 8,
        scaleFactor = 1.75,
        rotation = 90,
        mimeType = "image/png",
        offsetX = 75,
        offsetY = 25,
        publishedAt = "2024-10-15T00:00:00.000Z",
        kind = "kindle",
        paletteIds = listOf("gray-256"),
    )

private val previewDeviceByod =
    DeviceModel(
        name = "inkplate_10",
        label = "Inkplate 10",
        description = "Inkplate 10 - 9.7 inch e-paper display with ESP32",
        width = 1200,
        height = 820,
        colors = 8,
        bitDepth = 3,
        scaleFactor = 1.0,
        rotation = 0,
        mimeType = "image/png",
        offsetX = 0,
        offsetY = 0,
        publishedAt = "2024-03-20T00:00:00.000Z",
        kind = "byod",
        paletteIds = listOf("gray-8", "bw"),
    )
