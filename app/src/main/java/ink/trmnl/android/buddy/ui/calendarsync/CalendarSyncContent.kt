package ink.trmnl.android.buddy.ui.calendarsync

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.calendar.models.SyncCalendar
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UI content for [CalendarSyncScreen].
 *
 * Handles the full calendar sync management UI including:
 * - Runtime READ_CALENDAR permission request via Accompanist Permissions
 * - Calendar list with selection toggles (when permission granted)
 * - Sync status display (last sync time, status, errors)
 * - "Sync Now", "Refresh Calendars", and "Disconnect Sync" actions
 * - Disconnect confirmation dialog
 *
 * **Permission Flow:**
 * 1. `status.isGranted` → Shows full calendar UI and loads calendars
 * 2. `status.shouldShowRationale` → Shows [PermissionRationaleDialog] (user denied once)
 * 3. Default → Shows "Grant Calendar Access" button (first time or permanent deny)
 *
 * @see CalendarSyncPresenter Business logic and state management
 * @see CalendarPermissionHandler Permission state handling composable
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@CircuitInject(CalendarSyncScreen::class, AppScope::class)
@Composable
fun CalendarSyncContent(
    state: CalendarSyncScreen.State,
    modifier: Modifier = Modifier,
) {
    val calendarPermissionState =
        rememberPermissionState(permission = Manifest.permission.READ_CALENDAR)

    // Load calendars when READ_CALENDAR permission becomes granted
    LaunchedEffect(calendarPermissionState.status.isGranted) {
        if (calendarPermissionState.status.isGranted) {
            state.eventSink(CalendarSyncScreen.Event.RefreshCalendarsClicked)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Calendar Sync") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(CalendarSyncScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CalendarPermissionHandler(
                permissionState = calendarPermissionState,
                calendarContent = {
                    CalendarListContent(
                        state = state,
                    )
                },
            )
        }
    }

    // Disconnect confirmation dialog
    if (state.showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { state.eventSink(CalendarSyncScreen.Event.DisconnectDismissed) },
            title = { Text("Disconnect Calendar Sync") },
            text = {
                Text(
                    "This will disable calendar sync and clear all calendar selections. " +
                        "Your calendars will no longer sync to TRMNL. Are you sure?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { state.eventSink(CalendarSyncScreen.Event.DisconnectConfirmed) },
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { state.eventSink(CalendarSyncScreen.Event.DisconnectDismissed) },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Handles the READ_CALENDAR permission state and renders appropriate UI.
 *
 * Three permission states:
 * 1. `status.isGranted` → Shows [calendarContent]
 * 2. `status.shouldShowRationale` → Shows [PermissionRationaleDialog] with rationale
 * 3. Default → Shows "Grant Calendar Access" button for first-time or permanent deny
 *
 * **Important**: `launchPermissionRequest()` is always called from non-composable scope
 * (Button.onClick lambda) as required by Accompanist.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarPermissionHandler(
    permissionState: com.google.accompanist.permissions.PermissionState,
    calendarContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        permissionState.status.isGranted -> {
            calendarContent()
        }

        permissionState.status.shouldShowRationale -> {
            // User denied permission once - show a rationale dialog
            PermissionRationaleDialog(
                onRequestPermission = { permissionState.launchPermissionRequest() },
                modifier = modifier,
            )
        }

        else -> {
            // First-time request or user selected "Don't ask again"
            PermissionRequestContent(
                onRequestPermission = { permissionState.launchPermissionRequest() },
                modifier = modifier,
            )
        }
    }
}

/**
 * Shows a rationale dialog explaining why READ_CALENDAR is needed.
 * Displayed when the user has denied the permission once.
 *
 * Permission request is launched from the Button's onClick (non-composable scope).
 */
@Composable
fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
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
                    painter = painterResource(R.drawable.calendar_month_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Calendar Access Needed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text =
                    "TRMNL Buddy needs access to your calendars to sync events to your TRMNL " +
                        "e-ink display. Your calendar data is only read — never modified.\n\n" +
                        "Please grant Calendar permission to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Grant Calendar Access")
            }
        }
    }
}

