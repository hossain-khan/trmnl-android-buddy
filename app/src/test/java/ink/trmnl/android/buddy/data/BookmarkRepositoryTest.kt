package ink.trmnl.android.buddy.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.data.database.FakeBookmarkedRecipeDao
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for [DefaultBookmarkRepository].
 */
class BookmarkRepositoryTest {
    @Test
    fun `toggleBookmark adds recipe when not bookmarked`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)
            val recipe =
                Recipe(
                    id = 1,
                    name = "Test Recipe",
                    iconUrl = "https://example.com/icon.png",
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 100, forks = 10),
                )

            repository.toggleBookmark(recipe)

            repository.isBookmarked(1).test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `toggleBookmark removes recipe when already bookmarked`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)
            val recipe =
                Recipe(
                    id = 1,
                    name = "Test Recipe",
                    iconUrl = "https://example.com/icon.png",
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 100, forks = 10),
                )

            // First toggle - adds bookmark
            repository.toggleBookmark(recipe)
            // Second toggle - removes bookmark
            repository.toggleBookmark(recipe)

            repository.isBookmarked(1).test {
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `isBookmarked returns true when recipe is bookmarked`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)
            val recipe =
                Recipe(
                    id = 1,
                    name = "Test Recipe",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 100, forks = 10),
                )

            repository.toggleBookmark(recipe)

            repository.isBookmarked(1).test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `isBookmarked returns false when recipe is not bookmarked`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)

            repository.isBookmarked(1).test {
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `getAllBookmarks returns all bookmarked recipes`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)
            val recipe1 =
                Recipe(
                    id = 1,
                    name = "Recipe 1",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 100, forks = 10),
                )
            val recipe2 =
                Recipe(
                    id = 2,
                    name = "Recipe 2",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 200, forks = 20),
                )

            repository.toggleBookmark(recipe1)
            repository.toggleBookmark(recipe2)

            repository.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).hasSize(2)
            }
        }

    @Test
    fun `getAllBookmarks returns empty list when no bookmarks`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)

            repository.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).isEmpty()
            }
        }

    @Test
    fun `getAllBookmarkedIds returns all bookmarked recipe IDs`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val repository = DefaultBookmarkRepository(dao)
            val recipe1 =
                Recipe(
                    id = 1,
                    name = "Recipe 1",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 100, forks = 10),
                )
            val recipe2 =
                Recipe(
                    id = 2,
                    name = "Recipe 2",
                    iconUrl = null,
                    screenshotUrl = null,
                    stats = RecipeStats(installs = 200, forks = 20),
                )

            repository.toggleBookmark(recipe1)
            repository.toggleBookmark(recipe2)

            repository.getAllBookmarkedIds().test {
                val ids = awaitItem()
                assertThat(ids).hasSize(2)
            }
        }
}
