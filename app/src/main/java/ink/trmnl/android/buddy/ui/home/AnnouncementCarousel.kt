package ink.trmnl.android.buddy.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Announcement carousel state for UI.
 */
data class AnnouncementCarouselState(
    val announcements: List<AnnouncementEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Announcement carousel composable.
 * Displays the latest 3 announcements in a horizontal pager with auto-rotation.
 *
 * Features:
 * - Auto-rotation every 5 seconds
 * - Manual swipe gestures
 * - Page indicators (dots)
 * - Unread badge
 * - Loading and empty states
 * - Click to open in browser
 */
@Composable
fun AnnouncementCarousel(
    state: AnnouncementCarouselState,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> {
            LoadingState(modifier = modifier)
        }

        state.error != null -> {
            // Silently fail - don't show error for announcements
        }

        state.announcements.isEmpty() -> {
            EmptyState(modifier = modifier)
        }

        else -> {
            CarouselContent(
                announcements = state.announcements.take(3), // Show only top 3
                onAnnouncementClick = onAnnouncementClick,
                modifier = modifier,
            )
        }
    }
}

/**
 * Carousel content with horizontal pager and auto-rotation.
 */
@Composable
private fun CarouselContent(
    announcements: List<AnnouncementEntity>,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { announcements.size })
    val coroutineScope = rememberCoroutineScope()
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-rotation every 5 seconds when not interacting
    LaunchedEffect(pagerState.currentPage, isUserInteracting) {
        if (!isUserInteracting && announcements.size > 1) {
            delay(5.seconds)
            val nextPage = (pagerState.currentPage + 1) % announcements.size
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    // Track user interaction
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isUserInteracting = true
            delay(10.seconds) // Reset interaction flag after 10 seconds
            isUserInteracting = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 8.dp,
        ) { page ->
            AnnouncementCard(
                announcement = announcements[page],
                onClick = { onAnnouncementClick(announcements[page]) },
            )
        }

        // Page indicators
        PageIndicators(
            pagerState = pagerState,
            pageCount = announcements.size,
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp),
        )
    }
}

/**
 * Individual announcement card.
 */
@Composable
private fun AnnouncementCard(
    announcement: AnnouncementEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Title with unread indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (announcement.isRead) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
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

            // Summary
            Text(
                text = announcement.summary,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (announcement.isRead) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            // Date
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
    }
}

/**
 * Page indicators (dots) for the carousel.
 */
@Composable
private fun PageIndicators(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            Surface(
                shape = CircleShape,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    },
                modifier =
                    Modifier.size(
                        if (isSelected) 8.dp else 6.dp,
                    ),
            ) {}
        }
    }
}

/**
 * Loading state for carousel.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(120.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Empty state for carousel.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Card(
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = "No announcements",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "No announcements available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Format date as relative time (e.g., "2 days ago").
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

// ========== Previews ==========

private val sampleAnnouncements =
    listOf(
        AnnouncementEntity(
            id = "1",
            title = "New Feature Available",
            summary = "We're excited to announce a new feature that will improve your experience.",
            link = "https://usetrmnl.com/posts/new-feature",
            publishedDate = Clock.System.now() - 2.seconds * 86400, // 2 days ago
            isRead = false,
            fetchedAt = Clock.System.now(),
        ),
        AnnouncementEntity(
            id = "2",
            title = "Platform Update",
            summary = "Important platform maintenance scheduled for this weekend.",
            link = "https://usetrmnl.com/posts/update",
            publishedDate = Clock.System.now() - 5.seconds * 86400, // 5 days ago
            isRead = true,
            fetchedAt = Clock.System.now(),
        ),
        AnnouncementEntity(
            id = "3",
            title = "Community Spotlight",
            summary = "Check out this amazing project created by our community member!",
            link = "https://usetrmnl.com/posts/spotlight",
            publishedDate = Clock.System.now() - 7.seconds * 86400, // 7 days ago
            isRead = false,
            fetchedAt = Clock.System.now(),
        ),
    )

@PreviewLightDark
@Composable
private fun AnnouncementCarouselPreview() {
    TrmnlBuddyAppTheme {
        Surface {
            AnnouncementCarousel(
                state =
                    AnnouncementCarouselState(
                        announcements = sampleAnnouncements,
                        isLoading = false,
                    ),
                onAnnouncementClick = {},
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AnnouncementCarouselLoadingPreview() {
    TrmnlBuddyAppTheme {
        Surface {
            AnnouncementCarousel(
                state =
                    AnnouncementCarouselState(
                        isLoading = true,
                    ),
                onAnnouncementClick = {},
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun AnnouncementCarouselEmptyPreview() {
    TrmnlBuddyAppTheme {
        Surface {
            AnnouncementCarousel(
                state =
                    AnnouncementCarouselState(
                        announcements = emptyList(),
                        isLoading = false,
                    ),
                onAnnouncementClick = {},
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
