package ink.trmnl.android.buddy.ui.bookmarkedrecipes

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.api.models.Recipe
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying user's bookmarked recipes.
 *
 * This screen shows all recipes that the user has bookmarked for quick access.
 * Users can view bookmarked recipes, navigate to recipe details, or remove bookmarks.
 */
@Parcelize
data object BookmarkedRecipesScreen : Screen {
    /**
     * UI state for the Bookmarked Recipes Screen.
     *
     * @property bookmarkedRecipes List of bookmarked recipes
     * @property isLoading True when loading bookmarks
     * @property error Error message to display, or null if no error
     * @property showClearAllDialog True when showing the clear all confirmation dialog
     * @property eventSink Handler for UI events
     */
    data class State(
        val bookmarkedRecipes: List<Recipe> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val showClearAllDialog: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * UI events for the Bookmarked Recipes Screen.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * User clicked the back button.
         */
        data object BackClicked : Event()

        /**
         * User clicked the clear all bookmarks button.
         */
        data object ClearAllClicked : Event()

        /**
         * User confirmed clearing all bookmarks in the dialog.
         */
        data object ConfirmClearAll : Event()

        /**
         * User dismissed the clear all confirmation dialog.
         */
        data object DismissClearAllDialog : Event()

        /**
         * User clicked on a recipe to view details.
         *
         * @property recipe The clicked recipe
         */
        data class RecipeClicked(
            val recipe: Recipe,
        ) : Event()

        /**
         * User clicked the bookmark button to remove bookmark.
         *
         * @property recipe The recipe to unbookmark
         */
        data class BookmarkClicked(
            val recipe: Recipe,
        ) : Event()
    }
}
