package ink.trmnl.android.buddy.ui.blogposts

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
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
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.util.BrowserUtils
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Blog Posts Screen displaying list of TRMNL blog posts.
 *
 * Features:
 * - List of blog posts with title, summary, author, category, date
 * - Featured images when available
 * - Pull-to-refresh support
 * - Filter by category
 * - Mark as read/favorite
 * - Click to open in Chrome Custom Tabs
 *
 * @param isEmbedded When true, hides the top app bar (for use in ContentHubScreen).
 */
@Parcelize
data class BlogPostsScreen(
    val isEmbedded: Boolean = false,
) : Screen {
    data class State(
        val blogPosts: List<BlogPostEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
        val selectedCategory: String? = null, // null = All
        val availableCategories: List<String> = emptyList(),
        val unreadCount: Int = 0,
        val showTopBar: Boolean = true, // Control whether to show top app bar
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object Refresh : Event()

        data class BlogPostClicked(
            val blogPost: BlogPostEntity,
        ) : Event()

        data class ToggleFavorite(
            val blogPostId: String,
        ) : Event()

        data class CategorySelected(
            val category: String?, // null = All
        ) : Event()

        data object MarkAllAsRead : Event()
    }
}

/**
 * Presenter for BlogPostsScreen.
 * Manages blog posts data and interactions.
 */
