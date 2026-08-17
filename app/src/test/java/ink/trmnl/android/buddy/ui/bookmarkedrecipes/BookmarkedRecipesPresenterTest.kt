package ink.trmnl.android.buddy.ui.bookmarkedrecipes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
import ink.trmnl.android.buddy.data.FakeBookmarkRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [BookmarkedRecipesPresenter] and [BookmarkedRecipesScreen].
 *
 * Verifies:
 * - Presenter state emissions with reactive bookmarks
 * - Clear all dialog flows (open, dismiss, confirm)
 * - Bookmark toggling and deletion
 * - Back and Share click events
 * - Core bookmark repository operations
 */
@RunWith(RobolectricTestRunner::class)
class BookmarkedRecipesPresenterTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `presenter emits initial state with bookmarked recipes`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe1 = createSampleRecipe(1, "Recipe 1")
            val recipe2 = createSampleRecipe(2, "Recipe 2")
            bookmarkRepository.toggleBookmark(recipe1)
            bookmarkRepository.toggleBookmark(recipe2)

            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                var loadedState: BookmarkedRecipesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.bookmarkedRecipes.isEmpty())

                assertThat(loadedState.bookmarkedRecipes).hasSize(2)
                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.error).isNull()
                assertThat(loadedState.showClearAllDialog).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `BackClicked event pops navigator`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(BookmarkedRecipesScreen.Event.BackClicked)

                val popped = navigator.awaitPop()
                assertThat(popped).isNotNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ClearAllClicked and DismissClearAllDialog control dialog state`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                val state = awaitItem()
                assertThat(state.showClearAllDialog).isFalse()

                // Show dialog
                state.eventSink(BookmarkedRecipesScreen.Event.ClearAllClicked)
                val dialogState = awaitItem()
                assertThat(dialogState.showClearAllDialog).isTrue()

                // Dismiss dialog
                dialogState.eventSink(BookmarkedRecipesScreen.Event.DismissClearAllDialog)
                val dismissedState = awaitItem()
                assertThat(dismissedState.showClearAllDialog).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ConfirmClearAll clears bookmarks in repository and closes dialog`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe = createSampleRecipe(1, "Recipe 1")
            bookmarkRepository.toggleBookmark(recipe)

            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                var loadedState: BookmarkedRecipesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.bookmarkedRecipes.isEmpty())
                assertThat(loadedState.bookmarkedRecipes).hasSize(1)

                // Show and confirm clear all
                loadedState.eventSink(BookmarkedRecipesScreen.Event.ClearAllClicked)
                val dialogState = awaitItem()
                assertThat(dialogState.showClearAllDialog).isTrue()

                dialogState.eventSink(BookmarkedRecipesScreen.Event.ConfirmClearAll)
                val clearedDialogState = awaitItem()
                assertThat(clearedDialogState.showClearAllDialog).isFalse()

                var emptyState: BookmarkedRecipesScreen.State = clearedDialogState
                while (emptyState.bookmarkedRecipes.isNotEmpty()) {
                    emptyState = awaitItem()
                }
                assertThat(emptyState.bookmarkedRecipes).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `BookmarkClicked removes recipe from bookmarks`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe = createSampleRecipe(1, "Recipe 1")
            bookmarkRepository.toggleBookmark(recipe)

            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                var loadedState: BookmarkedRecipesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.bookmarkedRecipes.isEmpty())
                assertThat(loadedState.bookmarkedRecipes).hasSize(1)

                // Toggle bookmark off
                loadedState.eventSink(BookmarkedRecipesScreen.Event.BookmarkClicked(recipe))

                var emptyState: BookmarkedRecipesScreen.State
                do {
                    emptyState = awaitItem()
                } while (emptyState.bookmarkedRecipes.isNotEmpty())

                assertThat(emptyState.bookmarkedRecipes).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `RecipeClicked and ShareClicked events execute cleanly when bookmarks exist`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe = createSampleRecipe(1, "Recipe 1")
            bookmarkRepository.toggleBookmark(recipe)

            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                var loadedState: BookmarkedRecipesScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.bookmarkedRecipes.isEmpty())

                // Trigger RecipeClicked
                loadedState.eventSink(BookmarkedRecipesScreen.Event.RecipeClicked(recipe))

                // Trigger ShareClicked with non-empty bookmarks
                loadedState.eventSink(BookmarkedRecipesScreen.Event.ShareClicked)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ShareClicked executes cleanly when bookmarks list is empty`() =
        runTest {
            val bookmarkRepository = FakeBookmarkRepository()
            val navigator = FakeNavigator(BookmarkedRecipesScreen)
            val presenter = BookmarkedRecipesPresenter(navigator, context, bookmarkRepository)

            presenter.test {
                val state = awaitItem()

                // Trigger ShareClicked with empty bookmarks
                state.eventSink(BookmarkedRecipesScreen.Event.ShareClicked)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ========== Repository Integration Tests ==========
    @Test
    fun `repository loads bookmarked recipes correctly`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe1 = createSampleRecipe(1, "Recipe 1")
            val recipe2 = createSampleRecipe(2, "Recipe 2")

            // When
            bookmarkRepository.toggleBookmark(recipe1)
            bookmarkRepository.toggleBookmark(recipe2)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(2)
            assertThat(bookmarks.any { it.id == 1 }).isTrue()
            assertThat(bookmarks.any { it.id == 2 }).isTrue()
        }

    @Test
    fun `repository shows empty state when no bookmarks exist`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()

            // When/Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).isEmpty()
        }

    @Test
    fun `repository handles single bookmarked recipe`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe = createSampleRecipe(1, "Single Recipe")

            // When
            bookmarkRepository.toggleBookmark(recipe)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(1)
            assertThat(bookmarks[0].id).isEqualTo(1)
            assertThat(bookmarks[0].name).isEqualTo("Single Recipe")
        }

    @Test
    fun `repository handles many bookmarked recipes`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipes = (1..120).map { createSampleRecipe(it, "Recipe $it") }

            // When
            recipes.forEach { bookmarkRepository.toggleBookmark(it) }

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(120)
        }

    @Test
    fun `toggle bookmark removes bookmarked recipe`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe = createSampleRecipe(1, "Recipe 1")
            bookmarkRepository.toggleBookmark(recipe)

            // When - toggle again to remove
            bookmarkRepository.toggleBookmark(recipe)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).isEmpty()
        }

    @Test
    fun `toggle bookmark removes only the specified bookmark`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe1 = createSampleRecipe(1, "Recipe 1")
            val recipe2 = createSampleRecipe(2, "Recipe 2")
            val recipe3 = createSampleRecipe(3, "Recipe 3")
            bookmarkRepository.toggleBookmark(recipe1)
            bookmarkRepository.toggleBookmark(recipe2)
            bookmarkRepository.toggleBookmark(recipe3)

            // When - remove recipe2
            bookmarkRepository.toggleBookmark(recipe2)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(2)
            assertThat(bookmarks.any { it.id == 1 }).isTrue()
            assertThat(bookmarks.any { it.id == 3 }).isTrue()
            assertThat(bookmarks.any { it.id == 2 }).isFalse()
        }

    @Test
    fun `clear all bookmarks removes all bookmarks`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe1 = createSampleRecipe(1, "Recipe 1")
            val recipe2 = createSampleRecipe(2, "Recipe 2")
            bookmarkRepository.toggleBookmark(recipe1)
            bookmarkRepository.toggleBookmark(recipe2)

            // When
            bookmarkRepository.clearAllBookmarks()

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).isEmpty()
        }

    @Test
    fun `recipe with missing icon url is stored correctly`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe =
                Recipe(
                    id = 1,
                    name = "Recipe Without Icon",
                    iconUrl = null,
                    screenshotUrl = null,
                    authorBio = null,
                    customFields = emptyList(),
                    stats = RecipeStats(installs = 100, forks = 10),
                )

            // When
            bookmarkRepository.toggleBookmark(recipe)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(1)
            assertThat(bookmarks[0].iconUrl).isNull()
        }

    @Test
    fun `recipe with very long title is stored correctly`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val longTitle =
                "This is a very long recipe title that exceeds normal length " +
                    "and should be handled properly by the UI without causing issues"
            val recipe = createSampleRecipe(1, longTitle)

            // When
            bookmarkRepository.toggleBookmark(recipe)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(1)
            assertThat(bookmarks[0].name).isEqualTo(longTitle)
        }

    @Test
    fun `recipe with missing screenshot url is stored correctly`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe =
                Recipe(
                    id = 1,
                    name = "Recipe Without Screenshot",
                    iconUrl = "https://example.com/icon.png",
                    screenshotUrl = null,
                    authorBio = null,
                    customFields = emptyList(),
                    stats = RecipeStats(installs = 100, forks = 10),
                )

            // When
            bookmarkRepository.toggleBookmark(recipe)

            // Then
            val bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(1)
            assertThat(bookmarks[0].screenshotUrl).isNull()
        }

    @Test
    fun `bookmarks are updated in real-time via Flow`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe = createSampleRecipe(1, "Recipe 1")

            // When - initially empty
            var bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).isEmpty()

            // Add bookmark
            bookmarkRepository.toggleBookmark(recipe)

            // Then - should be updated
            bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(1)
        }

    @Test
    fun `removing all bookmarks one by one results in empty state`() =
        runTest {
            // Given
            val bookmarkRepository = FakeBookmarkRepository()
            val recipe1 = createSampleRecipe(1, "Recipe 1")
            val recipe2 = createSampleRecipe(2, "Recipe 2")
            val recipe3 = createSampleRecipe(3, "Recipe 3")
            bookmarkRepository.toggleBookmark(recipe1)
            bookmarkRepository.toggleBookmark(recipe2)
            bookmarkRepository.toggleBookmark(recipe3)

            // When - remove one by one
            bookmarkRepository.toggleBookmark(recipe1)
            var bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(2)

            bookmarkRepository.toggleBookmark(recipe2)
            bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).hasSize(1)

            bookmarkRepository.toggleBookmark(recipe3)
            bookmarks = bookmarkRepository.getAllBookmarks().first()
            assertThat(bookmarks).isEmpty()
        }

    @Test
    fun `navigator pop is callable`() =
        runTest {
            // Given
            val navigator = FakeNavigator(BookmarkedRecipesScreen)

            // When
            navigator.pop()

            // Then - verify navigation occurred
            assertThat(navigator.awaitPop()).isNotNull()
        }
}

/**
 * Create a sample Recipe for testing.
 */
private fun createSampleRecipe(
    id: Int,
    name: String = "Recipe $id",
): Recipe =
    Recipe(
        id = id,
        name = name,
        iconUrl = "https://example.com/icon$id.png",
        screenshotUrl = "https://example.com/screenshot$id.png",
        authorBio = null,
        customFields = emptyList(),
        stats =
            RecipeStats(
                installs = id * 100,
                forks = id * 10,
            ),
    )
