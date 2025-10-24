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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Content carousel displaying latest content (announcements and blog posts) from TRMNL.
 *
 * Features:
 * - Auto-rotation every 5 seconds
 * - Manual swipe gestures
 * - Page indicators
 * - Unread content badges
 * - Content type indicators (Announcement vs Blog Post)
 * - Click to open in browser
 * - Loading and empty states
 * - View all button to navigate to full content hub screen
 *
 * @param contentItems List of content items to display (max 3, combined announcements and blog posts)
 * @param isLoading Whether content is being loaded
 * @param onContentClick Callback when content item is clicked
 * @param onViewAllClick Callback when "View All" button is clicked
 * @param modifier Optional modifier for the carousel
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContentCarousel(
    contentItems: List<ContentItem>,
    isLoading: Boolean,
    onContentClick: (ContentItem) -> Unit,
    onViewAllClick: () -> Unit,
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with "View All" button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Announcements & Blog Posts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TextButton(onClick = onViewAllClick) {
                    Text(text = "View All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = "View all content",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

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

                contentItems.isEmpty() -> {
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
                            text = "No content available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    // Display content carousel
                    val pagerState = rememberPagerState(pageCount = { contentItems.size })

                    // Auto-rotation every 5 seconds
                    LaunchedEffect(pagerState, contentItems.size) {
                        while (true) {
                            delay(5000) // 5 seconds
                            val nextPage = (pagerState.currentPage + 1) % contentItems.size
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
                            val contentItem = contentItems[page]
                            ContentCard(
                                contentItem = contentItem,
                                onClick = { onContentClick(contentItem) },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .animatePageAlpha(pagerState, page),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Page indicators
                        PageIndicators(
                            pageCount = contentItems.size,
                            currentPage = pagerState.currentPage,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        } // Close outer Column
    } // Close Card
}

/**
 * Individual content card displayed in the carousel.
 * Shows different layouts for announcements vs blog posts.
 */
@Composable
private fun ContentCard(
    contentItem: ContentItem,
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
            // Type indicator chip and unread badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Content type chip
                ContentTypeChip(contentItem = contentItem)

                if (!contentItem.isRead) {
                    UnreadBadge()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = contentItem.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Summary
            Text(
                text = contentItem.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Published date and author (for blog posts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeDate(contentItem.publishedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )

                // Show author for blog posts
                if (contentItem is ContentItem.BlogPost) {
                    Text(
                        text = "by ${contentItem.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/**
 * Content type indicator chip.
 */
@Composable
private fun ContentTypeChip(
    contentItem: ContentItem,
    modifier: Modifier = Modifier,
) {
    val (label, containerColor, contentColor) =
        when (contentItem) {
            is ContentItem.Announcement ->
                Triple(
                    "Announcement",
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            is ContentItem.BlogPost ->
                Triple(
                    "Blog",
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                )
        }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
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

// Backward compatibility wrapper for existing code

/**
 * Announcement carousel displaying latest announcements from TRMNL.
 * This is a backward-compatible wrapper around ContentCarousel.
 *
 * @deprecated Use ContentCarousel with ContentItem for combined announcements and blog posts
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnnouncementCarousel(
    announcements: List<AnnouncementEntity>,
    isLoading: Boolean,
    onAnnouncementClick: (AnnouncementEntity) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentItems = announcements.map { ContentItem.Announcement(it) }
    ContentCarousel(
        contentItems = contentItems,
        isLoading = isLoading,
        onContentClick = { contentItem ->
            if (contentItem is ContentItem.Announcement) {
                onAnnouncementClick(contentItem.entity)
            }
        },
        onViewAllClick = onViewAllClick,
        modifier = modifier,
    )
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
private fun ContentCarouselPreview() {
    val sampleContentItems =
        listOf(
            ContentItem.Announcement(
                AnnouncementEntity(
                    id = "1",
                    title = "New Feature: Custom Refresh Rates",
                    summary = "You can now set custom refresh rates for your TRMNL devices. Check out the settings page!",
                    link = "https://usetrmnl.com/posts/custom-refresh-rates",
                    publishedDate = Instant.now().minus(2, ChronoUnit.DAYS),
                    isRead = false,
                    fetchedAt = Instant.now(),
                ),
            ),
            ContentItem.BlogPost(
                BlogPostEntity(
                    id = "2",
                    title = "No more flicker",
                    summary = "We've made several improvements to the display rendering to eliminate flicker during updates.",
                    link = "https://usetrmnl.com/blog/no-more-flicker",
                    authorName = "Ryan Kulp",
                    category = "trmnl",
                    publishedDate = Instant.now().minus(5, ChronoUnit.DAYS),
                    featuredImageUrl = null,
                    isRead = true,
                    fetchedAt = Instant.now(),
                ),
            ),
            ContentItem.Announcement(
                AnnouncementEntity(
                    id = "3",
                    title = "Welcome to TRMNL Buddy",
                    summary = "Get started with your new TRMNL companion app. Manage devices, track battery, and more.",
                    link = "https://usetrmnl.com/posts/welcome",
                    publishedDate = Instant.now().minus(7, ChronoUnit.DAYS),
                    isRead = true,
                    fetchedAt = Instant.now(),
                ),
            ),
        )

    TrmnlBuddyAppTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                // With content
                ContentCarousel(
                    contentItems = sampleContentItems,
                    isLoading = false,
                    onContentClick = {},
                    onViewAllClick = {},
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Loading state
                ContentCarousel(
                    contentItems = emptyList(),
                    isLoading = true,
                    onContentClick = {},
                    onViewAllClick = {},
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Empty state
                ContentCarousel(
                    contentItems = emptyList(),
                    isLoading = false,
                    onContentClick = {},
                    onViewAllClick = {},
                )
            }
        }
    }
}