@Inject
class BlogPostsPresenter
    constructor(
        @Assisted private val screen: BlogPostsScreen,
        @Assisted private val navigator: Navigator,
        @ApplicationContext private val context: Context,
        private val blogPostRepository: BlogPostRepository,
    ) : Presenter<BlogPostsScreen.State> {
        @Composable
        override fun present(): BlogPostsScreen.State {
            var blogPosts by rememberRetained { mutableStateOf<List<BlogPostEntity>>(emptyList()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var isRefreshing by rememberRetained { mutableStateOf(false) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            var selectedCategory by rememberRetained { mutableStateOf<String?>(null) }
            var availableCategories by rememberRetained { mutableStateOf<List<String>>(emptyList()) }
            var unreadCount by rememberRetained { mutableStateOf(0) }
            val coroutineScope = rememberCoroutineScope()

            // Capture theme colors for Custom Tabs
            val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
            val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

            // Collect blog posts based on selected category
            LaunchedEffect(selectedCategory) {
                val category = selectedCategory // Capture to local variable for smart cast
                val flow =
                    if (category == null) {
                        blogPostRepository.getAllBlogPosts()
                    } else {
                        blogPostRepository.getBlogPostsByCategory(category)
                    }

                flow.collect { posts ->
                    blogPosts = posts
                    isLoading = false

                    // Extract unique categories
                    val categories = posts.mapNotNull { it.category }.distinct().sorted()
                    availableCategories = categories
                }
            }

            // Collect unread count efficiently
            LaunchedEffect(Unit) {
                blogPostRepository.getUnreadCount().collect { count ->
                    unreadCount = count
                }
            }

            // Fetch blog posts on initial load
            LaunchedEffect(Unit) {
                if (blogPosts.isEmpty()) {
                    coroutineScope.launch {
                        val result = blogPostRepository.refreshBlogPosts()
                        if (result.isFailure) {
                            errorMessage = "Failed to fetch blog posts: ${result.exceptionOrNull()?.message}"
                            isLoading = false
                        }
                    }
                }
            }

            return BlogPostsScreen.State(
                blogPosts = blogPosts,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                errorMessage = errorMessage,
                selectedCategory = selectedCategory,
                availableCategories = availableCategories,
                unreadCount = unreadCount,
                showTopBar = !screen.isEmbedded, // Hide top bar when embedded
            ) { event ->
                when (event) {
                    BlogPostsScreen.Event.Refresh -> {
                        isRefreshing = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = blogPostRepository.refreshBlogPosts()
                            if (result.isFailure) {
                                errorMessage = "Failed to refresh: ${result.exceptionOrNull()?.message}"
                            }
                            isRefreshing = false
                        }
                    }

                    is BlogPostsScreen.Event.BlogPostClicked -> {
                        // Open blog post in Chrome Custom Tabs
                        BrowserUtils.openUrlInCustomTab(
                            context = context,
                            url = event.blogPost.link,
                            toolbarColor = primaryColor,
                            secondaryColor = surfaceColor,
                        )
                        // Mark as read
                        coroutineScope.launch {
                            blogPostRepository.markAsRead(event.blogPost.id)
                        }
                    }

                    is BlogPostsScreen.Event.ToggleFavorite -> {
                        coroutineScope.launch {
                            blogPostRepository.toggleFavorite(event.blogPostId)
                        }
                    }

                    is BlogPostsScreen.Event.CategorySelected -> {
                        selectedCategory = event.category
                    }

                    BlogPostsScreen.Event.MarkAllAsRead -> {
                        coroutineScope.launch {
                            blogPostRepository.markAllAsRead()
                        }
                    }
                }
            }
        }

        @CircuitInject(BlogPostsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: BlogPostsScreen,
                navigator: Navigator,
            ): BlogPostsPresenter
        }
    }

/**
 * UI content for BlogPostsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(BlogPostsScreen::class, AppScope::class)
@Composable
fun BlogPostsContent(
    state: BlogPostsScreen.State,
    modifier: Modifier = Modifier,
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Track FAB visibility based on scroll direction
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
                        TrmnlTitle(
                            if (state.selectedCategory != null) {
                                "Blog Posts - ${state.selectedCategory}"
                            } else {
                                "Blog Posts"
                            },
                        )
                    },
                    actions = {
                        // Category filter button
                        Box {
                            IconButton(onClick = { showCategoryMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                    contentDescription = "Filter by category",
                                )
                            }

                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                            ) {
                                // All categories option
                                DropdownMenuItem(
                                    text = { Text("All") },
                                    onClick = {
                                        state.eventSink(BlogPostsScreen.Event.CategorySelected(null))
                                        showCategoryMenu = false
                                    },
                                )

                                // Individual categories
                                state.availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            state.eventSink(BlogPostsScreen.Event.CategorySelected(category))
                                            showCategoryMenu = false
                                        },
                                    )
                                }
                            }
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
                    onClick = { state.eventSink(BlogPostsScreen.Event.MarkAllAsRead) },
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
        when {
            state.isLoading -> {
                LoadingState()
            }

            state.errorMessage != null -> {
                ErrorState(
                    errorMessage = state.errorMessage,
                    onRetry = { state.eventSink(BlogPostsScreen.Event.Refresh) },
                )
            }

            state.blogPosts.isEmpty() -> {
                EmptyState()
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { state.eventSink(BlogPostsScreen.Event.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = state.blogPosts,
                            key = { it.id },
                        ) { blogPost ->
                            BlogPostCard(
                                blogPost = blogPost,
                                onClick = { state.eventSink(BlogPostsScreen.Event.BlogPostClicked(blogPost)) },
                                onToggleFavorite = { state.eventSink(BlogPostsScreen.Event.ToggleFavorite(blogPost.id)) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Blog post card composable.
 * Shows blog post details with featured image, title, summary, metadata.
 * Includes press animation for better user feedback.
 */
@Composable
private fun BlogPostCard(
    blogPost: BlogPostEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track press state for animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animate scale when pressed - Material 3 emphasis pattern
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "Card Press Scale",
    )

    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
            ),
        interactionSource = interactionSource,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Featured image if available
            blogPost.featuredImageUrl?.let { imageUrl ->
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Featured image for ${blogPost.title}",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    },
                    error = {
                        // Silently fail - don't show error for images
                    },
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Title and favorite icon row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Title
                        Text(
                            text = blogPost.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Favorite and unread indicators
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!blogPost.isRead) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(8.dp),
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (blogPost.isFavorite) {
                                            R.drawable.favorite_24dp_e8eaed_fill1_wght400_grad0_opsz24
                                        } else {
                                            R.drawable.heart_plus_24dp_e8eaed_fill0_wght400_grad0_opsz24
                                        },
                                    ),
                                contentDescription = if (blogPost.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint =
                                    if (blogPost.isFavorite) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }

                // Summary
                Text(
                    text = blogPost.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )

                // Metadata row: author and date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = blogPost.authorName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = formatRelativeDate(blogPost.publishedDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

/**
 * Loading state composable with fade-in animation.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text("Loading blog posts...")
        }
    }
}

/**
 * Error state composable.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.OutlinedButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Empty state composable with gentle animations.
 */
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                contentDescription = "No blog posts",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No blog posts found",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "Pull to refresh or check back later",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

