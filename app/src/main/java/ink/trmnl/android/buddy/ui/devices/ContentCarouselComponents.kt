package ink.trmnl.android.buddy.ui.devices

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.content.models.ContentItem
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.delay

/**
 * Content carousel composable.
 * Shows combined feed of announcements and blog posts with post type indicators.
 * Features auto-rotation every 5 seconds that stops permanently once user manually swipes.
 * Includes accessibility improvements and lifecycle awareness.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContentCarousel(
    content: List<ContentItem>,
    isLoading: Boolean,
    onContentClick: (ContentItem) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    var isAppInForeground by remember { mutableStateOf(true) }
    var userIsInteracting by remember { mutableStateOf(false) }
    // Track if user has manually swiped - once true, auto-rotation stops permanently
    var hasUserManuallyPaged by remember { mutableStateOf(false) }

    // Observe lifecycle to pause auto-rotation when app is backgrounded
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                isAppInForeground = event == Lifecycle.Event.ON_RESUME
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Content carousel showing announcements and blog posts"
                },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with "View All" button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Announcements & Blog Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onViewAllClick()
                    },
                    modifier =
                        Modifier.semantics {
                            contentDescription = "View all announcements and blog posts"
                        },
                ) {
                    Text(text = "View All")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = null, // Decorative - text button already has description
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            when {
                isLoading -> {
                    // Loading skeleton with shimmer effect
                    LoadingSkeletonCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                content.isEmpty() -> {
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
                    // Display content carousel with auto-rotation
                    val pagerState = rememberPagerState(pageCount = { content.size })

                    // Detect when user manually swipes (drags) the pager
                    // This stops auto-rotation permanently once user demonstrates awareness of paging
                    // Listens for DragInteraction.Start events which only occur on user-initiated drags
                    LaunchedEffect(pagerState.interactionSource) {
                        pagerState.interactionSource.interactions.collect { interaction ->
                            if (interaction is DragInteraction.Start && !hasUserManuallyPaged) {
                                // User has started dragging the pager - permanently disable auto-rotation
                                hasUserManuallyPaged = true
                            }
                        }
                    }

                    // Auto-rotation every 5 seconds
                    // Stops permanently once user manually swipes (hasUserManuallyPaged = true)
                    // Also pauses when app is backgrounded or user is pressing on content
                    LaunchedEffect(pagerState, content.size, isAppInForeground, userIsInteracting, hasUserManuallyPaged) {
                        // Only auto-rotate if user hasn't manually paged yet
                        while (isAppInForeground && !userIsInteracting && !hasUserManuallyPaged) {
                            delay(5000) // 5 seconds
                            val nextPage = (pagerState.currentPage + 1) % content.size
                            pagerState.animateScrollToPage(
                                page = nextPage,
                                animationSpec = tween(durationMillis = 500),
                            )
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            userIsInteracting = true
                                            tryAwaitRelease()
                                            // Keep paused for a bit after release
                                            delay(2000)
                                            userIsInteracting = false
                                        },
                                    )
                                },
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            pageSpacing = 12.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) { page ->
                            val item = content[page]
                            ContentItemCard(
                                item = item,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onContentClick(item)
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .animatePageScale(pagerState, page),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Page indicators
                        PageIndicators(
                            pageCount = content.size,
                            currentPage = pagerState.currentPage,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .semantics {
                                        contentDescription = "Page ${pagerState.currentPage + 1} of ${content.size}"
                                    },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content item card composable.
 * Shows a single content item (announcement or blog post) with type indicator.
 * Includes accessibility improvements, semantic colors, proper touch targets, and animations.
 */
