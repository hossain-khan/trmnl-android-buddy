package ink.trmnl.android.buddy.ui.recipescatalog

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.data.RecipesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for RecipesCatalogScreen presenter.
 *
 * Verifies:
 * - Initial loading state and data fetch
 * - Search functionality with debouncing
 * - Sort option selection (all 5 options)
 * - Pagination with load more
 * - Error handling and retry
 * - Navigation
 */
class RecipesCatalogPresenterTest {
    @Test
    fun `presenter loads recipes on initial composition`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(2),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait until we get recipes loaded (skip initial and loading states)
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty() && loadedState.error == null)

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.recipes).hasSize(2)
                assertThat(loadedState.error).isNull()
                assertThat(loadedState.totalRecipes).isEqualTo(100)
                assertThat(loadedState.hasMorePages).isTrue()
            }
        }

    @Test
    fun `search query triggers debounced API call`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial load to complete
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Send search query
                loadedState.eventSink(RecipesCatalogScreen.Event.SearchQueryChanged("weather"))

                // Should update search query immediately
                val searchingState = awaitItem()
                assertThat(searchingState.searchQuery).isEqualTo("weather")

                // Wait for debounce (500ms) + extra time for API call
                delay(700)

                // Should have search results
                var searchedState = loadedState
                do {
                    searchedState = awaitItem()
                } while (searchedState.isLoading)

                assertThat(repository.lastSearchQuery).isEqualTo("weather")
            }
        }

    @Test
    fun `clear search resets query and fetches all recipes`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(2),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Clear search
                loadedState.eventSink(RecipesCatalogScreen.Event.ClearSearchClicked)

                // Should clear query immediately
                val clearedState = awaitItem()
                assertThat(clearedState.searchQuery).isEqualTo("")

                // Wait for results
                delay(100)
                var resultsState = clearedState
                do {
                    resultsState = awaitItem()
                } while (resultsState.isLoading)

                // Should fetch all recipes (null search)
                assertThat(repository.lastSearchQuery).isNull()
            }
        }

    @Test
    fun `selecting sort option fetches recipes with new sort`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(2),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                assertThat(loadedState.selectedSort).isEqualTo(SortOption.NEWEST)

                // Select popularity sort
                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.POPULARITY))

                // Wait for sorted results
                delay(100)
                var sortedState = loadedState
                do {
                    sortedState = awaitItem()
                } while (sortedState.isLoading)

                assertThat(sortedState.selectedSort).isEqualTo(SortOption.POPULARITY)
                assertThat(repository.lastSortBy).isEqualTo("popularity")
            }
        }

    @Test
    fun `load more appends next page of recipes`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(2, currentPage = 1),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                val state = awaitItem()
                assertThat(state.recipes).hasSize(2)
                assertThat(state.currentPage).isEqualTo(1)
                assertThat(state.hasMorePages).isTrue()

                // Load more
                state.eventSink(RecipesCatalogScreen.Event.LoadMoreClicked)

                // Update repository response for page 2
                repository.recipesResponse = createSampleRecipesResponse(2, currentPage = 2, hasNext = false)

                // Wait for load more to complete
                delay(100)
                var loadedMoreState = state
                do {
                    loadedMoreState = awaitItem()
                } while (loadedMoreState.isLoadingMore)

                assertThat(loadedMoreState.recipes).hasSize(4) // 2 + 2
                assertThat(loadedMoreState.currentPage).isEqualTo(2)
                assertThat(loadedMoreState.hasMorePages).isFalse()
            }
        }

    @Test
    fun `error state shows error message and allows retry`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    shouldFail = true,
                    errorMessage = "Network error",
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for error state (initial load fails)
                var errorState: RecipesCatalogScreen.State
                do {
                    errorState = awaitItem()
                } while (errorState.error == null)

                assertThat(errorState.error).isEqualTo("Network error")
                assertThat(errorState.recipes).isEmpty()

                // Retry
                repository.shouldFail = false
                repository.recipesResponse = createSampleRecipesResponse(1)
                errorState.eventSink(RecipesCatalogScreen.Event.RetryClicked)

                // Wait for retry to complete
                delay(100)
                var retriedState = errorState
                do {
                    retriedState = awaitItem()
                } while (retriedState.isLoading || retriedState.error != null)

                assertThat(retriedState.error).isNull()
                assertThat(retriedState.recipes).hasSize(1)
            }
        }

    @Test
    fun `back clicked navigates back`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                loadedState.eventSink(RecipesCatalogScreen.Event.BackClicked)

                // Verify navigation
                assertThat(navigator.awaitPop()).isEqualTo(RecipesCatalogScreen)
            }
        }

    @Test
    fun `all sort options work correctly`() =
        runTest {
            // Given
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val presenter = RecipesCatalogPresenter(navigator, repository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Test each sort option
                val sortOptions =
                    listOf(
                        SortOption.NEWEST to "newest",
                        SortOption.OLDEST to "oldest",
                        SortOption.POPULARITY to "popularity",
                        SortOption.INSTALLS to "install",
                        SortOption.FORKS to "fork",
                    )

                for ((sortOption, expectedApiValue) in sortOptions) {
                    loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(sortOption))

                    // Wait for sort to complete
                    delay(100)
                    var sortedState = loadedState
                    do {
                        sortedState = awaitItem()
                    } while (sortedState.isLoading)

                    assertThat(sortedState.selectedSort).isEqualTo(sortOption)
                    assertThat(repository.lastSortBy).isEqualTo(expectedApiValue)
                    loadedState = sortedState
                }
            }
        }
}

