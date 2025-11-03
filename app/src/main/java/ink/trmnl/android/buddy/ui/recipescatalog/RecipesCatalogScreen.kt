package ink.trmnl.android.buddy.ui.recipescatalog

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import ink.trmnl.android.buddy.api.models.Recipe
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying searchable and sortable TRMNL community plugin recipes.
 *
 * This screen allows users to browse, search, and discover community recipes
 * that can be installed on TRMNL devices.
 *
 * **Note**: The recipes API is in alpha testing and may be moved before end of 2025.
 */
@Parcelize
data object RecipesCatalogScreen : Screen {
    /**
     * UI state for the Recipes Catalog Screen.
     *
     * @property recipes List of recipes to display
     * @property bookmarkedRecipeIds Set of IDs of bookmarked recipes
     * @property searchQuery Current search query text
     * @property selectedSort Currently selected sort option
     * @property isLoading True when loading initial recipes
     * @property isLoadingMore True when loading next page of recipes
     * @property error Error message to display, or null if no error
     * @property currentPage Current page number in pagination
     * @property hasMorePages True if more pages are available
     * @property totalRecipes Total number of recipes matching the query
     * @property eventSink Handler for UI events
     */
    data class State(
        val recipes: List<Recipe> = emptyList(),
        val bookmarkedRecipeIds: Set<Int> = emptySet(),
        val searchQuery: String = "",
        val selectedSort: SortOption = SortOption.NEWEST,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val currentPage: Int = 1,
        val hasMorePages: Boolean = false,
        val totalRecipes: Int = 0,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    /**
     * UI events for the Recipes Catalog Screen.
     */
    sealed class Event : CircuitUiEvent {
        /**
         * User clicked the back button.
         */
        data object BackClicked : Event()

        /**
         * User changed the search query text.
         *
         * @property query New search query text
         */
        data class SearchQueryChanged(
            val query: String,
        ) : Event()

        /**
         * User submitted the search (e.g., pressed search button or Enter key).
         */
        data object SearchClicked : Event()

        /**
         * User clicked the clear search button (X icon).
         */
        data object ClearSearchClicked : Event()

        /**
         * User selected a different sort option.
         *
         * @property sort New sort option
         */
        data class SortSelected(
            val sort: SortOption,
        ) : Event()

        /**
         * User clicked on a recipe to view details.
         *
         * @property recipe The clicked recipe
         */
        data class RecipeClicked(
            val recipe: Recipe,
        ) : Event()

        /**
         * User clicked the bookmark button on a recipe.
         *
         * @property recipe The recipe to bookmark/unbookmark
         */
        data class BookmarkClicked(
            val recipe: Recipe,
        ) : Event()

        /**
         * User clicked the "Load More" button to fetch next page.
         */
        data object LoadMoreClicked : Event()

        /**
         * User clicked retry button after an error.
         */
        data object RetryClicked : Event()
    }
}

/**
 * Sort options for recipe catalog.
 *
 * Maps UI display names to API parameter values.
 *
 * @property apiValue The value to send to the API's "sort-by" parameter
 * @property displayName The human-readable name shown in the UI
 */
enum class SortOption(
    val apiValue: String,
    val displayName: String,
) {
    /**
     * Sort by newest recipes first (default).
     */
    NEWEST("newest", "Newest"),

    /**
     * Sort by oldest recipes first.
     */
    OLDEST("oldest", "Oldest"),

    /**
     * Sort by popularity (most popular first).
     */
    POPULARITY("popularity", "Popular"),

    /**
     * Sort by number of installs (most installed first).
     */
    INSTALLS("install", "Most Installed"),

    /**
     * Sort by number of forks (most forked first).
     */
    FORKS("fork", "Most Forked"),
}
