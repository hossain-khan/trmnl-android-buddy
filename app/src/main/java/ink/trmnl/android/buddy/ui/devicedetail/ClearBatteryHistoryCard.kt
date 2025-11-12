package ink.trmnl.android.buddy.ui.devicedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.battery.BatteryHistoryAnalyzer
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Card component that prompts users to clear battery history when charging events
 * or stale data is detected.
 *
 * This card is shown when:
 * - Battery history contains a charging event (>50% jump between readings)
 * - Battery history data is older than 6 months
 * - Both conditions are met
 *
 * @param clearReason The reason why history should be cleared
 * @param onClearHistory Callback when user confirms clearing history
 * @param modifier Modifier for the card
 */
@Composable
internal fun ClearBatteryHistoryCard(
    clearReason: BatteryHistoryAnalyzer.ClearHistoryReason,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmationDialog by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_info_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = "Battery History Recommendation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }

            Text(
                text = getMessageForReason(clearReason),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            OutlinedButton(
                onClick = { showConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
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

    if (showConfirmationDialog) {
        ClearHistoryConfirmationDialog(
            clearReason = clearReason,
            onConfirm = {
                showConfirmationDialog = false
                onClearHistory()
            },
            onDismiss = { showConfirmationDialog = false },
        )
    }
}

/**
 * Gets the user-facing message explaining why battery history should be cleared.
 *
 * @param reason The reason for clearing history
 * @return A descriptive message for the user
 */
private fun getMessageForReason(reason: BatteryHistoryAnalyzer.ClearHistoryReason): String =
    when (reason) {
        BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED ->
            "We detected that your device has likely been charged (battery level increased by more than 50%). " +
                "This can affect battery drain predictions. Consider clearing the history to start fresh tracking."

        BatteryHistoryAnalyzer.ClearHistoryReason.STALE_DATA ->
            "Your battery history data is older than 6 months. " +
                "Old data may not accurately reflect current battery health. " +
                "Consider clearing the history to start fresh tracking."

        BatteryHistoryAnalyzer.ClearHistoryReason.BOTH ->
            "We detected charging events and your battery history is older than 6 months. " +
                "This data may not accurately reflect current battery health. " +
                "Consider clearing the history to start fresh tracking."
    }

/**
 * Confirmation dialog shown before clearing battery history.
 *
 * @param clearReason The reason for clearing history
 * @param onConfirm Callback when user confirms the action
 * @param onDismiss Callback when user dismisses the dialog
 */
@Composable
private fun ClearHistoryConfirmationDialog(
    clearReason: BatteryHistoryAnalyzer.ClearHistoryReason,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.outline_info_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text(
                text = "Clear Battery History?",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "This will permanently delete all battery history data for this device.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = getDialogReasonText(clearReason),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Text("Clear History")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Gets the dialog-specific text explaining the reason for clearing.
 *
 * @param reason The reason for clearing history
 * @return A descriptive text for the dialog
 */
private fun getDialogReasonText(reason: BatteryHistoryAnalyzer.ClearHistoryReason): String =
    when (reason) {
        BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED ->
            "Reason: Charging event detected in history."

        BatteryHistoryAnalyzer.ClearHistoryReason.STALE_DATA ->
            "Reason: Data is older than 6 months."

        BatteryHistoryAnalyzer.ClearHistoryReason.BOTH ->
            "Reason: Charging event detected and data is older than 6 months."
    }

// Preview Composables
@Preview(showBackground = true)
@Composable
private fun ClearBatteryHistoryCardChargingPreview() {
    TrmnlBuddyAppTheme {
        ClearBatteryHistoryCard(
            clearReason = BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED,
            onClearHistory = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClearBatteryHistoryCardStaleDataPreview() {
    TrmnlBuddyAppTheme {
        ClearBatteryHistoryCard(
            clearReason = BatteryHistoryAnalyzer.ClearHistoryReason.STALE_DATA,
            onClearHistory = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClearBatteryHistoryCardBothPreview() {
    TrmnlBuddyAppTheme {
        ClearBatteryHistoryCard(
            clearReason = BatteryHistoryAnalyzer.ClearHistoryReason.BOTH,
            onClearHistory = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClearBatteryHistoryCardDarkPreview() {
    TrmnlBuddyAppTheme(darkTheme = true) {
        ClearBatteryHistoryCard(
            clearReason = BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED,
            onClearHistory = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClearHistoryConfirmationDialogPreview() {
    TrmnlBuddyAppTheme {
        ClearHistoryConfirmationDialog(
            clearReason = BatteryHistoryAnalyzer.ClearHistoryReason.CHARGING_DETECTED,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