/**
 * Shows the initial permission request UI for first-time access or when "Don't ask again" was selected.
 * Permission request is launched from the Button's onClick (non-composable scope).
 */
@Composable
private fun PermissionRequestContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.calendar_month_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = "Sync Calendars to TRMNL",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text =
                    "Display your upcoming events on your TRMNL e-ink device. " +
                        "Grant Calendar access to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.calendar_month_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text("Grant Calendar Access")
            }
        }
    }
}

/**
 * Main calendar list content shown when READ_CALENDAR permission is granted.
 *
 * Displays:
 * - Loading state while calendars are being fetched
 * - Sync status card (last sync time, status, errors)
 * - Empty state when no calendars are found
 * - List of available calendars with selection toggles
 * - "Sync Now", "Refresh Calendars", and "Disconnect Sync" action buttons
 */
@Composable
private fun CalendarListContent(
    state: CalendarSyncScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Sync status card
        SyncStatusCard(state = state)

        // Error message
        if (state.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Calendar list section
        CalendarListSection(state = state)

        // Action buttons
        CalendarActionButtons(state = state)
    }
}

/**
 * Displays current sync status including last sync time and any errors.
 */
@Composable
private fun SyncStatusCard(
    state: CalendarSyncScreen.State,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.calendar_month_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Calendar Sync",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.weight(1f))
                // Sync status indicator
                when (state.syncStatus) {
                    CalendarSyncScreen.SyncStatus.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    CalendarSyncScreen.SyncStatus.Error -> {
                        Icon(
                            painter = painterResource(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Sync error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    CalendarSyncScreen.SyncStatus.Idle -> {
                        if (state.isSyncEnabled) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Sync enabled",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last sync time
            val lastSyncText =
                if (state.lastSyncTime > 0L) {
                    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    "Last synced: ${sdf.format(Date(state.lastSyncTime))}"
                } else {
                    "Never synced"
                }
            Text(
                text = lastSyncText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Sync error details
            if (state.lastSyncError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Error: ${state.lastSyncError}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Sync enabled/disabled status
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    when {
                        state.syncStatus == CalendarSyncScreen.SyncStatus.Syncing -> "Syncing in progress..."
                        state.isSyncEnabled -> "Sync enabled — select calendars below to manage"
                        else -> "Select calendars below to enable sync"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Shows the list of available calendars with checkboxes for selection.
 * Displays a loading indicator while calendars are being loaded.
 * Shows an empty state message if no calendars are found.
 */
@Composable
private fun CalendarListSection(
    state: CalendarSyncScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.list_alt_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Available Calendars",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            when {
                state.isLoading -> {
                    // Loading state
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Text(
                                text = "Loading calendars...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                state.calendars.isEmpty() -> {
                    // Empty state
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.calendar_month_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = "No calendars found",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "No calendars are available on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                else -> {
                    // Calendar list
                    Column {
                        state.calendars.forEachIndexed { index, calendar ->
                            CalendarListItem(
                                calendar = calendar,
                                onToggle = { selected ->
                                    state.eventSink(
                                        CalendarSyncScreen.Event.CalendarToggled(
                                            calendarId = calendar.calendarId,
                                            selected = selected,
                                        ),
                                    )
                                },
                            )
                            if (index < state.calendars.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a single calendar entry in the list.
 *
 * Shows:
 * - Calendar color indicator (colored circle)
 * - Calendar display name
 * - Account name and type
 * - Selection checkbox
 */
@Composable
private fun CalendarListItem(
    calendar: SyncCalendar,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = calendar.calendarName,
                style = MaterialTheme.typography.titleSmall,
            )
        },
        supportingContent = {
            Text(
                text = "${calendar.accountName} · ${formatAccountType(calendar.accountType)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingContent = {
            // Calendar color indicator
            Box(
                modifier =
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            color = calendar.calendarColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                        ),
            )
        },
        trailingContent = {
            Checkbox(
                checked = calendar.isSelected,
                onCheckedChange = onToggle,
            )
        },
        colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

/**
 * Formats an Android account type string to a human-readable label.
 * Examples: "com.google" → "Google", "com.microsoft.exchange" → "Exchange"
 */
private fun formatAccountType(accountType: String): String =
    when {
        accountType.contains("google", ignoreCase = true) -> "Google"
        accountType.contains("microsoft", ignoreCase = true) || accountType.contains("exchange", ignoreCase = true) -> "Exchange"
        accountType.contains("outlook", ignoreCase = true) -> "Outlook"
        accountType.contains("yahoo", ignoreCase = true) -> "Yahoo"
        accountType.contains("caldav", ignoreCase = true) -> "CalDAV"
        accountType.contains("local", ignoreCase = true) -> "Local"
        else -> accountType.substringAfterLast(".").replaceFirstChar { it.uppercaseChar() }
    }

/**
 * Action buttons for calendar sync management.
 * Includes: "Sync Now", "Refresh Calendars", "Disconnect Sync".
 */
@Composable
private fun CalendarActionButtons(
    state: CalendarSyncScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Sync Now button
        Button(
            onClick = { state.eventSink(CalendarSyncScreen.Event.SyncNowClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.isSyncEnabled && state.syncStatus != CalendarSyncScreen.SyncStatus.Syncing,
        ) {
            if (state.syncStatus == CalendarSyncScreen.SyncStatus.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                text =
                    if (state.syncStatus == CalendarSyncScreen.SyncStatus.Syncing) {
                        "Syncing..."
                    } else {
                        "Sync Now"
                    },
            )
        }

        // Refresh Calendars button
        OutlinedButton(
            onClick = { state.eventSink(CalendarSyncScreen.Event.RefreshCalendarsClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Icon(
                painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text("Refresh Calendars")
        }

        // Disconnect button
        OutlinedButton(
            onClick = { state.eventSink(CalendarSyncScreen.Event.DisconnectClicked) },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Icon(
                painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
            Text("Disconnect Sync")
        }
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun CalendarSyncContentPermissionGrantedPreview() {
    TrmnlBuddyAppTheme {
        CalendarListContent(
            state =
                CalendarSyncScreen.State(
                    isLoading = false,
                    calendars =
                        listOf(
                            SyncCalendar(
                                calendarId = 1L,
                                calendarName = "Personal",
                                accountName = "user@gmail.com",
                                accountType = "com.google",
                                ownerAccount = "user@gmail.com",
                                calendarColor = 0xFF4285F4.toInt(),
                                isSelected = true,
                            ),
                            SyncCalendar(
                                calendarId = 2L,
                                calendarName = "Work",
                                accountName = "user@company.com",
                                accountType = "com.microsoft.exchange",
                                ownerAccount = "user@company.com",
                                calendarColor = 0xFF0F9D58.toInt(),
                                isSelected = false,
                            ),
                        ),
                    isSyncEnabled = true,
                    lastSyncTime = System.currentTimeMillis() - 3600_000,
                    syncStatus = CalendarSyncScreen.SyncStatus.Idle,
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun CalendarSyncContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        CalendarListContent(
            state =
                CalendarSyncScreen.State(
                    isLoading = true,
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun CalendarSyncContentEmptyPreview() {
    TrmnlBuddyAppTheme {
        CalendarListContent(
            state =
                CalendarSyncScreen.State(
                    isLoading = false,
                    calendars = emptyList(),
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun CalendarSyncContentPermissionRationalePreview() {
    TrmnlBuddyAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PermissionRationaleDialog(
                onRequestPermission = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CalendarSyncContentSyncingPreview() {
    TrmnlBuddyAppTheme {
        CalendarListContent(
            state =
                CalendarSyncScreen.State(
                    isLoading = false,
                    calendars =
                        listOf(
                            SyncCalendar(
                                calendarId = 1L,
                                calendarName = "Personal",
                                accountName = "user@gmail.com",
                                accountType = "com.google",
                                ownerAccount = "user@gmail.com",
                                calendarColor = 0xFF4285F4.toInt(),
                                isSelected = true,
                            ),
                        ),
                    isSyncEnabled = true,
                    syncStatus = CalendarSyncScreen.SyncStatus.Syncing,
                    eventSink = {},
                ),
        )
    }
}
