package ink.trmnl.android.buddy.ui.blogposts

import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
 */
@Parcelize
data object BlogPostsScreen : Screen {
    data class State(
        val blogPosts: List<BlogPostEntity> = emptyList(),
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null,
        val selectedCategory: String? = null, // null = All
        val availableCategories: List<String> = emptyList(),
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
    }
}

/**
 * Presenter for BlogPostsScreen.
 * Manages blog posts data and interactions.
 */
@Inject
class BlogPostsPresenter
    constructor(
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
                }
            }
        }

        @CircuitInject(BlogPostsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): BlogPostsPresenter
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
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
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
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
 */
@Composable
private fun BlogPostCard(
    blogPost: BlogPostEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        // Category chip
                        blogPost.category?.let { category ->
                            AssistChip(
                                onClick = { /* Optional: filter by category */ },
                                label = { Text(text = category, style = MaterialTheme.typography.labelSmall) },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }

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
                                            R.drawable.settings_heart_24dp_e8eaed_fill1_wght400_grad0_opsz24
                                        } else {
                                            R.drawable.settings_24dp_e8eaed_fill0_wght400_grad0_opsz24
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
                    maxLines = 3,
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
 * Loading state composable.
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
 * Empty state composable.
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
