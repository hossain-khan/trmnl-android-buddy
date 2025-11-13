package ink.trmnl.android.buddy.dev

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.dev.DevelopmentScreen.Event
import kotlin.math.roundToInt

/**
 * UI for the Development screen.
 *
 * Provides testing controls for:
 * - Low battery notifications (1-5 devices, adjustable threshold)
 * - Blog post notifications (1-10 posts)
 * - Announcement notifications (1-10 announcements)
 * - One-time worker triggers
 * - Notification permission management
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@CircuitInject(DevelopmentScreen::class, AppScope::class)
@Composable
fun DevelopmentContent(
    state: DevelopmentScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Development Tools") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(Event.NavigateBack) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
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
            // Permission status section
            PermissionStatusSection(
                permissionGranted = state.notificationPermissionGranted,
                onRequestPermission = { state.eventSink(Event.RequestNotificationPermission) },
                onOpenSettings = { state.eventSink(Event.OpenNotificationSettings) },
            )

            // Notification testing sections
            NotificationTestingSection(
                title = "Low Battery Notifications",
                description = "Test low battery alerts with mock device data",
                eventSink = state.eventSink,
            )

            RssFeedNotificationTestingSection(
                title = "RSS Feed Notifications",
                description = "Test blog post and announcement notifications",
                eventSink = state.eventSink,
            )

            // Worker testing section
            WorkerTestingSection(
                title = "Worker Triggers",
                description = "Manually trigger one-time worker executions",
                eventSink = state.eventSink,
            )

            // Worker status section
            WorkerStatusSection(
                title = "Background Workers",
                description = "Monitor worker status and execution",
                workerStatuses = state.workerStatuses,
                eventSink = state.eventSink,
            )

            // Development info
            DevelopmentInfoSection()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionStatusSection(
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (permissionGranted) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Notification Permission",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (permissionGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                )

                Text(
                    text = if (permissionGranted) "✓ Granted" else "✗ Denied",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (permissionGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                )
            }

            if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Text(
                    text = "Notifications require POST_NOTIFICATIONS permission on Android 13+",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )

                val permissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { permissionState.launchPermissionRequest() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Request Permission")
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = null,
                        )
                        Text("Settings")
                    }
                }
            } else if (permissionGranted) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                    )
                    Text("Open Notification Settings")
                }
            }
        }
    }
}

@Composable
private fun NotificationTestingSection(
    title: String,
    description: String,
    eventSink: (Event) -> Unit,
) {
    var deviceCount by remember { mutableIntStateOf(1) }
    var thresholdPercent by remember { mutableFloatStateOf(20f) }

    SectionCard(title = title, description = description) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Device count slider
            Column {
                Text(
                    text = "Number of devices: $deviceCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = deviceCount.toFloat(),
                    onValueChange = { deviceCount = it.roundToInt() },
                    valueRange = 1f..5f,
                    steps = 3,
                )
            }

            // Threshold slider
            Column {
                Text(
                    text = "Battery threshold: ${thresholdPercent.roundToInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = thresholdPercent,
                    onValueChange = { thresholdPercent = it },
                    valueRange = 5f..50f,
                    steps = 8,
                )
            }

            // Test button
            OutlinedButton(
                onClick = {
                    eventSink(
                        Event.TestLowBatteryNotification(
                            deviceCount = deviceCount,
                            thresholdPercent = thresholdPercent.roundToInt(),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text("Test Low Battery Notification")
            }
        }
    }
}

@Composable
private fun RssFeedNotificationTestingSection(
    title: String,
    description: String,
    eventSink: (Event) -> Unit,
) {
    var blogPostCount by remember { mutableIntStateOf(1) }
    var announcementCount by remember { mutableIntStateOf(1) }

    SectionCard(title = title, description = description) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Blog posts
            Column {
                Text(
                    text = "Blog Posts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "New posts: $blogPostCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = blogPostCount.toFloat(),
                    onValueChange = { blogPostCount = it.roundToInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                )
                OutlinedButton(
                    onClick = { eventSink(Event.TestBlogPostNotification(blogPostCount)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Test Blog Post Notification")
                }
            }

            HorizontalDivider()

            // Announcements
            Column {
                Text(
                    text = "Announcements",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "New announcements: $announcementCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = announcementCount.toFloat(),
                    onValueChange = { announcementCount = it.roundToInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                )
                OutlinedButton(
                    onClick = { eventSink(Event.TestAnnouncementNotification(announcementCount)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Test Announcement Notification")
                }
            }
        }
    }
}

@Composable
private fun WorkerTestingSection(
    title: String,
    description: String,
    eventSink: (Event) -> Unit,
) {
    SectionCard(title = title, description = description) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Triggers one-time execution with real data from TRMNL API/RSS feeds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { eventSink(Event.TriggerLowBatteryWorker) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Trigger Low Battery Worker")
            }

            OutlinedButton(
                onClick = { eventSink(Event.TriggerBlogPostWorker) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Trigger Blog Post Worker")
            }

            OutlinedButton(
                onClick = { eventSink(Event.TriggerAnnouncementWorker) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Trigger Announcement Worker")
            }

            OutlinedButton(
                onClick = { eventSink(Event.TriggerBatteryCollectionWorker) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Trigger Battery Collection Worker")
            }
        }
    }
}

@Composable
private fun WorkerStatusSection(
    title: String,
    description: String,
    workerStatuses: List<ink.trmnl.android.buddy.work.WorkerStatus>,
    eventSink: (Event) -> Unit,
) {
    SectionCard(title = title, description = description) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (workerStatuses.isEmpty()) {
                Text(
                    text = "No workers scheduled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                workerStatuses.forEach { workerStatus ->
                    WorkerStatusItem(workerStatus)
                }
            }

            HorizontalDivider()

            // Worker management buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { eventSink(Event.CancelAllWorkers) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel All")
                }

                OutlinedButton(
                    onClick = { eventSink(Event.ResetWorkerSchedules) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Reset All")
                }
            }
        }
    }
}

@Composable
private fun WorkerStatusItem(workerStatus: ink.trmnl.android.buddy.work.WorkerStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Worker name and state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = workerStatus.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = workerStatus.state.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        when (workerStatus.state) {
                            androidx.work.WorkInfo.State.SUCCEEDED -> MaterialTheme.colorScheme.primary
                            androidx.work.WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.tertiary
                            androidx.work.WorkInfo.State.ENQUEUED -> MaterialTheme.colorScheme.secondary
                            androidx.work.WorkInfo.State.FAILED -> MaterialTheme.colorScheme.error
                            androidx.work.WorkInfo.State.BLOCKED -> MaterialTheme.colorScheme.outline
                            androidx.work.WorkInfo.State.CANCELLED -> MaterialTheme.colorScheme.outline
                        },
                )
            }

            // Run attempt count
            if (workerStatus.runAttemptCount > 0) {
                Text(
                    text = "Run attempts: ${workerStatus.runAttemptCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Constraints
            workerStatus.constraints?.let { constraints ->
                Text(
                    text = "Constraints: $constraints",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Tags (for debugging)
            if (workerStatus.tags.isNotEmpty()) {
                Text(
                    text = "Tags: ${workerStatus.tags.take(3).joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DevelopmentInfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "ℹ️ Development Mode",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "This screen is only available in debug builds.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "• Notification tests use mock data (no API calls)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "• Worker triggers use real API/RSS feed data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "• Check logcat for detailed execution logs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            content()
        }
    }
}
