package ink.trmnl.android.buddy.ui.announcements

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.util.BrowserUtils
import ink.trmnl.android.buddy.util.formatRelativeDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * UI content for AnnouncementsScreen.
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@CircuitInject(AnnouncementsScreen::class, AppScope::class)
@Composable
fun AnnouncementsContent(
    state: AnnouncementsScreen.State,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Track FAB visibility based on scroll position
    val fabVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 ||
                listState.firstVisibleItemScrollOffset < 100
        }
    }

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
            AnimatedVisibility(
                visible = state.unreadCount > 0 && fabVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn() + scaleIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut() + scaleOut(),
            ) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { state.eventSink(AnnouncementsScreen.Event.MarkAllAsRead) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.done_all_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Mark all as read",
                        )
                    },
                    text = { Text("Mark All Read") },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter chips fixed at the top - always visible regardless of content state
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                FilterChips(
                    selectedFilter = state.filter,
                    onFilterChanged = { newFilter ->
                        state.eventSink(AnnouncementsScreen.Event.FilterChanged(newFilter))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }

            // Content area below filters
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
                        showAuthBanner = state.showAuthBanner,
                        listState = listState,
                        onRefresh = { state.eventSink(AnnouncementsScreen.Event.Refresh) },
                        onAnnouncementClick = { announcement ->
                            state.eventSink(AnnouncementsScreen.Event.AnnouncementClicked(announcement))
                        },
                        onToggleReadStatus = { announcement ->
                            state.eventSink(AnnouncementsScreen.Event.ToggleReadStatus(announcement))
                        },
                        onDismissBanner = {
                            state.eventSink(AnnouncementsScreen.Event.DismissAuthBanner)
                        },
                    )
                }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter =
                    painterResource(
                        when (filter) {
                            AnnouncementsScreen.Filter.ALL -> R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24
                            AnnouncementsScreen.Filter.UNREAD -> R.drawable.markunread_mailbox_24dp_e8eaed_fill0_wght400_grad0_opsz24
                            AnnouncementsScreen.Filter.READ -> R.drawable.done_all_24dp_e8eaed_fill0_wght400_grad0_opsz24
                        },
                    ),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AnnouncementsList(
    announcements: List<AnnouncementEntity>,
    isRefreshing: Boolean,
    filter: AnnouncementsScreen.Filter,
    showAuthBanner: Boolean,
    listState: LazyListState,
    onRefresh: () -> Unit,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    onToggleReadStatus: (AnnouncementEntity) -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local state for controlling banner visibility during animation
    var isBannerDismissing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Determine if banner should be shown (either not dismissed, or currently animating out)
    val shouldShowBanner = showAuthBanner || isBannerDismissing

    // Scrollable list with pull-to-refresh
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            // Authentication info banner (if not dismissed) with smooth slide-out animation
            if (shouldShowBanner) {
                item(key = "auth_banner") {
                    AuthenticationBanner(
                        onDismiss = {
                            // Start dismissing animation
                            isBannerDismissing = true
                            // Wait for animation, then save preference and clean up state
                            coroutineScope.launch {
                                delay(300) // Match LazyColumn's default animation duration
                                onDismissBanner()
                                isBannerDismissing = false
                            }
                        },
                        modifier =
                            Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .animateItem(), // Use LazyColumn's built-in item animation
                    )
                }
            }

            // Group announcements by date
            val groupedAnnouncements = announcements.groupByDate()

            groupedAnnouncements.forEach { (dateCategory, items) ->
                stickyHeader(key = "date_$dateCategory") {
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
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/**
 * Authentication information banner shown at the top of announcements list.
 * Informs users that they need a TRMNL account to view announcement details.
 */
@Composable
private fun AuthenticationBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_info_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text =
                        "Announcements require authentication to view. " +
                            "You must have a TRMNL account to view announcement details on the web.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
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

    // Track press state for animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate scale when pressed for better feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "Announcement Item Press Scale",
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
            modifier =
                Modifier
                    .clickable(
                        onClick = onClick,
                        interactionSource = interactionSource,
                    ).graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
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

// ============================================================================
// Compose Previews
// ============================================================================

private val sampleAnnouncements =
    listOf(
        AnnouncementEntity(
            id = "1",
            title = "New Feature: Device Scheduling",
            summary = "You can now schedule when your TRMNL device refreshes content automatically.",
            link = "https://trmnl.com/announcements/device-scheduling",
            publishedDate = Instant.now().minus(2, ChronoUnit.HOURS),
            isRead = false,
            fetchedAt = Instant.now().minus(2, ChronoUnit.HOURS),
        ),
        AnnouncementEntity(
            id = "2",
            title = "API Rate Limit Update",
            summary = "We've increased the API rate limit for all paid plans. Check the docs for details.",
            link = "https://trmnl.com/announcements/api-update",
            publishedDate = Instant.now().minus(1, ChronoUnit.DAYS),
            isRead = true,
            fetchedAt = Instant.now().minus(1, ChronoUnit.DAYS),
        ),
        AnnouncementEntity(
            id = "3",
            title = "Maintenance Window Scheduled",
            summary = "Brief maintenance scheduled for Saturday 2am-4am EST. Expect 5min downtime.",
            link = "https://trmnl.com/announcements/maintenance",
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
@Preview(name = "Full Screen - With Auth Banner")
@Composable
private fun AnnouncementsWithAuthBannerPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        AnnouncementsContent(
            state =
                AnnouncementsScreen.State(
                    isLoading = false,
                    announcements = sampleAnnouncements,
                    filter = AnnouncementsScreen.Filter.ALL,
                    unreadCount = 2,
                    showTopBar = true,
                    showAuthBanner = true, // Show the authentication banner
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
