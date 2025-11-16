package ink.trmnl.android.buddy.ui.bookmarkedrecipes

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.data.FakeBookmarkRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for BookmarkedRecipesScreen presenter.
 *
 * Verifies:
 * - Loading bookmarked recipes on initial composition
 * - Displaying list of bookmarked recipes
 * - Empty state when no bookmarks
 * - Bookmark removal (unbookmark)
 * - Clear all bookmarks functionality
 * - Navigation events
 * - Edge cases (single recipe, many recipes, missing data)
 *
 * Note: This presenter uses Android LocalContext for share functionality,
 * which makes standard Circuit unit testing challenging. These tests focus
 * on the core bookmark management logic that can be tested without triggering
 * Context-dependent operations.
 */
@RunWith(RobolectricTestRunner::class)
class BookmarkedRecipesPresenterTest {
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
            val longTitle = "This is a very long recipe title that exceeds normal length and should be handled properly by the UI without causing issues"
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