/**
 * Fake implementation of RecipesRepository for testing.
 */
private class FakeRecipesRepository(
    var recipesResponse: RecipesResponse = createSampleRecipesResponse(0),
    var shouldFail: Boolean = false,
    var errorMessage: String = "Error fetching recipes",
) : RecipesRepository {
    var lastSearchQuery: String? = null
    var lastSortBy: String? = null
    var lastPage: Int = 0

    override suspend fun getRecipes(
        search: String?,
        sortBy: String?,
        page: Int,
        perPage: Int,
    ): Result<RecipesResponse> {
        lastSearchQuery = search
        lastSortBy = sortBy
        lastPage = page

        return if (shouldFail) {
            Result.failure(Exception(errorMessage))
        } else {
            Result.success(recipesResponse)
        }
    }

    override suspend fun getRecipe(id: Int): Result<Recipe> =
        if (shouldFail) {
            Result.failure(Exception(errorMessage))
        } else {
            Result.success(createSampleRecipe(id))
        }
}

/**
 * Create a sample RecipesResponse for testing.
 */
private fun createSampleRecipesResponse(
    count: Int,
    currentPage: Int = 1,
    hasNext: Boolean = true,
): RecipesResponse {
    val recipes = (1..count).map { createSampleRecipe(it) }
    return RecipesResponse(
        data = recipes,
        total = 100,
        from = (currentPage - 1) * 25 + 1,
        to = (currentPage - 1) * 25 + count,
        perPage = 25,
        currentPage = currentPage,
        prevPageUrl = if (currentPage > 1) "https://usetrmnl.com/recipes.json?page=${currentPage - 1}" else null,
        nextPageUrl = if (hasNext) "https://usetrmnl.com/recipes.json?page=${currentPage + 1}" else null,
    )
}

/**
 * Create a sample Recipe for testing.
 */
private fun createSampleRecipe(id: Int): Recipe =
    Recipe(
        id = id,
        name = "Recipe $id",
        iconUrl = "https://example.com/icon$id.png",
        screenshotUrl = "https://example.com/screenshot$id.png",
        stats =
            RecipeStats(
                installs = id * 100,
                forks = id * 10,
            ),
    )
