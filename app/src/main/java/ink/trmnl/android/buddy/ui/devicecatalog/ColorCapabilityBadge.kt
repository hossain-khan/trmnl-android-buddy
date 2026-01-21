package ink.trmnl.android.buddy.ui.devicecatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Badge component that visually indicates a device's color capability.
 *
 * Displays a small colored indicator next to device names in the catalog.
 * Uses Material 3 color scheme for theme consistency and accessibility.
 *
 * @param capability The color capability category to display
 * @param modifier Optional modifier for the component
 */
@Composable
fun ColorCapabilityBadge(
    capability: ColorCapability,
    modifier: Modifier = Modifier,
) {
    val label = capability.getLabel()

    Surface(
        modifier =
            modifier.semantics {
                contentDescription = "$label display"
            },
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color indicator dot
            ColorIndicatorDot(capability)

            // Label text
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Colored dot indicator for the color capability badge.
 *
 * Uses different colors and patterns to visually represent each capability type:
 * - Full Color: Gradient with vibrant colors
 * - Grayscale: Medium gray
 * - Multi-tone: Darker gray
 * - Monochrome: Very dark gray
 *
 * @param capability The color capability category to display
 */
@Composable
private fun ColorIndicatorDot(capability: ColorCapability) {
    when (capability) {
        ColorCapability.FULL_COLOR -> {
            // Rainbow gradient for full color devices
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary,
                                            MaterialTheme.colorScheme.secondary,
                                        ),
                                ),
                            shape = CircleShape,
                        ),
            )
        }

        ColorCapability.GRAYSCALE -> {
            // Medium gray for grayscale devices
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            shape = CircleShape,
                        ),
            )
        }

        ColorCapability.MULTI_TONE -> {
            // Darker gray for multi-tone devices
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            shape = CircleShape,
                        ),
            )
        }

        ColorCapability.MONOCHROME -> {
            // Very dark gray for monochrome devices
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = CircleShape,
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
private fun ColorCapabilityBadgeFullColorPreview() {
    TrmnlBuddyAppTheme {
        Surface {
            ColorCapabilityBadge(
                capability = ColorCapability.FULL_COLOR,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorCapabilityBadgeGrayscalePreview() {
    TrmnlBuddyAppTheme {
        Surface {
            ColorCapabilityBadge(
                capability = ColorCapability.GRAYSCALE,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorCapabilityBadgeMultiTonePreview() {
    TrmnlBuddyAppTheme {
        Surface {
            ColorCapabilityBadge(
                capability = ColorCapability.MULTI_TONE,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorCapabilityBadgeMonochromePreview() {
    TrmnlBuddyAppTheme {
        Surface {
            ColorCapabilityBadge(
                capability = ColorCapability.MONOCHROME,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ColorCapabilityBadgeAllPreview() {
    TrmnlBuddyAppTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColorCapabilityBadge(capability = ColorCapability.FULL_COLOR)
                ColorCapabilityBadge(capability = ColorCapability.GRAYSCALE)
                ColorCapabilityBadge(capability = ColorCapability.MULTI_TONE)
                ColorCapabilityBadge(capability = ColorCapability.MONOCHROME)
            }
        }
    }
}
