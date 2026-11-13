package ink.trmnl.android.buddy.ui.devicediagnostics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.DeviceTimeline
import ink.trmnl.android.buddy.api.models.TimelineCheckIn
import ink.trmnl.android.buddy.api.models.TimelineRecordedItem
import ink.trmnl.android.buddy.api.models.TimelineStep
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * UI for [DeviceDiagnosticsScreen] with Forecast, Timeline, and Logs tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DeviceDiagnosticsScreen::class, AppScope::class)
@Composable
fun DeviceDiagnosticsContent(
    state: DeviceDiagnosticsScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diagnostics & Forecast",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.deviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(DeviceDiagnosticsScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(DeviceDiagnosticsScreen.Event.RefreshClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.refresh_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Refresh",
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
                    .padding(innerPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                DeviceDiagnosticsScreen.DiagnosticsTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { state.eventSink(DeviceDiagnosticsScreen.Event.TabSelected(tab)) },
                        text = { Text(text = tab.label) },
                    )
                }
            }

            when (state.selectedTab) {
                DeviceDiagnosticsScreen.DiagnosticsTab.FORECAST -> {
                    ForecastTabContent(
                        selectedHours = state.forecastHours,
                        isLoading = state.isForecastLoading,
                        error = state.forecastError,
                        steps = state.forecastSteps,
                        onHoursSelected = { hours ->
                            state.eventSink(DeviceDiagnosticsScreen.Event.ForecastHoursSelected(hours))
                        },
                        onRetry = { state.eventSink(DeviceDiagnosticsScreen.Event.RefreshClicked) },
                    )
                }

                DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE -> {
                    TimelineTabContent(
                        selectedHours = state.timelineHours,
                        isLoading = state.isTimelineLoading,
                        error = state.timelineError,
                        timeline = state.timeline,
                        onHoursSelected = { hours ->
                            state.eventSink(DeviceDiagnosticsScreen.Event.TimelineHoursSelected(hours))
                        },
                        onRetry = { state.eventSink(DeviceDiagnosticsScreen.Event.RefreshClicked) },
                    )
                }

                DeviceDiagnosticsScreen.DiagnosticsTab.LOGS -> {
                    LogsTabContent(
                        selectedLevel = state.selectedLogLevel,
                        isLoading = state.isLogsLoading,
                        isLoadingMore = state.isLoadingMoreLogs,
                        error = state.logsError,
                        logs = state.logs,
                        hasMore = state.hasMoreLogs,
                        onLevelSelected = { level ->
                            state.eventSink(DeviceDiagnosticsScreen.Event.LogLevelSelected(level))
                        },
                        onLoadMore = { state.eventSink(DeviceDiagnosticsScreen.Event.LoadMoreLogs) },
                        onRetry = { state.eventSink(DeviceDiagnosticsScreen.Event.RefreshClicked) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastTabContent(
    selectedHours: Int,
    isLoading: Boolean,
    error: String?,
    steps: List<TimelineStep>,
    onHoursSelected: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterChipBar(
            options = listOf(12 to "12h", 24 to "24h", 48 to "48h"),
            selectedOption = selectedHours,
            onSelected = onHoursSelected,
        )

        when {
            isLoading -> {
                LoadingState(message = "Loading check-in forecast...")
            }

            error != null -> {
                ErrorState(message = error, onRetry = onRetry)
            }

            steps.isEmpty() -> {
                EmptyState(
                    title = "No upcoming check-ins scheduled",
                    subtitle = "No forecast steps were returned for the next ${selectedHours}h window.",
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "next_checkin_summary") {
                        NextCheckInHeroCard(step = steps.first())
                    }

                    itemsIndexed(
                        items = steps,
                        key = { index, step -> "${step.at}_$index" },
                    ) { index, step ->
                        ForecastStepCard(
                            step = step,
                            isNext = index == 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextCheckInHeroCard(step: TimelineStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.clock_loader_60_24dp_999999_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Next Expected Check-in",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = formatDisplayTimestamp(step.at),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Screen: ${step.name ?: "Scheduled item"}",
                style = MaterialTheme.typography.bodyMedium,
            )

            val cadenceSeconds = step.refreshRateSeconds ?: step.refreshSeconds
            val reasonText = step.reason?.takeIf { it.isNotBlank() }
            if (cadenceSeconds != null || reasonText != null) {
                val metaParts =
                    buildList {
                        if (cadenceSeconds != null) add("Every ${formatDurationSeconds(cadenceSeconds)}")
                        if (reasonText != null) add(reasonText.replace('_', ' '))
                    }
                Text(
                    text = metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun ForecastStepCard(
    step: TimelineStep,
    isNext: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = step.name ?: "Scheduled Render",
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatDisplayTimestamp(step.at),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val renderInfo =
                        listOfNotNull(
                            step.reason?.replace('_', ' '),
                            step.renderReason?.replace('_', ' '),
                        ).distinct().joinToString(" • ")
                    if (renderInfo.isNotBlank()) {
                        Text(
                            text = renderInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            trailingContent = {
                val cadence = step.refreshRateSeconds ?: step.refreshSeconds
                Column(horizontalAlignment = Alignment.End) {
                    if (isNext) {
                        StatusBadge(
                            label = "Next",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (cadence != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDurationSeconds(cadence),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun TimelineTabContent(
    selectedHours: Int,
    isLoading: Boolean,
    error: String?,
    timeline: DeviceTimeline?,
    onHoursSelected: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterChipBar(
            options = listOf(3 to "3h", 12 to "12h", 24 to "24h"),
            selectedOption = selectedHours,
            onSelected = onHoursSelected,
        )

        when {
            isLoading -> {
                LoadingState(message = "Loading activity timeline...")
            }

            error != null -> {
                ErrorState(message = error, onRetry = onRetry)
            }

            timeline == null -> {
                EmptyState(
                    title = "Timeline unavailable",
                    subtitle = "Pull to refresh or check back shortly.",
                )
            }

            !timeline.available || timeline.queryFailed -> {
                EmptyState(
                    title = "Timeline data unavailable",
                    subtitle = "Server reported no available timeline data for ${timeline.date}.",
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    timeline.checkIn?.let { checkIn ->
                        item(key = "check_in_status") {
                            CheckInHealthCard(
                                checkIn = checkIn,
                                zone = timeline.zone,
                                truncated = timeline.truncated,
                            )
                        }
                    }

                    if (timeline.recorded.isNotEmpty()) {
                        item(key = "recorded_header") {
                            SectionHeader(title = "Recorded Events (${timeline.recorded.size})")
                        }
                        itemsIndexed(
                            items = timeline.recorded,
                            key = { index, item -> "rec_${item.at}_$index" },
                        ) { _, item ->
                            RecordedTimelineCard(item = item)
                        }
                    }

                    if (timeline.expected.isNotEmpty()) {
                        item(key = "expected_header") {
                            SectionHeader(title = "Expected Next (${timeline.expected.size})")
                        }
                        itemsIndexed(
                            items = timeline.expected,
                            key = { index, step -> "exp_${step.at}_$index" },
                        ) { _, step ->
                            ForecastStepCard(step = step, isNext = false)
                        }
                    }

                    if (timeline.recorded.isEmpty() && timeline.expected.isEmpty()) {
                        item(key = "timeline_empty") {
                            EmptyState(
                                title = "No timeline events",
                                subtitle = "No recorded or expected events in this ${selectedHours}h window.",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInHealthCard(
    checkIn: TimelineCheckIn,
    zone: String,
    truncated: Boolean,
) {
    val (containerColor, contentColor, iconRes, label) =
        when (checkIn.state.lowercase()) {
            "on_time" -> {
                Quadruple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    R.drawable.check_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                    "On Time",
                )
            }

            "overdue" -> {
                Quadruple(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                    "Overdue",
                )
            }

            "asleep" -> {
                Quadruple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    R.drawable.clock_loader_60_24dp_999999_fill0_wght400_grad0_opsz24,
                    "Asleep",
                )
            }

            else -> {
                Quadruple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    R.drawable.timeline_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                    checkIn.state.replace('_', ' ').replaceFirstChar { it.uppercase() },
                )
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painter = painterResource(iconRes), contentDescription = null)
                Text(
                    text = "Check-in Status: $label",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            checkIn.expectedAt?.let { expectedAt ->
                Text(
                    text = "Expected: ${formatDisplayTimestamp(expectedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = if (truncated) "Timezone: $zone • Truncated results" else "Timezone: $zone",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun RecordedTimelineCard(item: TimelineRecordedItem) {
    val outcomeLower = item.outcome?.lowercase().orEmpty()
    val isFailure = outcomeLower.contains("fail") || outcomeLower.contains("error") || outcomeLower.contains("miss")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = item.name ?: "Device Activity",
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = formatDisplayTimestamp(item.at),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (item.supporting.isNotEmpty() || item.traceEntries.isNotEmpty()) {
                        Text(
                            text = "${item.supporting.size} supporting • ${item.traceEntries.size} trace entries",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            trailingContent = {
                item.outcome?.takeIf { it.isNotBlank() }?.let { outcome ->
                    StatusBadge(
                        label = outcome.replace('_', ' '),
                        containerColor =
                            if (isFailure) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        contentColor =
                            if (isFailure) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                    )
                }
            },
        )
    }
}

@Composable
private fun LogsTabContent(
    selectedLevel: DeviceDiagnosticsScreen.LogLevelFilter,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    logs: List<DeviceDiagnosticsScreen.DeviceLogEntry>,
    hasMore: Boolean,
    onLevelSelected: (DeviceDiagnosticsScreen.LogLevelFilter) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeviceDiagnosticsScreen.LogLevelFilter.entries.forEach { level ->
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = { onLevelSelected(level) },
                    label = { Text(level.label) },
                )
            }
        }

        when {
            isLoading -> {
                LoadingState(message = "Loading device logs...")
            }

            error != null && logs.isEmpty() -> {
                ErrorState(message = error, onRetry = onRetry)
            }

            logs.isEmpty() -> {
                EmptyState(
                    title = "No logs found",
                    subtitle = "No ${selectedLevel.label.lowercase()} log entries recorded for this device.",
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (error != null) {
                        item(key = "logs_inline_error") {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    items(
                        items = logs,
                        key = { it.id },
                    ) { entry ->
                        LogEntryCard(entry = entry)
                    }

                    if (hasMore) {
                        item(key = "load_more_logs") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    enabled = !isLoadingMore,
                                ) {
                                    if (isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                    Text(if (isLoadingMore) "Loading..." else "Load Older Logs")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: DeviceDiagnosticsScreen.DeviceLogEntry) {
    val levelLower = entry.level?.lowercase()
    val (badgeBg, badgeFg, iconRes) =
        when (levelLower) {
            "error" -> {
                Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    R.drawable.data_info_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                )
            }

            "warn", "warning" -> {
                Triple(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                )
            }

            "info" -> {
                Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                )
            }

            else -> {
                Triple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24,
                )
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = badgeFg,
                    )
                    entry.level?.let { level ->
                        StatusBadge(
                            label = level.uppercase(),
                            containerColor = badgeBg,
                            contentColor = badgeFg,
                        )
                    }
                    entry.source?.let { source ->
                        StatusBadge(
                            label = source.replace('_', ' '),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
                Text(
                    text = formatDisplayTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodyMedium,
            )

            entry.details?.let { details ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun <T> FilterChipBar(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelected: (T) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selectedOption == value,
                onClick = { onSelected(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun StatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun LoadingState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.data_info_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

internal fun formatDisplayTimestamp(isoTimestamp: String): String =
    runCatching {
        val instant = Instant.parse(isoTimestamp)
        val formatter =
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    }.getOrDefault(isoTimestamp)

internal fun formatDurationSeconds(seconds: Int): String =
    when {
        seconds >= 3600 && seconds % 3600 == 0 -> "${seconds / 3600}h"
        seconds >= 60 -> "${seconds / 60}m"
        else -> "${seconds}s"
    }

@PreviewLightDark
@Composable
private fun DeviceDiagnosticsForecastPreview() {
    TrmnlBuddyAppTheme {
        DeviceDiagnosticsContent(
            state =
                DeviceDiagnosticsScreen.State(
                    deviceName = "Living Room TRMNL",
                    selectedTab = DeviceDiagnosticsScreen.DiagnosticsTab.FORECAST,
                    isForecastLoading = false,
                    forecastSteps =
                        listOf(
                            TimelineStep(
                                at = "2026-11-13T05:15:00Z",
                                name = "Weather & Calendar",
                                refreshRateSeconds = 900,
                                reason = "scheduled_refresh",
                            ),
                            TimelineStep(
                                at = "2026-11-13T05:30:00Z",
                                name = "GitHub Contributions",
                                refreshRateSeconds = 900,
                                reason = "playlist_rotation",
                            ),
                        ),
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceDiagnosticsTimelinePreview() {
    TrmnlBuddyAppTheme {
        DeviceDiagnosticsContent(
            state =
                DeviceDiagnosticsScreen.State(
                    deviceName = "Living Room TRMNL",
                    selectedTab = DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE,
                    isTimelineLoading = false,
                    timeline =
                        DeviceTimeline(
                            date = "2026-11-13",
                            today = "2026-11-13",
                            zone = "America/New_York",
                            available = true,
                            checkIn =
                                TimelineCheckIn(
                                    state = "on_time",
                                    expectedAt = "2026-11-13T05:15:00Z",
                                ),
                            recorded =
                                listOf(
                                    TimelineRecordedItem(
                                        at = "2026-11-13T05:00:00Z",
                                        name = "Weather & Calendar",
                                        outcome = "delivered",
                                        leading =
                                            buildJsonObject {
                                                put("status", JsonPrimitive(200))
                                            },
                                    ),
                                ),
                        ),
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DeviceDiagnosticsLogsPreview() {
    TrmnlBuddyAppTheme {
        DeviceDiagnosticsContent(
            state =
                DeviceDiagnosticsScreen.State(
                    deviceName = "Living Room TRMNL",
                    selectedTab = DeviceDiagnosticsScreen.DiagnosticsTab.LOGS,
                    isLogsLoading = false,
                    logs =
                        listOf(
                            DeviceDiagnosticsScreen.DeviceLogEntry(
                                id = "evt_1",
                                timestamp = "2026-11-13T05:00:00Z",
                                level = "info",
                                source = "device",
                                message = "Screen fetched successfully",
                                details = "rssi: -62 • battery_voltage: 4.12",
                            ),
                            DeviceDiagnosticsScreen.DeviceLogEntry(
                                id = "evt_2",
                                timestamp = "2026-11-13T04:45:00Z",
                                level = "warn",
                                source = "plugin",
                                message = "Slow upstream response (2.4s)",
                            ),
                        ),
                    hasMoreLogs = true,
                ),
        )
    }
}
