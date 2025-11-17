package ink.trmnl.android.buddy.ui.blogposts

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import kotlinx.parcelize.Parcelize

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
