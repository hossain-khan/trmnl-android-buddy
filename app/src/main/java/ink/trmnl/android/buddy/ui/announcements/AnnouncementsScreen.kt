package ink.trmnl.android.buddy.ui.announcements

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import kotlin.time.Duration.Companion.days

/**
 * Filter options for announcements.
 */
enum class AnnouncementFilter {
    ALL,
    UNREAD,
    READ,
}

/**
 * Date grouping for announcements.
 */
enum class DateGroup {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    OLDER,
}

/**
 * Screen for viewing all announcements with filtering and grouping.
 */
@Parcelize
data object AnnouncementsScreen : Screen {
    data class State(
        val announcements: List<AnnouncementEntity> = emptyList(),
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val currentFilter: AnnouncementFilter = AnnouncementFilter.ALL,
        val unreadCount: Int = 0,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object RefreshClicked : Event()

        data object MarkAllAsReadClicked : Event()

        data class FilterChanged(
            val filter: AnnouncementFilter,
        ) : Event()

        data class AnnouncementClicked(
            val announcement: AnnouncementEntity,
        ) : Event()

        data class ToggleReadStatus(
            val announcement: AnnouncementEntity,
        ) : Event()
    }
}

/**
 * Presenter for AnnouncementsScreen.
 */
@Inject
class AnnouncementsPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val announcementRepository: AnnouncementRepository,
    ) : Presenter<AnnouncementsScreen.State> {
        @Composable
        override fun present(): AnnouncementsScreen.State {
            var announcements by rememberRetained { mutableStateOf<List<AnnouncementEntity>>(emptyList()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var isRefreshing by rememberRetained { mutableStateOf(false) }
            var currentFilter by rememberRetained { mutableStateOf(AnnouncementFilter.ALL) }
            var unreadCount by rememberRetained { mutableStateOf(0) }
            val coroutineScope = rememberCoroutineScope()

            // Collect announcements based on filter
            LaunchedEffect(currentFilter) {
                val flow =
                    when (currentFilter) {
                        AnnouncementFilter.ALL -> announcementRepository.getAllAnnouncements()
                        AnnouncementFilter.UNREAD -> announcementRepository.getUnreadAnnouncements()
                        AnnouncementFilter.READ -> announcementRepository.getReadAnnouncements()
                    }

                flow.collect { fetchedAnnouncements ->
                    announcements = fetchedAnnouncements
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
                currentFilter = currentFilter,
                unreadCount = unreadCount,
            ) { event ->
                when (event) {
                    AnnouncementsScreen.Event.BackClicked -> {
                        navigator.pop()
                    }

                    AnnouncementsScreen.Event.RefreshClicked -> {
                        isRefreshing = true
                        coroutineScope.launch {
                            announcementRepository.refreshAnnouncements()
                            // Flow will automatically update the announcements
                        }
                    }

                    AnnouncementsScreen.Event.MarkAllAsReadClicked -> {
                        coroutineScope.launch {
                            announcementRepository.markAllAsRead()
                        }
                    }

                    is AnnouncementsScreen.Event.FilterChanged -> {
                        currentFilter = event.filter
                    }

                    is AnnouncementsScreen.Event.AnnouncementClicked -> {
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
                }
            }
        }

        @CircuitInject(AnnouncementsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): AnnouncementsPresenter
        }
    }

/**
 * UI content for AnnouncementsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AnnouncementsScreen::class, AppScope::class)
@Composable
fun AnnouncementsContent(
    state: AnnouncementsScreen.State,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
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
        },
        floatingActionButton = {
            if (state.unreadCount > 0) {
                FloatingActionButton(
                    onClick = { state.eventSink(AnnouncementsScreen.Event.MarkAllAsReadClicked) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check_24dp_e8eaed_fill1_wght400_grad0_opsz24),
                        contentDescription = "Mark all as read",
                    )
                }
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { state.eventSink(AnnouncementsScreen.Event.RefreshClicked) },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    LoadingState()
                }

                state.announcements.isEmpty() -> {
                    EmptyState(filter = state.currentFilter)
                }

                else -> {
                    AnnouncementsList(
                        announcements = state.announcements,
                        currentFilter = state.currentFilter,
                        onFilterChanged = { filter ->
                            state.eventSink(AnnouncementsScreen.Event.FilterChanged(filter))
                        },
                        onAnnouncementClick = { announcement ->
                            state.eventSink(AnnouncementsScreen.Event.AnnouncementClicked(announcement))
                            openInCustomTab(context, announcement.link)
                        },
                        onToggleReadStatus = { announcement ->
                            state.eventSink(AnnouncementsScreen.Event.ToggleReadStatus(announcement))
                        },
                    )
                }
            }
        }
    }
}

/**
 * Loading state composable.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Empty state composable.
 */
@Composable
private fun EmptyState(
    filter: AnnouncementFilter,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    when (filter) {
                        AnnouncementFilter.ALL -> "No announcements"
                        AnnouncementFilter.UNREAD -> "No unread announcements"
                        AnnouncementFilter.READ -> "No read announcements"
                    },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Announcements list with filtering and grouping.
 */
@Composable
private fun AnnouncementsList(
    announcements: List<AnnouncementEntity>,
    currentFilter: AnnouncementFilter,
    onFilterChanged: (AnnouncementFilter) -> Unit,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    onToggleReadStatus: (AnnouncementEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Filter chips
        item(key = "filters") {
            FilterChipsRow(
                currentFilter = currentFilter,
                onFilterChanged = onFilterChanged,
            )
        }

        // Group announcements by date
        val groupedAnnouncements = groupAnnouncementsByDate(announcements)

        groupedAnnouncements.forEach { (dateGroup, groupAnnouncements) ->
            // Date header
            item(key = "header_${dateGroup.name}") {
                Text(
                    text = dateGroup.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            // Announcements in this group
            items(
                items = groupAnnouncements,
                key = { announcement -> announcement.id },
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

/**
 * Filter chips row.
 */
@Composable
private fun FilterChipsRow(
    currentFilter: AnnouncementFilter,
    onFilterChanged: (AnnouncementFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnnouncementFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = {
                    Text(
                        text =
                            when (filter) {
                                AnnouncementFilter.ALL -> "All"
                                AnnouncementFilter.UNREAD -> "Unread"
                                AnnouncementFilter.READ -> "Read"
                            },
                    )
                },
            )
        }
    }
}

/**
 * Individual announcement item with swipe to toggle read status.
 */
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
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (announcement.isRead) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                ),
        ) {
            ListItem(
                headlineContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = announcement.title,
                            style = MaterialTheme.typography.titleMedium,
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
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = announcement.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatRelativeDate(announcement.publishedDate),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (announcement.isRead) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                },
                        )
                    }
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor =
                            if (announcement.isRead) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                        headlineColor =
                            if (announcement.isRead) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                        supportingColor =
                            if (announcement.isRead) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                    ),
            )
        }
    }
}

