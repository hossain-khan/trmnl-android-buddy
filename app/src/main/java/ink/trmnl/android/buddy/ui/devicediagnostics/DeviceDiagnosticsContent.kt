package ink.trmnl.android.buddy.ui.devicediagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                        onUpdateApiKey = { state.eventSink(DeviceDiagnosticsScreen.Event.UpdateApiKeyClicked) },
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
                        onUpdateApiKey = { state.eventSink(DeviceDiagnosticsScreen.Event.UpdateApiKeyClicked) },
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
                        onUpdateApiKey = { state.eventSink(DeviceDiagnosticsScreen.Event.UpdateApiKeyClicked) },
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
    error: DeviceDiagnosticsScreen.DiagnosticsError?,
    steps: List<TimelineStep>,
    onHoursSelected: (Int) -> Unit,
    onUpdateApiKey: () -> Unit,
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
                ErrorState(
                    error = error,
                    onUpdateApiKey = onUpdateApiKey,
                    onRetry = onRetry,
                )
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
                text = formatStepTitle(step),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )

            val cadenceSeconds = step.refreshRateSeconds ?: step.refreshSeconds
            val reasonLabel = formatStepReason(step.reason)
            val isSleepWakeup = step.name == null && step.reason?.equals("asleep", ignoreCase = true) == true
            if (cadenceSeconds != null || reasonLabel != null) {
                val metaParts =
                    buildList {
                        if (cadenceSeconds != null && !isSleepWakeup) {
                            add("Stays active ${formatDurationSeconds(cadenceSeconds)}")
                        }
                        if (reasonLabel != null) add(reasonLabel)
                    }
                if (metaParts.isNotEmpty()) {
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecastStepCard(
    step: TimelineStep,
    isNext: Boolean,
) {
    val cadence = step.refreshRateSeconds ?: step.refreshSeconds
    val isSleepWakeup = step.name == null && step.reason?.equals("asleep", ignoreCase = true) == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isNext) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
            ),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = formatStepTitle(step),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
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
                            formatStepReason(step.reason),
                            formatStepReason(step.renderReason),
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
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (isNext) {
                        StatusBadge(
                            label = "Next",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (cadence != null && !isSleepWakeup) {
                        StatusBadge(
                            label = "Active ${formatDurationSeconds(cadence)}",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
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
    error: DeviceDiagnosticsScreen.DiagnosticsError?,
    timeline: DeviceTimeline?,
    onHoursSelected: (Int) -> Unit,
    onUpdateApiKey: () -> Unit,
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
                ErrorState(
                    error = error,
                    onUpdateApiKey = onUpdateApiKey,
                    onRetry = onRetry,
                )
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
                val totalNodes = timeline.recorded.size + timeline.expected.size
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    timeline.checkIn?.let { checkIn ->
                        item(key = "check_in_status") {
                            CheckInHealthCard(
                                checkIn = checkIn,
                                zone = timeline.zone,
                                truncated = timeline.truncated,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    if (timeline.recorded.isNotEmpty()) {
                        item(key = "recorded_header") {
                            SectionHeader(title = "Recorded (${timeline.recorded.size})")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = timeline.recorded,
                            key = { index, item -> "rec_${item.at}_$index" },
                        ) { index, item ->
                            val outcomeLower = item.outcome?.lowercase().orEmpty()
                            val isFailure =
                                outcomeLower.contains("fail") ||
                                    outcomeLower.contains("error") ||
                                    outcomeLower.contains("miss")
                            val globalIndex = index
                            VerticalTimelineRow(
                                timeLabel = formatShortTime(item.at),
                                nodeType =
                                    if (isFailure) {
                                        TimelineNodeType.RECORDED_FAILURE
                                    } else {
                                        TimelineNodeType.RECORDED_SUCCESS
                                    },
                                isFirst = globalIndex == 0,
                                isLast = globalIndex == totalNodes - 1,
                                title = item.name ?: "Device Check-in",
                                subtitle =
                                    if (item.supporting.isNotEmpty() || item.traceEntries.isNotEmpty()) {
                                        "${item.supporting.size} supporting • ${item.traceEntries.size} trace entries"
                                    } else {
                                        formatDisplayTimestamp(item.at)
                                    },
                                badgeLabel = item.outcome?.takeIf { it.isNotBlank() }?.replace('_', ' '),
                                badgeContainerColor =
                                    if (isFailure) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                badgeContentColor =
                                    if (isFailure) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    },
                            )
                        }
                    }

                    if (timeline.expected.isNotEmpty()) {
                        item(key = "expected_header") {
                            SectionHeader(
                                title =
                                    if (timeline.recorded.isNotEmpty()) {
                                        "Upcoming Schedule (${timeline.expected.size})"
                                    } else {
                                        "Upcoming Schedule (${timeline.expected.size}) — No recorded activity yet"
                                    },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(
                            items = timeline.expected,
                            key = { index, step -> "exp_${step.at}_$index" },
                        ) { index, step ->
                            val globalIndex = timeline.recorded.size + index
                            val isNext = index == 0
                            val cadence = step.refreshRateSeconds ?: step.refreshSeconds
                            val isSleepWakeup = step.name == null && step.reason?.equals("asleep", ignoreCase = true) == true
                            VerticalTimelineRow(
                                timeLabel = formatShortTime(step.at),
                                nodeType = if (isNext) TimelineNodeType.NEXT_UP else TimelineNodeType.UPCOMING,
                                isFirst = globalIndex == 0,
                                isLast = globalIndex == totalNodes - 1,
                                title = formatStepTitle(step),
                                subtitle =
                                    listOfNotNull(
                                        formatStepReason(step.reason),
                                        formatStepReason(step.renderReason),
                                    ).distinct().joinToString(" • ").ifBlank { formatDisplayTimestamp(step.at) },
                                badgeLabel =
                                    when {
                                        isSleepWakeup -> "Wake-up"
                                        cadence != null -> "Active ${formatDurationSeconds(cadence)}"
                                        else -> null
                                    },
                                badgeContainerColor =
                                    if (isNext) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                badgeContentColor =
                                    if (isNext) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                            )
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

private enum class TimelineNodeType {
    RECORDED_SUCCESS,
    RECORDED_FAILURE,
    NEXT_UP,
    UPCOMING,
}

@Composable
private fun VerticalTimelineRow(
    timeLabel: String,
    nodeType: TimelineNodeType,
    isFirst: Boolean,
    isLast: Boolean,
    title: String,
    subtitle: String,
    badgeLabel: String?,
    badgeContainerColor: Color,
    badgeContentColor: Color,
) {
    val railColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (nodeType == TimelineNodeType.NEXT_UP) FontWeight.Bold else FontWeight.Medium,
            color =
                if (nodeType == TimelineNodeType.NEXT_UP) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            textAlign = TextAlign.End,
            modifier =
                Modifier
                    .width(68.dp)
                    .padding(top = 14.dp, end = 6.dp),
        )

        Box(
            modifier =
                Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (!isFirst) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(14.dp)
                            .background(railColor),
                )
            }
            if (!isLast) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 24.dp)
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(railColor),
                )
            }

            Box(
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (nodeType) {
                    TimelineNodeType.RECORDED_SUCCESS -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_24dp_e8eaed_fill1_wght400_grad0_opsz24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    TimelineNodeType.RECORDED_FAILURE -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(errorColor),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.data_info_alert_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    TimelineNodeType.NEXT_UP -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(primaryContainer)
                                    .border(2.dp, primaryColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor),
                            )
                        }
                    }

                    TimelineNodeType.UPCOMING -> {
                        Box(
                            modifier =
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(surfaceColor)
                                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                    }
                }
            }
        }

        Card(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 6.dp, bottom = 10.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (nodeType == TimelineNodeType.NEXT_UP) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    if (badgeLabel != null) {
                        StatusBadge(
                            label = badgeLabel,
                            containerColor = badgeContainerColor,
                            contentColor = badgeContentColor,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LogsTabContent(
    selectedLevel: DeviceDiagnosticsScreen.LogLevelFilter,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: DeviceDiagnosticsScreen.DiagnosticsError?,
    logs: List<DeviceDiagnosticsScreen.DeviceLogEntry>,
    hasMore: Boolean,
    onLevelSelected: (DeviceDiagnosticsScreen.LogLevelFilter) -> Unit,
    onLoadMore: () -> Unit,
    onUpdateApiKey: () -> Unit,
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
                ErrorState(
                    error = error,
                    onUpdateApiKey = onUpdateApiKey,
                    onRetry = onRetry,
                )
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
                                text = error.message,
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
    error: DeviceDiagnosticsScreen.DiagnosticsError,
    onUpdateApiKey: () -> Unit,
    onRetry: () -> Unit,
) {
    when (error) {
        is DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities -> {
            CapabilityErrorState(
                error = error,
                onUpdateApiKey = onUpdateApiKey,
                onRetry = onRetry,
            )
        }

        is DeviceDiagnosticsScreen.DiagnosticsError.General -> {
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
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityErrorState(
    error: DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities,
    onUpdateApiKey: () -> Unit,
    onRetry: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "API Key Capabilities Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                )

                Text(
                    text = "Generate a new key at ${error.accountUrl} with device diagnostics capabilities, then update it in the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { uriHandler.openUri(error.accountUrl) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Create New API Key")
                    }

                    OutlinedButton(
                        onClick = onUpdateApiKey,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Update Key in App")
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        TextButton(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
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

internal fun formatShortTime(isoTimestamp: String): String =
    runCatching {
        val instant = Instant.parse(isoTimestamp)
        val formatter =
            DateTimeFormatter
                .ofLocalizedTime(FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    }.getOrDefault(isoTimestamp)

internal fun formatStepTitle(step: TimelineStep): String =
    step.name?.takeIf { it.isNotBlank() }
        ?: if (step.reason.equals("asleep", ignoreCase = true)) {
            "Waking from sleep"
        } else {
            "Scheduled Screen"
        }

internal fun formatStepReason(reason: String?): String? =
    when (reason?.lowercase()?.trim()) {
        null, "" -> null
        "asleep" -> "Sleep mode ends"
        "rotation" -> "Playlist rotation"
        else -> reason.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

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

@PreviewLightDark
@Composable
private fun DeviceDiagnosticsCapabilityErrorPreview() {
    TrmnlBuddyAppTheme {
        DeviceDiagnosticsContent(
            state =
                DeviceDiagnosticsScreen.State(
                    deviceName = "Living Room TRMNL",
                    selectedTab = DeviceDiagnosticsScreen.DiagnosticsTab.TIMELINE,
                    isTimelineLoading = false,
                    timelineError =
                        DeviceDiagnosticsScreen.DiagnosticsError.InsufficientKeyCapabilities(
                            message =
                                "This is a legacy API key, which reaches only the endpoints it had before " +
                                    "API keys held capabilities. Create an API key with the capabilities " +
                                    "you need at https://trmnl.com/account.",
                        ),
                ),
        )
    }
}
