package ink.trmnl.android.buddy.data.database

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for [BookmarkedRecipeDao].
 *
 * Uses a fake in-memory implementation for testing without a real database.
 */
class BookmarkedRecipeDaoTest {
    @Test
    fun `insert adds bookmark to database`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Test Recipe",
                    recipeIconUrl = "https://example.com/icon.png",
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = System.currentTimeMillis(),
                )

            dao.insert(bookmark)

            dao.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).hasSize(1)
                assertThat(bookmarks.first()).isEqualTo(bookmark)
            }
        }

    @Test
    fun `insert replaces existing bookmark`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark1 =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Test Recipe",
                    recipeIconUrl = "https://example.com/icon.png",
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = 1000L,
                )
            val bookmark2 =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Test Recipe Updated",
                    recipeIconUrl = "https://example.com/icon2.png",
                    installs = 200,
                    forks = 20,
                    bookmarkedAt = 2000L,
                )

            dao.insert(bookmark1)
            dao.insert(bookmark2)

            dao.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).hasSize(1)
                assertThat(bookmarks.first()).isEqualTo(bookmark2)
            }
        }

    @Test
    fun `deleteByRecipeId removes bookmark`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Test Recipe",
                    recipeIconUrl = null,
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = System.currentTimeMillis(),
                )

            dao.insert(bookmark)
            dao.deleteByRecipeId(1)

            dao.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).isEmpty()
            }
        }

    @Test
    fun `getAllBookmarks returns all bookmarks ordered by time descending`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark1 =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Recipe 1",
                    recipeIconUrl = null,
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = 1000L,
                )
            val bookmark2 =
                BookmarkedRecipeEntity(
                    recipeId = 2,
                    recipeName = "Recipe 2",
                    recipeIconUrl = null,
                    installs = 200,
                    forks = 20,
                    bookmarkedAt = 2000L,
                )
            val bookmark3 =
                BookmarkedRecipeEntity(
                    recipeId = 3,
                    recipeName = "Recipe 3",
                    recipeIconUrl = null,
                    installs = 300,
                    forks = 30,
                    bookmarkedAt = 1500L,
                )

            dao.insert(bookmark1)
            dao.insert(bookmark2)
            dao.insert(bookmark3)

            dao.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).hasSize(3)
                // Should be ordered by bookmarkedAt DESC: bookmark2, bookmark3, bookmark1
                assertThat(bookmarks[0]).isEqualTo(bookmark2)
                assertThat(bookmarks[1]).isEqualTo(bookmark3)
                assertThat(bookmarks[2]).isEqualTo(bookmark1)
            }
        }

    @Test
    fun `isBookmarked returns true when recipe is bookmarked`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Test Recipe",
                    recipeIconUrl = null,
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = System.currentTimeMillis(),
                )

            dao.insert(bookmark)

            dao.isBookmarked(1).test {
                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `isBookmarked returns false when recipe is not bookmarked`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()

            dao.isBookmarked(1).test {
                assertThat(awaitItem()).isFalse()
            }
        }

    @Test
    fun `getAllBookmarkedIds returns all bookmarked recipe IDs`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark1 =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Recipe 1",
                    recipeIconUrl = null,
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = 1000L,
                )
            val bookmark2 =
                BookmarkedRecipeEntity(
                    recipeId = 2,
                    recipeName = "Recipe 2",
                    recipeIconUrl = null,
                    installs = 200,
                    forks = 20,
                    bookmarkedAt = 2000L,
                )

            dao.insert(bookmark1)
            dao.insert(bookmark2)

            dao.getAllBookmarkedIds().test {
                val ids = awaitItem()
                assertThat(ids).hasSize(2)
                assertThat(ids).contains(1)
                assertThat(ids).contains(2)
            }
        }

    @Test
    fun `deleteAll removes all bookmarks`() =
        runTest {
            val dao = FakeBookmarkedRecipeDao()
            val bookmark1 =
                BookmarkedRecipeEntity(
                    recipeId = 1,
                    recipeName = "Recipe 1",
                    recipeIconUrl = null,
                    installs = 100,
                    forks = 10,
                    bookmarkedAt = 1000L,
                )
            val bookmark2 =
                BookmarkedRecipeEntity(
                    recipeId = 2,
                    recipeName = "Recipe 2",
                    recipeIconUrl = null,
                    installs = 200,
                    forks = 20,
                    bookmarkedAt = 2000L,
                )

            dao.insert(bookmark1)
            dao.insert(bookmark2)
            dao.deleteAll()

            dao.getAllBookmarks().test {
                val bookmarks = awaitItem()
                assertThat(bookmarks).isEmpty()
            }
        }
}
