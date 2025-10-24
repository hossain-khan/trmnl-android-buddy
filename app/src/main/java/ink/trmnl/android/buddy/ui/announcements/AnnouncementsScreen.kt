package ink.trmnl.android.buddy.ui.announcements

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.util.BrowserUtils
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Screen for displaying all announcements.
 *
 * @param isEmbedded When true, hides the top app bar (for use in ContentHubScreen)
 */
@Parcelize
data class AnnouncementsScreen(
    val isEmbedded: Boolean = false,
) : Screen {
    data class State(
        val announcements: List<AnnouncementEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val filter: Filter = Filter.ALL,
        val unreadCount: Int = 0,
        val errorMessage: String? = null,
        val showTopBar: Boolean = true, // Control whether to show top app bar
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object Refresh : Event()

        data class FilterChanged(
            val filter: Filter,
        ) : Event()

        data class AnnouncementClicked(
            val announcement: AnnouncementEntity,
        ) : Event()

        data class ToggleReadStatus(
            val announcement: AnnouncementEntity,
        ) : Event()

        data object MarkAllAsRead : Event()
    }

    enum class Filter {
        ALL,
        UNREAD,
        READ,
    }
}

/**
 * Presenter for AnnouncementsScreen.
 */
@Inject
class AnnouncementsPresenter
    constructor(
        @Assisted private val screen: AnnouncementsScreen,
        @Assisted private val navigator: Navigator,
        @ApplicationContext private val context: Context,
        private val announcementRepository: AnnouncementRepository,
    ) : Presenter<AnnouncementsScreen.State> {
        @Composable
        override fun present(): AnnouncementsScreen.State {
            var announcements by rememberRetained { mutableStateOf<List<AnnouncementEntity>>(emptyList()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var isRefreshing by rememberRetained { mutableStateOf(false) }
            var filter by rememberRetained { mutableStateOf(AnnouncementsScreen.Filter.ALL) }
            var unreadCount by rememberRetained { mutableStateOf(0) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            // Capture theme colors for Custom Tabs
            val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
            val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

            // Collect announcements from repository
            LaunchedEffect(filter) {
                val flow =
                    when (filter) {
                        AnnouncementsScreen.Filter.ALL -> announcementRepository.getAllAnnouncements()
                        AnnouncementsScreen.Filter.UNREAD -> announcementRepository.getUnreadAnnouncements()
                        AnnouncementsScreen.Filter.READ -> announcementRepository.getReadAnnouncements()
                    }

                flow.collect { latestAnnouncements ->
                    announcements = latestAnnouncements
                    isLoading = false
                    isRefreshing = false
                }
            }

            // Collect unread count
            LaunchedEffect(Unit) {
                announcementRepository.getUnreadCount().collect { count ->
                    unreadCount = count
                }
            }

            return AnnouncementsScreen.State(
                announcements = announcements,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                filter = filter,
                unreadCount = unreadCount,
                errorMessage = errorMessage,
                showTopBar = !screen.isEmbedded, // Hide top bar when embedded
            ) { event ->
                when (event) {
                    AnnouncementsScreen.Event.BackClicked -> {
                        navigator.pop()
                    }

                    AnnouncementsScreen.Event.Refresh -> {
                        isRefreshing = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = announcementRepository.refreshAnnouncements()
                            if (result.isFailure) {
                                errorMessage = "Failed to refresh: ${result.exceptionOrNull()?.message}"
                            }
                            isRefreshing = false
                        }
                    }

                    is AnnouncementsScreen.Event.FilterChanged -> {
                        filter = event.filter
                        isLoading = true
                    }

                    is AnnouncementsScreen.Event.AnnouncementClicked -> {
                        // Open announcement in Chrome Custom Tabs
                        BrowserUtils.openUrlInCustomTab(
                            context = context,
                            url = event.announcement.link,
                            toolbarColor = primaryColor,
                            secondaryColor = surfaceColor,
                        )
                        // Mark announcement as read
                        coroutineScope.launch {
                            announcementRepository.markAsRead(event.announcement.id)
                        }
                    }

                    is AnnouncementsScreen.Event.ToggleReadStatus -> {
                        coroutineScope.launch {
                            if (event.announcement.isRead) {
                                announcementRepository.markAsUnread(event.announcement.id)
                            } else {
                                announcementRepository.markAsRead(event.announcement.id)
                            }
                        }
                    }

                    AnnouncementsScreen.Event.MarkAllAsRead -> {
                        coroutineScope.launch {
                            announcementRepository.markAllAsRead()
                        }
                    }
                }
            }
        }

        @CircuitInject(AnnouncementsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: AnnouncementsScreen,
                navigator: Navigator,
            ): AnnouncementsPresenter
        }
    }

/**
 * UI content for AnnouncementsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@CircuitInject(AnnouncementsScreen::class, AppScope::class)
@Composable
fun AnnouncementsContent(
    state: AnnouncementsScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (state.showTopBar) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Announcements")
                            if (state.unreadCount > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = state.unreadCount.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { state.eventSink(AnnouncementsScreen.Event.BackClicked) }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (state.unreadCount > 0) {
                FloatingActionButton(
                    onClick = { state.eventSink(AnnouncementsScreen.Event.MarkAllAsRead) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check_24dp_e8eaed_fill1_wght400_grad0_opsz24),
                        contentDescription = "Mark all as read",
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }

            state.announcements.isEmpty() -> {
                EmptyState(
                    filter = state.filter,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                AnnouncementsList(
                    announcements = state.announcements,
                    isRefreshing = state.isRefreshing,
                    filter = state.filter,
                    onRefresh = { state.eventSink(AnnouncementsScreen.Event.Refresh) },
                    onFilterChanged = { newFilter ->
                        state.eventSink(AnnouncementsScreen.Event.FilterChanged(newFilter))
                    },
                    onAnnouncementClick = { announcement ->
                        state.eventSink(AnnouncementsScreen.Event.AnnouncementClicked(announcement))
                    },
                    onToggleReadStatus = { announcement ->
                        state.eventSink(AnnouncementsScreen.Event.ToggleReadStatus(announcement))
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(
    filter: AnnouncementsScreen.Filter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val message =
            when (filter) {
                AnnouncementsScreen.Filter.ALL -> "No announcements available"
                AnnouncementsScreen.Filter.UNREAD -> "No unread announcements"
                AnnouncementsScreen.Filter.READ -> "No read announcements"
            }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AnnouncementsList(
    announcements: List<AnnouncementEntity>,
    isRefreshing: Boolean,
    filter: AnnouncementsScreen.Filter,
    onRefresh: () -> Unit,
    onFilterChanged: (AnnouncementsScreen.Filter) -> Unit,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    onToggleReadStatus: (AnnouncementEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            // Sticky filter chips at the top
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    FilterChips(
                        selectedFilter = filter,
                        onFilterChanged = onFilterChanged,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            // Group announcements by date
            val groupedAnnouncements = announcements.groupByDate()

            groupedAnnouncements.forEach { (dateCategory, items) ->
                stickyHeader {
                    DateHeader(dateCategory)
                }

                items(
                    items = items,
                    key = { it.id },
                ) { announcement ->
                    AnnouncementItem(
                        announcement = announcement,
                        onClick = { onAnnouncementClick(announcement) },
                        onToggleReadStatus = { onToggleReadStatus(announcement) },
                    )
                }
            }
        }
    }
}

/**
 * Modern segmented button row for filter selection.
 * Uses Material Design 3 SingleChoiceSegmentedButtonRow for better UX.
 */
@Composable
private fun FilterChips(
    selectedFilter: AnnouncementsScreen.Filter,
    onFilterChanged: (AnnouncementsScreen.Filter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth(),
    ) {
        AnnouncementsScreen.Filter.entries.forEachIndexed { index, filter ->
            SegmentedButton(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AnnouncementsScreen.Filter.entries.size,
                    ),
                icon = {
                    // Directly provide icon without using SegmentedButtonDefaults.Icon
                    // to prevent default checkmark behavior
                    Icon(
                        painter =
                            painterResource(
                                when (filter) {
                                    AnnouncementsScreen.Filter.ALL ->
                                        R.drawable.list_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                                    AnnouncementsScreen.Filter.UNREAD ->
                                        R.drawable.markunread_mailbox_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                    AnnouncementsScreen.Filter.READ ->
                                        R.drawable.done_all_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                },
                            ),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            ) {
                Text(
                    text =
                        when (filter) {
                            AnnouncementsScreen.Filter.ALL -> "All"
                            AnnouncementsScreen.Filter.UNREAD -> "Unread"
                            AnnouncementsScreen.Filter.READ -> "Read"
                        },
                )
            }
        }
    }
}

@Composable
private fun DateHeader(dateCategory: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Text(
            text = dateCategory,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementItem(
    announcement: AnnouncementEntity,
    onClick: () -> Unit,
    onToggleReadStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                    onToggleReadStatus()
                    false // Don't actually dismiss, just toggle
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                contentAlignment =
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                        Alignment.CenterEnd
                    } else {
                        Alignment.CenterStart
                    },
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (announcement.isRead) {
                                R.drawable.visibility_off_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            } else {
                                R.drawable.visibility_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            },
                        ),
                    contentDescription =
                        if (announcement.isRead) {
                            "Mark as unread"
                        } else {
                            "Mark as read"
                        },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        modifier = modifier,
    ) {
        ListItem(
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!announcement.isRead) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!announcement.isRead) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp),
                        ) {}
                    }
                }
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Only show summary if it's not blank
                    if (announcement.summary.isNotBlank()) {
                        Text(
                            text = announcement.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = formatRelativeDate(announcement.publishedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

/**
 * Group announcements by date category (Today, Yesterday, This Week, Older).
 */
private fun List<AnnouncementEntity>.groupByDate(): Map<String, List<AnnouncementEntity>> {
    val now = LocalDate.now()
    val today = now.atStartOfDay(ZoneId.systemDefault()).toInstant()
    val yesterday = now.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
    val thisWeekStart = now.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()

    return groupBy { announcement ->
        when {
            announcement.publishedDate >= today -> "Today"
            announcement.publishedDate >= yesterday -> "Yesterday"
            announcement.publishedDate >= thisWeekStart -> "This Week"
            else -> "Older"
        }
    }.toSortedMap(
        compareBy { category ->
            when (category) {
                "Today" -> 0
                "Yesterday" -> 1
                "This Week" -> 2
                "Older" -> 3
                else -> 4
            }
        },
    )
}

/**
 * Format date as relative time (e.g., "2 days ago", "1 hour ago").
 */
private fun formatRelativeDate(instant: Instant): String {
    val now = Instant.now()
    val days = ChronoUnit.DAYS.between(instant, now)
    val hours = ChronoUnit.HOURS.between(instant, now)
    val minutes = ChronoUnit.MINUTES.between(instant, now)

    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"} ago"
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        minutes > 0 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        else -> "Just now"
    }
}

// ============================================================================
// Compose Previews
// ============================================================================

private val sampleAnnouncements =
    listOf(
        AnnouncementEntity(
            id = "1",
            title = "New Feature: Device Scheduling",
            summary = "You can now schedule when your TRMNL device refreshes content automatically.",
            link = "https://usetrmnl.com/announcements/device-scheduling",
            publishedDate = Instant.now().minus(2, ChronoUnit.HOURS),
            isRead = false,
            fetchedAt = Instant.now().minus(2, ChronoUnit.HOURS),
        ),
        AnnouncementEntity(
            id = "2",
            title = "API Rate Limit Update",
            summary = "We've increased the API rate limit for all paid plans. Check the docs for details.",
            link = "https://usetrmnl.com/announcements/api-update",
            publishedDate = Instant.now().minus(1, ChronoUnit.DAYS),
            isRead = true,
            fetchedAt = Instant.now().minus(1, ChronoUnit.DAYS),
        ),
        AnnouncementEntity(
            id = "3",
            title = "Maintenance Window Scheduled",
            summary = "Brief maintenance scheduled for Saturday 2am-4am EST. Expect 5min downtime.",
            link = "https://usetrmnl.com/announcements/maintenance",
            publishedDate = Instant.now().minus(3, ChronoUnit.DAYS),
            isRead = false,
            fetchedAt = Instant.now().minus(3, ChronoUnit.DAYS),
        ),
    )

@Preview(name = "Loading State")
@Composable
private fun AnnouncementsLoadingPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = true,
                    showTopBar = true,
                ),
        )
    }
}