private val sampleBlogPosts =
    listOf(
        BlogPostEntity(
            id = "1",
            title = "Introducing TRMNL Plugins: Extend Your Dashboard",
            summary =
                "We're excited to announce our new plugin system that lets you " +
                    "customize your TRMNL experience with community-built integrations.",
            link = "https://usetrmnl.com/blog/introducing-plugins",
            authorName = "Ryan Kulp",
            category = "Product Updates",
            publishedDate = Instant.now().minus(2, ChronoUnit.HOURS),
            featuredImageUrl = "https://images.unsplash.com/photo-1531297484001-80022131f5a1",
            isRead = false,
            isFavorite = false,
            fetchedAt = Instant.now(),
        ),
        BlogPostEntity(
            id = "2",
            title = "5 Creative Ways to Use TRMNL at Home",
            summary = "From kitchen timers to family calendars, discover how TRMNL users are getting creative with their e-ink displays.",
            link = "https://usetrmnl.com/blog/creative-uses",
            authorName = "Mario Lurig",
            category = "Community",
            publishedDate = Instant.now().minus(1, ChronoUnit.DAYS),
            featuredImageUrl = "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b",
            isRead = true,
            isFavorite = true,
            fetchedAt = Instant.now(),
        ),
        BlogPostEntity(
            id = "3",
            title = "Behind the Scenes: Building TRMNL's Hardware",
            summary = "A deep dive into the engineering challenges we faced creating a beautiful, low-power e-ink display.",
            link = "https://usetrmnl.com/blog/hardware-engineering",
            authorName = "Ryan Kulp",
            category = "Engineering",
            publishedDate = Instant.now().minus(3, ChronoUnit.DAYS),
            featuredImageUrl = null, // No featured image
            isRead = false,
            isFavorite = false,
            fetchedAt = Instant.now(),
        ),
    )

@Preview(name = "Loading State")
@Composable
private fun BlogPostsLoadingPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        BlogPostsContent(
            state = BlogPostsScreen.State(isLoading = true),
        )
    }
}

@Preview(name = "Error State")
@Composable
private fun BlogPostsErrorPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        BlogPostsContent(
            state =
                BlogPostsScreen.State(
                    isLoading = false,
                    errorMessage = "Failed to load blog posts. Please check your connection.",
                ),
        )
    }
}

@Preview(name = "Empty State")
@Composable
private fun BlogPostsEmptyPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        BlogPostsContent(
            state =
                BlogPostsScreen.State(
                    isLoading = false,
                    blogPosts = emptyList(),
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Blog Post Card - With Image & Unread")
@Composable
private fun BlogPostCardWithImagePreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            BlogPostCard(
                blogPost = sampleBlogPosts[0],
                onClick = {},
                onToggleFavorite = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Blog Post Card - Read & Favorited")
@Composable
private fun BlogPostCardFavoritedPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            BlogPostCard(
                blogPost = sampleBlogPosts[1],
                onClick = {},
                onToggleFavorite = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Blog Post Card - No Image")
@Composable
private fun BlogPostCardNoImagePreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        Surface {
            BlogPostCard(
                blogPost = sampleBlogPosts[2],
                onClick = {},
                onToggleFavorite = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Full Screen - With Blog Posts")
@Composable
private fun BlogPostsFullScreenPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        BlogPostsContent(
            state =
                BlogPostsScreen.State(
                    isLoading = false,
                    blogPosts = sampleBlogPosts,
                    availableCategories = listOf("Product Updates", "Community", "Engineering"),
                    selectedCategory = null,
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Full Screen - Filtered by Category")
@Composable
private fun BlogPostsFilteredPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        BlogPostsContent(
            state =
                BlogPostsScreen.State(
                    isLoading = false,
                    blogPosts = listOf(sampleBlogPosts[0]),
                    availableCategories = listOf("Product Updates", "Community", "Engineering"),
                    selectedCategory = "Product Updates",
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Full Screen - Embedded (No TopBar)")
@Composable
private fun BlogPostsEmbeddedPreview() {
    ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme {
        BlogPostsContent(
            state =
                BlogPostsScreen.State(
                    isLoading = false,
                    blogPosts = sampleBlogPosts,
                    availableCategories = listOf("Product Updates", "Community", "Engineering"),
                    selectedCategory = null,
                    showTopBar = false, // Embedded mode
                ),
        )
    }
}
