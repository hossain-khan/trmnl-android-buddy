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
import ink.trmnl.android.buddy.api.models.AuthorBio
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.api.models.RecipesResponse
import ink.trmnl.android.buddy.data.FakeBookmarkRepository
import ink.trmnl.android.buddy.data.RecipesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
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
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

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
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

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

                // Advance time for debounce (500ms) + coroutine completion
                testScheduler.advanceTimeBy(500)
                testScheduler.advanceUntilIdle()

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
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Clear search
                loadedState.eventSink(RecipesCatalogScreen.Event.ClearSearchClicked)
                testScheduler.advanceUntilIdle()

                // Should have cleared search and fetched
                assertThat(repository.lastSearchQuery).isNull()

                cancelAndIgnoreRemainingEvents()
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
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                assertThat(loadedState.selectedSort).isEqualTo(SortOption.POPULARITY)

                // Select popularity sort
                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.POPULARITY))
                testScheduler.advanceUntilIdle()

                assertThat(repository.lastSortBy).isEqualTo("popularity")

                cancelAndIgnoreRemainingEvents()
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
            // Set up page 2 response
            repository.setResponseForPage(2, createSampleRecipesResponse(2, currentPage = 2, hasNext = false))
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                assertThat(loadedState.recipes).hasSize(2)
                assertThat(loadedState.currentPage).isEqualTo(1)
                assertThat(loadedState.hasMorePages).isTrue()

                // Load more
                loadedState.eventSink(RecipesCatalogScreen.Event.LoadMoreClicked)
                testScheduler.advanceUntilIdle()

                // Wait for state with more recipes appended
                var loadedMoreState: RecipesCatalogScreen.State
                do {
                    loadedMoreState = awaitItem()
                } while (loadedMoreState.recipes.size < 4)

                assertThat(loadedMoreState.recipes).hasSize(4) // 2 + 2
                assertThat(loadedMoreState.currentPage).isEqualTo(2)
                assertThat(loadedMoreState.hasMorePages).isFalse()

                cancelAndIgnoreRemainingEvents()
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
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

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
                testScheduler.advanceUntilIdle()

                // Wait for state with recipes loaded
                var retriedState = errorState
                do {
                    retriedState = awaitItem()
                } while (retriedState.recipes.isEmpty())

                assertThat(retriedState.error).isNull()
                assertThat(retriedState.recipes).hasSize(1)

                cancelAndIgnoreRemainingEvents()
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
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                loadedState.eventSink(RecipesCatalogScreen.Event.BackClicked)

                // Verify navigation occurred - navigator.pop() was called
                assertThat(navigator.awaitPop()).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort by newest works correctly`() =
        runTest {
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            presenter.test {
                // Wait for initial load
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Initial sort is POPULARITY by default
                assertThat(loadedState.selectedSort).isEqualTo(SortOption.POPULARITY)
                assertThat(repository.lastSortBy).isEqualTo("popularity") // Called during initial load

                // Change to NEWEST
                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.NEWEST))
                testScheduler.advanceUntilIdle()

                // Repository was called with new sort
                assertThat(repository.lastSortBy).isEqualTo("newest")

                // Consume any state updates
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort by oldest works correctly`() =
        runTest {
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.OLDEST))
                testScheduler.advanceUntilIdle()

                assertThat(repository.lastSortBy).isEqualTo("oldest")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort by popularity works correctly`() =
        runTest {
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.POPULARITY))
                testScheduler.advanceUntilIdle()

                assertThat(repository.lastSortBy).isEqualTo("popularity")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort by installs works correctly`() =
        runTest {
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.INSTALLS))
                testScheduler.advanceUntilIdle()

                assertThat(repository.lastSortBy).isEqualTo("install")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `sort by forks works correctly`() =
        runTest {
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse = createSampleRecipesResponse(1),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                loadedState.eventSink(RecipesCatalogScreen.Event.SortSelected(SortOption.FORKS))
                testScheduler.advanceUntilIdle()

                assertThat(repository.lastSortBy).isEqualTo("fork")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `category filtering returns all recipes when no categories selected`() =
        runTest {
            // Given
            val recipes =
                listOf(
                    createSampleRecipeWithCategories(1, "calendar"),
                    createSampleRecipeWithCategories(2, "sports"),
                    createSampleRecipeWithCategories(3, null),
                )
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse =
                        RecipesResponse(
                            data = recipes,
                            total = 3,
                            from = 1,
                            to = 3,
                            perPage = 25,
                            currentPage = 1,
                            prevPageUrl = null,
                            nextPageUrl = null,
                        ),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // With no categories selected, all recipes should be returned
                assertThat(loadedState.recipes).hasSize(3)
                assertThat(loadedState.selectedCategories).isEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `category filtering filters recipes with single selected category`() =
        runTest {
            // Given
            val recipes =
                listOf(
                    createSampleRecipeWithCategories(1, "calendar"),
                    createSampleRecipeWithCategories(2, "sports"),
                    createSampleRecipeWithCategories(3, "calendar,custom"),
                    createSampleRecipeWithCategories(4, "news"),
                )
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse =
                        RecipesResponse(
                            data = recipes,
                            total = 4,
                            from = 1,
                            to = 4,
                            perPage = 25,
                            currentPage = 1,
                            prevPageUrl = null,
                            nextPageUrl = null,
                        ),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Select "calendar" category
                loadedState.eventSink(RecipesCatalogScreen.Event.CategorySelected("calendar"))
                testScheduler.advanceUntilIdle()

                var filteredState = awaitItem()
                assertThat(filteredState.selectedCategories).isEqualTo(setOf("calendar"))
                assertThat(filteredState.recipes).hasSize(2) // Recipes 1 and 3 have "calendar"
                assertThat(filteredState.recipes.map { it.id }).isEqualTo(listOf(1, 3))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `category filtering uses OR logic for multiple categories`() =
        runTest {
            // Given
            val recipes =
                listOf(
                    createSampleRecipeWithCategories(1, "calendar"),
                    createSampleRecipeWithCategories(2, "sports"),
                    createSampleRecipeWithCategories(3, "calendar,custom"),
                    createSampleRecipeWithCategories(4, "news"),
                    createSampleRecipeWithCategories(5, "sports,games"),
                )
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse =
                        RecipesResponse(
                            data = recipes,
                            total = 5,
                            from = 1,
                            to = 5,
                            perPage = 25,
                            currentPage = 1,
                            prevPageUrl = null,
                            nextPageUrl = null,
                        ),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Select "calendar" and "sports" categories
                loadedState.eventSink(RecipesCatalogScreen.Event.CategorySelected("calendar"))
                testScheduler.advanceUntilIdle()
                var state1 = awaitItem()

                state1.eventSink(RecipesCatalogScreen.Event.CategorySelected("sports"))
                testScheduler.advanceUntilIdle()
                var state2 = awaitItem()

                // Should match recipes with calendar OR sports
                assertThat(state2.selectedCategories).isEqualTo(setOf("calendar", "sports"))
                assertThat(state2.recipes).hasSize(4) // Recipes 1, 2, 3, 5 have calendar OR sports
                assertThat(state2.recipes.map { it.id }).isEqualTo(listOf(1, 2, 3, 5))

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `category filtering handles comma-separated categories correctly`() =
        runTest {
            // Given
            val recipes =
                listOf(
                    createSampleRecipeWithCategories(1, "calendar, custom"),
                    createSampleRecipeWithCategories(2, " sports , games "),
                    createSampleRecipeWithCategories(3, "news"),
                )
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse =
                        RecipesResponse(
                            data = recipes,
                            total = 3,
                            from = 1,
                            to = 3,
                            perPage = 25,
                            currentPage = 1,
                            prevPageUrl = null,
                            nextPageUrl = null,
                        ),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Select "custom" category (should match despite comma-separated format)
                loadedState.eventSink(RecipesCatalogScreen.Event.CategorySelected("custom"))
                testScheduler.advanceUntilIdle()

                var filteredState = awaitItem()
                assertThat(filteredState.recipes).hasSize(1) // Only recipe 1 has "custom"
                assertThat(filteredState.recipes.first().id).isEqualTo(1)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `category filtering handles null or empty category field`() =
        runTest {
            // Given
            val recipes =
                listOf(
                    createSampleRecipeWithCategories(1, null),
                    createSampleRecipeWithCategories(2, ""),
                    createSampleRecipeWithCategories(3, "calendar"),
                )
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse =
                        RecipesResponse(
                            data = recipes,
                            total = 3,
                            from = 1,
                            to = 3,
                            perPage = 25,
                            currentPage = 1,
                            prevPageUrl = null,
                            nextPageUrl = null,
                        ),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Select "calendar" category
                loadedState.eventSink(RecipesCatalogScreen.Event.CategorySelected("calendar"))
                testScheduler.advanceUntilIdle()

                var filteredState = awaitItem()
                assertThat(filteredState.recipes).hasSize(1) // Only recipe 3 matches
                assertThat(filteredState.recipes.first().id).isEqualTo(3)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `category filtering returns empty list when no recipes match`() =
        runTest {
            // Given
            val recipes =
                listOf(
                    createSampleRecipeWithCategories(1, "calendar"),
                    createSampleRecipeWithCategories(2, "sports"),
                )
            val navigator = FakeNavigator(RecipesCatalogScreen)
            val repository =
                FakeRecipesRepository(
                    recipesResponse =
                        RecipesResponse(
                            data = recipes,
                            total = 2,
                            from = 1,
                            to = 2,
                            perPage = 25,
                            currentPage = 1,
                            prevPageUrl = null,
                            nextPageUrl = null,
                        ),
                )
            val bookmarkRepository = FakeBookmarkRepository()
            val presenter = RecipesCatalogPresenter(navigator, repository, bookmarkRepository)

            // When/Then
            presenter.test {
                var loadedState: RecipesCatalogScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.recipes.isEmpty())

                // Select a category that doesn't match any recipes
                loadedState.eventSink(RecipesCatalogScreen.Event.CategorySelected("finance"))
                testScheduler.advanceUntilIdle()

                var filteredState = awaitItem()
                assertThat(filteredState.recipes).isEmpty()
                assertThat(filteredState.selectedCategories).isEqualTo(setOf("finance"))

                cancelAndIgnoreRemainingEvents()
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

    // Map of page number to response - used for pagination testing
    private val pageResponses = mutableMapOf<Int, RecipesResponse>()

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
            // Use page-specific response if available, otherwise use default
            val response = pageResponses[page] ?: recipesResponse
            Result.success(response)
        }
    }

    override suspend fun getRecipe(id: Int): Result<Recipe> =
        if (shouldFail) {
            Result.failure(Exception(errorMessage))
        } else {
            Result.success(createSampleRecipe(id))
        }

    override suspend fun getCategories(): Result<List<String>> =
        if (shouldFail) {
            Result.failure(Exception(errorMessage))
        } else {
            Result.success(
                listOf(
                    "analytics",
                    "art",
                    "calendar",
                    "comics",
                    "crm",
                    "custom",
                    "discovery",
                    "ecommerce",
                    "education",
                    "email",
                    "entertainment",
                    "environment",
                    "finance",
                    "games",
                    "humor",
                    "images",
                    "kpi",
                    "life",
                    "marketing",
                    "nature",
                    "news",
                    "personal",
                    "productivity",
                    "programming",
                    "sales",
                    "sports",
                    "travel",
                ),
            )
        }

    /**
     * Set a specific response for a given page number.
     * Useful for testing pagination.
     */
    fun setResponseForPage(
        page: Int,
        response: RecipesResponse,
    ) {
        pageResponses[page] = response
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
    // Generate unique IDs based on page to avoid duplicates when testing pagination
    val startId = (currentPage - 1) * 25 + 1
    val recipes = (startId until startId + count).map { createSampleRecipe(it) }
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

/**
 * Create a sample Recipe with specific categories for testing.
 */
private fun createSampleRecipeWithCategories(
    id: Int,
    categories: String?,
): Recipe =
    Recipe(
        id = id,
        name = "Recipe $id",
        iconUrl = "https://example.com/icon$id.png",
        screenshotUrl = "https://example.com/screenshot$id.png",
        authorBio = AuthorBio(category = categories),
        stats =
            RecipeStats(
                installs = id * 100,
                forks = id * 10,
            ),
    )
