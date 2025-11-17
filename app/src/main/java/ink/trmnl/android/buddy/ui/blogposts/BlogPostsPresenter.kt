package ink.trmnl.android.buddy.ui.blogposts

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.util.BrowserUtils
import kotlinx.coroutines.launch

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