/**
 * Group announcements by date.
 */
private fun groupAnnouncementsByDate(announcements: List<AnnouncementEntity>): Map<DateGroup, List<AnnouncementEntity>> {
    val now = Clock.System.now()
    val today = now
    val yesterday = now - 1.days
    val weekAgo = now - 7.days

    return announcements
        .groupBy { announcement ->
            when {
                isSameDay(announcement.publishedDate, today) -> DateGroup.TODAY
                isSameDay(announcement.publishedDate, yesterday) -> DateGroup.YESTERDAY
                announcement.publishedDate > weekAgo -> DateGroup.THIS_WEEK
                else -> DateGroup.OLDER
            }
        }.toSortedMap(compareBy { it.ordinal })
}

/**
 * Check if two instants are on the same day.
 */
private fun isSameDay(
    instant1: Instant,
    instant2: Instant,
): Boolean {
    val diff = instant2 - instant1
    return diff.inWholeDays == 0L && diff.inWholeSeconds >= 0
}

/**
 * Display name for date group.
 */
private fun DateGroup.displayName(): String =
    when (this) {
        DateGroup.TODAY -> "Today"
        DateGroup.YESTERDAY -> "Yesterday"
        DateGroup.THIS_WEEK -> "This Week"
        DateGroup.OLDER -> "Older"
    }

/**
 * Format date as relative time.
 */
private fun formatRelativeDate(instant: Instant): String {
    val now = Clock.System.now()
    val diff = now - instant

    return when {
        diff.inWholeMinutes < 1 -> "Just now"
        diff.inWholeMinutes < 60 -> "${diff.inWholeMinutes} minutes ago"
        diff.inWholeHours < 24 -> "${diff.inWholeHours} hours ago"
        diff.inWholeDays < 7 -> "${diff.inWholeDays} days ago"
        diff.inWholeDays < 30 -> "${diff.inWholeDays / 7} weeks ago"
        diff.inWholeDays < 365 -> "${diff.inWholeDays / 30} months ago"
        else -> "${diff.inWholeDays / 365} years ago"
    }
}

/**
 * Open URL in Chrome Custom Tabs.
 */
private fun openInCustomTab(
    context: Context,
    url: String,
) {
    try {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, url.toUri())
    } catch (e: Exception) {
        Timber.e(e, "Failed to open URL in Custom Tab: $url")
    }
}

// ========== Previews ==========

private val sampleAnnouncements =
    listOf(
        AnnouncementEntity(
            id = "1",
            title = "New Feature Available",
            summary = "We're excited to announce a new feature that will improve your experience with TRMNL displays.",
            link = "https://usetrmnl.com/posts/new-feature",
            publishedDate = Clock.System.now() - 2.days,
            isRead = false,
            fetchedAt = Clock.System.now(),
        ),
        AnnouncementEntity(
            id = "2",
            title = "Platform Update",
            summary = "Important platform maintenance scheduled for this weekend. Please save your work.",
            link = "https://usetrmnl.com/posts/update",
            publishedDate = Clock.System.now() - 5.days,
            isRead = true,
            fetchedAt = Clock.System.now(),
        ),
        AnnouncementEntity(
            id = "3",
            title = "Community Spotlight",
            summary = "Check out this amazing project created by our community member!",
            link = "https://usetrmnl.com/posts/spotlight",
            publishedDate = Clock.System.now() - 7.days,
            isRead = false,
            fetchedAt = Clock.System.now(),
        ),
    )

@PreviewLightDark
@Composable
private fun AnnouncementsContentPreview() {
    TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    announcements = sampleAnnouncements,
                    isLoading = false,
                    currentFilter = AnnouncementFilter.ALL,
                    unreadCount = 2,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AnnouncementsContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = true,
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun AnnouncementsContentEmptyPreview() {
    TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    announcements = emptyList(),
                    isLoading = false,
                ),
        )
    }
}