@Preview(name = "Empty State - All Filter")
@Composable
private fun AnnouncementsEmptyAllPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = false,
                    announcements = emptyList(),
                    filter = AnnouncementsScreen.Filter.ALL,
                    showTopBar = true,
                ),
        )
    }
}

@Preview(name = "Empty State - Unread Filter")
@Composable
private fun AnnouncementsEmptyUnreadPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = false,
                    announcements = emptyList(),
                    filter = AnnouncementsScreen.Filter.UNREAD,
                    showTopBar = true,
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Announcement Item - Unread")
@Composable
private fun AnnouncementItemUnreadPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            AnnouncementItem(
                announcement = sampleAnnouncements[0],
                onClick = {},
                onToggleReadStatus = {},
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Announcement Item - Read")
@Composable
private fun AnnouncementItemReadPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            AnnouncementItem(
                announcement = sampleAnnouncements[1],
                onClick = {},
                onToggleReadStatus = {},
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Filter Chips - All Selected")
@Composable
private fun FilterChipsAllPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            FilterChips(
                selectedFilter = AnnouncementsScreen.Filter.ALL,
                onFilterChanged = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Filter Chips - Unread Selected")
@Composable
private fun FilterChipsUnreadPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            FilterChips(
                selectedFilter = AnnouncementsScreen.Filter.UNREAD,
                onFilterChanged = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Full Screen - With Announcements")
@Composable
private fun AnnouncementsFullScreenPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = false,
                    announcements = sampleAnnouncements,
                    filter = AnnouncementsScreen.Filter.ALL,
                    unreadCount = 2,
                    showTopBar = true,
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Full Screen - Embedded (No TopBar)")
@Composable
private fun AnnouncementsEmbeddedPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = false,
                    announcements = sampleAnnouncements,
                    filter = AnnouncementsScreen.Filter.UNREAD,
                    unreadCount = 2,
                    showTopBar = false, // Embedded mode
                ),
        )
    }
}
