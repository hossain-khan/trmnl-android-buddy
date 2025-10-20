package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Debug panel for simulating battery history data.
 * Only shown in debug builds.
 *
 * @param currentBattery Current battery percentage (0.0 to 100.0)
 * @param onPopulateData Callback when user clicks populate button with minimum battery level
 * @param onClearData Callback when user clicks clear history button
 * @param modifier Modifier for the panel
 */
@Composable
internal fun DebugBatteryDataPanel(
    currentBattery: Double,
    onPopulateData: (Float) -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var minBatteryLevel by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.tools_outline),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "DEBUG: Simulate Battery History",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Text(
                text = "Generate 12 weeks of battery drain data from ${currentBattery.toInt()}% down to ${minBatteryLevel.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Minimum Battery Level: ${minBatteryLevel.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Slider(
                    value = minBatteryLevel,
                    onValueChange = { minBatteryLevel = it },
                    valueRange = 0f..currentBattery.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = { onPopulateData(minBatteryLevel) },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.graph_trend_up),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Populate Battery History Data")
            }

            // Clear history button
            OutlinedButton(
                onClick = { onClearData() },
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        brush = SolidColor(MaterialTheme.colorScheme.error),
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Battery History")
            }
        }
    }
}

@Preview(
    name = "Debug Battery Panel - Full Battery",
    showBackground = true,
)
@Composable
private fun DebugBatteryDataPanelPreview() {
    TrmnlBuddyAppTheme {
        DebugBatteryDataPanel(
            currentBattery = 100.0,
            onPopulateData = {},
            onClearData = {},
        )
    }
}

@Preview(
    name = "Debug Battery Panel - Mid Battery",
    showBackground = true,
)
@Composable
private fun DebugBatteryDataPanelMidBatteryPreview() {
    TrmnlBuddyAppTheme {
        DebugBatteryDataPanel(
            currentBattery = 67.0,
            onPopulateData = {},
            onClearData = {},
        )
    }
}

@Preview(
    name = "Debug Battery Panel - Low Battery",
    showBackground = true,
)
@Composable
private fun DebugBatteryDataPanelLowBatteryPreview() {
    TrmnlBuddyAppTheme {
        DebugBatteryDataPanel(
            currentBattery = 23.0,
            onPopulateData = {},
            onClearData = {},
        )
    }
}

@Preview(
    name = "Debug Battery Panel - Dark Theme",
    showBackground = true,
)
@Composable
private fun DebugBatteryDataPanelDarkPreview() {
    TrmnlBuddyAppTheme(darkTheme = true) {
        DebugBatteryDataPanel(
            currentBattery = 85.0,
            onPopulateData = {},
            onClearData = {},
        )
    }
}
