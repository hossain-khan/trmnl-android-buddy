package ink.trmnl.android.buddy.ui.bookmarkedrecipes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
        // Get context for clipboard and sharing
        val context = LocalContext.current

        // Collect bookmarked recipes as state
        val bookmarkedRecipes by bookmarkRepository.getAllBookmarks().collectAsState(initial = emptyList())
        var showClearAllDialog by remember { mutableStateOf(false) }

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

                BookmarkedRecipesScreen.Event.ShareClicked -> {
                    if (bookmarkedRecipes.isNotEmpty()) {
                        val recipeList = bookmarkedRecipes.joinToString(separator = "\n") { "• ${it.name}" }
                        val shareText = "My Bookmarked TRMNL Recipes:\n\n$recipeList"

                        // Copy to clipboard
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Bookmarked Recipes", shareText)
                        clipboard.setPrimaryClip(clip)
                        Timber.d("Bookmarked recipes copied to clipboard")

                        // Open share sheet
                        val shareIntent =
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_SUBJECT, "My Bookmarked TRMNL Recipes")
                            }
                        val chooser = Intent.createChooser(shareIntent, "Share Bookmarked Recipes")
                        context.startActivity(chooser)
                        Timber.d("Share sheet opened with ${bookmarkedRecipes.size} recipes")
                    } else {
                        Timber.d("No bookmarked recipes to share")
                    }
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
