package ink.trmnl.android.buddy.ui.bookmarkedrecipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.data.BookmarkRepository
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Presenter for BookmarkedRecipesScreen.
 *
 * Manages state for bookmarked recipes display including:
 * - Loading bookmarked recipes from repository
 * - Handling bookmark removal
 * - Navigation
 */
@Inject
class BookmarkedRecipesPresenter(
    @Assisted private val navigator: Navigator,
    private val bookmarkRepository: BookmarkRepository,
) : Presenter<BookmarkedRecipesScreen.State> {
    @Composable
    override fun present(): BookmarkedRecipesScreen.State {
        // Collect bookmarked recipes as state
        val bookmarkedRecipes by bookmarkRepository.getAllBookmarks().collectAsState(initial = emptyList())
        var showClearAllDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        val coroutineScope = rememberCoroutineScope()

        return BookmarkedRecipesScreen.State(
            bookmarkedRecipes = bookmarkedRecipes,
            isLoading = false,
            error = null,
            showClearAllDialog = showClearAllDialog,
        ) { event ->
            when (event) {
                BookmarkedRecipesScreen.Event.BackClicked -> {
                    navigator.pop()
                }

                BookmarkedRecipesScreen.Event.ClearAllClicked -> {
                    showClearAllDialog = true
                }

                BookmarkedRecipesScreen.Event.ConfirmClearAll -> {
                    showClearAllDialog = false
                    coroutineScope.launch {
                        try {
                            bookmarkRepository.clearAllBookmarks()
                            Timber.d("All bookmarks cleared")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to clear all bookmarks")
                        }
                    }
                }

                BookmarkedRecipesScreen.Event.DismissClearAllDialog -> {
                    showClearAllDialog = false
                }

                is BookmarkedRecipesScreen.Event.RecipeClicked -> {
                    // For now, just log. Navigation to detail screen can be added later.
                    Timber.d("Recipe clicked: ${event.recipe.name} (ID: ${event.recipe.id})")
                }

                is BookmarkedRecipesScreen.Event.BookmarkClicked -> {
                    coroutineScope.launch {
                        try {
                            bookmarkRepository.toggleBookmark(event.recipe)
                            Timber.d("Bookmark removed for recipe: ${event.recipe.name} (ID: ${event.recipe.id})")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to remove bookmark for recipe: ${event.recipe.name}")
                        }
                    }
                }
            }
        }
    }

    @CircuitInject(BookmarkedRecipesScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): BookmarkedRecipesPresenter
    }
}
