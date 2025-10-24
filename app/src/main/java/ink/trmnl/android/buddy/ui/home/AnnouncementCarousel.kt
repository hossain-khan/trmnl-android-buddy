package ink.trmnl.android.buddy.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Announcement carousel displaying latest announcements from TRMNL.
 *
 * Features:
 * - Auto-rotation every 5 seconds
 * - Manual swipe gestures
 * - Page indicators
 * - Unread announcement badges
 * - Click to open in browser
 * - Loading and empty states
 *
 * @param announcements List of announcements to display (max 3)
 * @param isLoading Whether announcements are being loaded
 * @param onAnnouncementClick Callback when announcement is clicked
 * @param modifier Optional modifier for the carousel
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementCarousel(
    announcements: List<AnnouncementEntity>,
    isLoading: Boolean,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            announcements.isEmpty() -> {
                // Empty state
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No announcements available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                // Display announcements carousel
                val pagerState = rememberPagerState(pageCount = { announcements.size })

                // Auto-rotation every 5 seconds
                LaunchedEffect(pagerState, announcements.size) {
                    while (true) {
                        delay(5000) // 5 seconds
                        val nextPage = (pagerState.currentPage + 1) % announcements.size
                        pagerState.animateScrollToPage(nextPage)
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 8.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        val announcement = announcements[page]
                        AnnouncementCard(
                            announcement = announcement,
                            onClick = { onAnnouncementClick(announcement) },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .animatePageAlpha(pagerState, page),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Page indicators
                    PageIndicators(
                        pageCount = announcements.size,
                        currentPage = pagerState.currentPage,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

/**
 * Individual announcement card displayed in the carousel.
 */
@Composable
private fun AnnouncementCard(
    announcement: AnnouncementEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            // Title and unread badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (!announcement.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    UnreadBadge()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary
            Text(
                text = announcement.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Published date
            Text(
                text = formatRelativeDate(announcement.publishedDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * Unread badge indicator.
 */
@Composable
private fun UnreadBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(8.dp),
    ) {}
}

/**
 * Page indicators showing current position in carousel.
 */
@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Surface(
                shape = CircleShape,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                modifier = Modifier.size(8.dp),
            ) {}
        }
    }
}

/**
 * Animate page alpha for fade effect during transitions.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.animatePageAlpha(
    pagerState: PagerState,
    page: Int,
): Modifier {
    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val alpha by animateFloatAsState(
        targetValue = if (pageOffset.toFloat() == 0f) 1f else 0.6f,
        animationSpec = tween(durationMillis = 300),
        label = "pageAlpha",
    )
    return this.alpha(alpha)
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
        days > 7 -> {
            // Show actual date if more than a week old
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        }
        days > 0 -> "$days day${if (days == 1L) "" else "s"} ago"
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        minutes > 0 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        else -> "Just now"
    }
}

// Preview
@PreviewLightDark
@Composable
private fun AnnouncementCarouselPreview() {
    val sampleAnnouncements =
        listOf(
            AnnouncementEntity(
                id = "1",
                title = "New Feature: Custom Refresh Rates",
                summary = "You can now set custom refresh rates for your TRMNL devices. Check out the settings page!",
                link = "https://usetrmnl.com/posts/custom-refresh-rates",
                publishedDate = Instant.now().minus(2, ChronoUnit.DAYS),
                isRead = false,
                fetchedAt = Instant.now(),
            ),
            AnnouncementEntity(
                id = "2",
                title = "Platform Updates",
                summary = "We've made several improvements to the platform including better battery management.",
                link = "https://usetrmnl.com/posts/platform-updates",
                publishedDate = Instant.now().minus(5, ChronoUnit.DAYS),
                isRead = true,
                fetchedAt = Instant.now(),
            ),
            AnnouncementEntity(
                id = "3",
                title = "Welcome to TRMNL Buddy",
                summary = "Get started with your new TRMNL companion app. Manage devices, track battery, and more.",
                link = "https://usetrmnl.com/posts/welcome",
                publishedDate = Instant.now().minus(7, ChronoUnit.DAYS),
                isRead = true,
                fetchedAt = Instant.now(),
            ),
        )

    TrmnlBuddyAppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                // With announcements
                AnnouncementCarousel(
                    announcements = sampleAnnouncements,
                    isLoading = false,
                    onAnnouncementClick = {},
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loading state
                AnnouncementCarousel(
                    announcements = emptyList(),
                    isLoading = true,
                    onAnnouncementClick = {},
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Empty state
                AnnouncementCarousel(
                    announcements = emptyList(),
                    isLoading = false,
                    onAnnouncementClick = {},
                )
            }
        }
    }
}
