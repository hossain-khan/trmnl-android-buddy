package app.example.circuit.overlay

// -------------------------------------------------------------------------------------
//
// THIS IS AN EXAMPLE FILE WITH CIRCUIT OVERLAYS EXAMPLE
// Read more about it at: https://slackhq.github.io/circuit/overlays/
//
// You should delete this file and create your own screens with presenters.
//
//  -------------------------------------------------------------------------------------

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.example.BuildConfig
import app.example.R
import com.slack.circuitx.overlays.BottomSheetOverlay

/**
 * App information overlay that shows as a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionName")
fun AppInfoOverlay(onDismiss: () -> Unit = {}): BottomSheetOverlay<Unit, Unit> =
    BottomSheetOverlay(
        model = Unit,
        onDismiss = {
            onDismiss()
        },
    ) { _, overlayNavigator ->
        AppInfoContent(
            onDismiss = { overlayNavigator.finish(Unit) },
        )
    }

@Composable
private fun AppInfoContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_info_24),
                    contentDescription = "App Info",
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(label = "App Name", value = "Circuit App Template")
            InfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
            InfoRow(label = "Package", value = BuildConfig.APPLICATION_ID)
            InfoRow(label = "Built Type", value = BuildConfig.BUILD_TYPE)
            InfoRow(label = "Framework", value = "Circuit + Compose + Metro")

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