@Composable
internal fun ContentItemCard(
    item: ContentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate card elevation on press
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardElevation",
    )

    // Animate card color on press
    val containerColor by animateColorAsState(
        targetValue =
            if (isPressed) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = tween(durationMillis = 150),
        label = "cardColor",
    )

    Card(
        modifier =
            modifier
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = null, // Custom ripple via state layers
                ).semantics {
                    contentDescription =
                        buildString {
                            append(
                                when (item) {
                                    is ContentItem.Announcement -> "Announcement: "
                                    is ContentItem.BlogPost -> "Blog post: "
                                },
                            )
                            append(item.title)
                            if (!item.isRead) append(", unread")
                            append(", published ${formatRelativeDate(item.publishedDate)}")
                        }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title, post type chip, and unread badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Post type chip with proper semantics
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color =
                            when (item) {
                                is ContentItem.Announcement -> MaterialTheme.colorScheme.primaryContainer
                                is ContentItem.BlogPost -> MaterialTheme.colorScheme.secondaryContainer
                            },
                        modifier =
                            Modifier.semantics {
                                contentDescription =
                                    when (item) {
                                        is ContentItem.Announcement -> "Type: Announcement"
                                        is ContentItem.BlogPost -> "Type: Blog post"
                                    }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        when (item) {
                                            is ContentItem.Announcement ->
                                                R.drawable
                                                    .campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                            is ContentItem.BlogPost ->
                                                R.drawable
                                                    .newspaper_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                        },
                                    ),
                                contentDescription = null, // Decorative
                                modifier = Modifier.size(14.dp),
                                tint =
                                    when (item) {
                                        is ContentItem.Announcement -> MaterialTheme.colorScheme.onPrimaryContainer
                                        is ContentItem.BlogPost -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                            )
                            Text(
                                text =
                                    when (item) {
                                        is ContentItem.Announcement -> "Announcement"
                                        is ContentItem.BlogPost -> "Blog"
                                    },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color =
                                    when (item) {
                                        is ContentItem.Announcement -> MaterialTheme.colorScheme.onPrimaryContainer
                                        is ContentItem.BlogPost -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                            )
                        }
                    }

                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }

                // Unread badge with minimum touch target
                if (!item.isRead) {
                    Box(
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .size(12.dp)
                                .semantics {
                                    contentDescription = "Unread"
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                        ) {}
                    }
                }
            }

            // Summary
            if (item.summary.isNotEmpty()) {
                Text(
                    text = item.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatRelativeDate(item.publishedDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Blog-specific metadata
                if (item is ContentItem.BlogPost) {
                    item.category?.let { category ->
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Page indicators showing current position in carousel.
 * Uses semantic colors and proper sizing for accessibility.
 */
@Composable
internal fun PageIndicators(
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
            val size by animateFloatAsState(
                targetValue = if (isSelected) 10f else 8f,
                animationSpec = tween(durationMillis = 200),
                label = "indicatorSize",
            )
            Surface(
                shape = CircleShape,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                modifier =
                    Modifier
                        .size(size.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Page ${index + 1}${if (isSelected) " selected" else ""}"
                        },
            ) {}
        }
    }
}

/**
 * Animate page scale for subtle zoom effect during transitions.
 * Pages that are not current are slightly scaled down.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.animatePageScale(
    pagerState: PagerState,
    page: Int,
): Modifier {
    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
    val scale by animateFloatAsState(
        targetValue = if (kotlin.math.abs(pageOffset) < 0.5f) 1f else 0.92f,
        animationSpec = tween(durationMillis = 300),
        label = "pageScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (kotlin.math.abs(pageOffset) < 0.5f) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "pageAlpha",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
}

/**
 * Loading skeleton with shimmer effect.
 * Provides visual feedback while content is loading.
 */
@Composable
internal fun LoadingSkeletonCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmerAlpha",
    )

    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Type chip skeleton
            Box(
                modifier =
                    Modifier
                        .width(100.dp)
                        .height(24.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha)),
            )

            // Title skeleton
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(24.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha)),
            )

            // Summary skeleton
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha),
                            ),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.6f)
                            .height(16.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha),
                            ),
                )
            }

            // Metadata skeleton
            Box(
                modifier =
                    Modifier
                        .width(120.dp)
                        .height(14.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = shimmerAlpha)),
            )
        }
    }
}

// ========== Helper Functions ==========

/**
 * Format Instant as relative time string (e.g., "2 days ago").
 */
private fun formatRelativeDate(instant: java.time.Instant): String {
    val now = java.time.Instant.now()
    val days =
        java.time.temporal.ChronoUnit.DAYS
            .between(instant, now)
    val hours =
        java.time.temporal.ChronoUnit.HOURS
            .between(instant, now)
    val minutes =
        java.time.temporal.ChronoUnit.MINUTES
            .between(instant, now)

    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"} ago"
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
        minutes > 0 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
        else -> "Just now"
    }
}

// ========== Previews ==========

/**
 * Sample content items for previews.
 */
private val sampleAnnouncement =
    ContentItem.Announcement(
        id = "1",
        title = "New Feature: Screen Sharing",
        summary =
            "We've added the ability to share your screen content with others. " +
                "Check out the new settings panel to get started.",
        link = "https://usetrmnl.com/announcements/screen-sharing",
        publishedDate =
            java.time.Instant
                .now()
                .minus(2, java.time.temporal.ChronoUnit.DAYS),
        isRead = false,
    )

private val sampleBlogPost =
    ContentItem.BlogPost(
        id = "2",
        title = "Building the Perfect E-Ink Dashboard",
        summary =
            "Learn how to create an efficient and beautiful dashboard for your TRMNL device. " +
                "We'll cover layout design, data sources, and optimization tips.",
        link = "https://usetrmnl.com/blog/perfect-dashboard",
        publishedDate =
            java.time.Instant
                .now()
                .minus(5, java.time.temporal.ChronoUnit.HOURS),
        isRead = false,
        authorName = "John Doe",
        category = "Tutorial",
        featuredImageUrl = "https://usetrmnl.com/images/blog/dashboard.jpg",
        isFavorite = false,
    )

private val sampleContentList =
    listOf(
        sampleBlogPost, // Recent blog post (5 hours ago)
        sampleAnnouncement, // Older announcement (2 days ago)
    )

@PreviewLightDark
@Composable
private fun ContentItemCardAnnouncementPreview() {
    TrmnlBuddyAppTheme {
        ContentItemCard(
            item = sampleAnnouncement,
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContentItemCardBlogPostPreview() {
    TrmnlBuddyAppTheme {
        ContentItemCard(
            item = sampleBlogPost,
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PageIndicatorsPreview() {
    TrmnlBuddyAppTheme {
        PageIndicators(
            pageCount = 3,
            currentPage = 1,
        )
    }
}

@PreviewLightDark
@Composable
private fun LoadingSkeletonCardPreview() {
    TrmnlBuddyAppTheme {
        LoadingSkeletonCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ContentCarouselPreview() {
    TrmnlBuddyAppTheme {
        ContentCarousel(
            content = sampleContentList,
            isLoading = false,
            onContentClick = {},
            onViewAllClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContentCarouselLoadingPreview() {
    TrmnlBuddyAppTheme {
        ContentCarousel(
            content = emptyList(),
            isLoading = true,
            onContentClick = {},
            onViewAllClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ContentCarouselEmptyPreview() {
    TrmnlBuddyAppTheme {
        ContentCarousel(
            content = emptyList(),
            isLoading = false,
            onContentClick = {},
            onViewAllClick = {},
        )
    }
}
