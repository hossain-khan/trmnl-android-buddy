package ink.trmnl.android.buddy.ui.welcome

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.db.BlogPostEntity
import ink.trmnl.android.buddy.content.db.FakeAnnouncementDao
import ink.trmnl.android.buddy.content.db.FakeBlogPostDao
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import ink.trmnl.android.buddy.ui.auth.AuthenticationScreen
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for WelcomeScreen presenter.
 * Tests onboarding flow, navigation logic, and content availability.
 */
class WelcomeScreenTest {
    // ========== Initial State Tests ==========

    @Test
    fun `presenter shows loading state initially`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                val state = awaitItem()
                assertThat(state.isLoading).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows not loading when preferences are loaded`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = null),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                // Skip loading state
                skipItems(1)
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects existing token`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = "test_token_123"),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasExistingToken).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter detects no token`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = null),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasExistingToken).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ========== Content Availability Tests ==========

    @Test
    fun `presenter shows no recent content when repositories are empty`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isRssFeedContentEnabled = true),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasRecentContent).isFalse()
                assertThat(state.recentContentCount).isEqualTo(0)
                assertThat(state.unreadContentCount).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows recent content when announcements exist`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isRssFeedContentEnabled = true),
                )
            val announcementDao = FakeAnnouncementDao()
            val announcementRepo = AnnouncementRepository(announcementDao)
            val blogPostRepo = createBlogPostRepository()

            // Add announcements to the DAO
            announcementDao.insertAll(
                listOf(
                    createAnnouncement("1"),
                    createAnnouncement("2"),
                ),
            )

            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasRecentContent).isTrue()
                assertThat(state.recentContentCount).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows recent content when blog posts exist`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isRssFeedContentEnabled = true),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostDao = FakeBlogPostDao()
            val blogPostRepo = BlogPostRepository(blogPostDao)

            // Add blog posts to the DAO
            blogPostDao.insertAll(
                listOf(
                    createBlogPost("1"),
                    createBlogPost("2"),
                    createBlogPost("3"),
                ),
            )

            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasRecentContent).isTrue()
                assertThat(state.recentContentCount).isEqualTo(3)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter combines announcements and blog posts count`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isRssFeedContentEnabled = true),
                )
            val announcementDao = FakeAnnouncementDao()
            val announcementRepo = AnnouncementRepository(announcementDao)
            val blogPostDao = FakeBlogPostDao()
            val blogPostRepo = BlogPostRepository(blogPostDao)

            // Add content to DAOs
            announcementDao.insertAll(
                listOf(
                    createAnnouncement("1"),
                    createAnnouncement("2"),
                ),
            )
            blogPostDao.insertAll(
                listOf(
                    createBlogPost("1"),
                    createBlogPost("2"),
                    createBlogPost("3"),
                ),
            )

            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasRecentContent).isTrue()
                assertThat(state.recentContentCount).isEqualTo(5) // 2 announcements + 3 blog posts
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter counts unread content correctly`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isRssFeedContentEnabled = true),
                )
            val announcementDao = FakeAnnouncementDao()
            val announcementRepo = AnnouncementRepository(announcementDao)
            val blogPostDao = FakeBlogPostDao()
            val blogPostRepo = BlogPostRepository(blogPostDao)

            // Add content with one read and one unread of each type
            announcementDao.insertAll(
                listOf(
                    createAnnouncement("1", isRead = false),
                    createAnnouncement("2", isRead = true),
                ),
            )
            blogPostDao.insertAll(
                listOf(
                    createBlogPost("1", isRead = false),
                ),
            )

            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.unreadContentCount).isEqualTo(2) // 1 unread announcement + 1 unread blog post
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter hides content when RSS feed is disabled`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(isRssFeedContentEnabled = false),
                )
            // Create empty repositories - content should not be fetched when RSS feed is disabled
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()

            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()
                assertThat(state.hasRecentContent).isFalse()
                assertThat(state.recentContentCount).isEqualTo(0)
                assertThat(state.unreadContentCount).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ========== Navigation Tests ==========

    @Test
    fun `GetStartedClicked navigates to AccessTokenScreen when no token exists`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences = UserPreferences(apiToken = null),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()

                state.eventSink(WelcomeScreen.Event.GetStartedClicked)

                assertThat(navigator.awaitNextScreen()).isEqualTo(AccessTokenScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GetStartedClicked navigates to TrmnlDevicesScreen when token exists and security disabled`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            apiToken = "test_token",
                            isSecurityEnabled = false,
                        ),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()

                state.eventSink(WelcomeScreen.Event.GetStartedClicked)

                assertThat(navigator.awaitResetRoot().newRoot).isEqualTo(TrmnlDevicesScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `GetStartedClicked navigates to AuthenticationScreen when token exists and security enabled`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            apiToken = "test_token",
                            isSecurityEnabled = true,
                        ),
                )
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()

                state.eventSink(WelcomeScreen.Event.GetStartedClicked)

                assertThat(navigator.awaitResetRoot().newRoot).isEqualTo(AuthenticationScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ViewUpdatesClicked navigates to ContentHubScreen`() =
        runTest {
            val navigator = FakeNavigator(WelcomeScreen)
            val userPrefsRepo = FakeUserPreferencesRepository()
            val announcementRepo = createAnnouncementRepository()
            val blogPostRepo = createBlogPostRepository()
            val presenter = WelcomePresenter(navigator, userPrefsRepo, announcementRepo, blogPostRepo)

            presenter.test {
                skipItems(1)
                val state = awaitItem()

                state.eventSink(WelcomeScreen.Event.ViewUpdatesClicked)

                assertThat(navigator.awaitNextScreen()).isEqualTo(ContentHubScreen)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ========== Test Fakes and Helpers ==========

    /**
     * Fake implementation of UserPreferencesRepository for testing.
     */
    private class FakeUserPreferencesRepository(
        initialPreferences: UserPreferences = UserPreferences(),
    ) : UserPreferencesRepository {
        private val _userPreferencesFlow = MutableStateFlow(initialPreferences)

        override val userPreferencesFlow = _userPreferencesFlow

        override suspend fun saveApiToken(token: String) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = token)
        }

        override suspend fun clearApiToken() {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(apiToken = null)
        }

        override suspend fun setOnboardingCompleted() {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isOnboardingCompleted = true)
        }

        override suspend fun setBatteryTrackingEnabled(enabled: Boolean) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isBatteryTrackingEnabled = enabled)
        }

        override suspend fun setLowBatteryNotificationEnabled(enabled: Boolean) {
            _userPreferencesFlow.value =
                _userPreferencesFlow.value.copy(isLowBatteryNotificationEnabled = enabled)
        }

        override suspend fun setLowBatteryThreshold(percent: Int) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(lowBatteryThresholdPercent = percent)
        }

        override suspend fun setRssFeedContentEnabled(enabled: Boolean) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isRssFeedContentEnabled = enabled)
        }

        override suspend fun setRssFeedContentNotificationEnabled(enabled: Boolean) {
            _userPreferencesFlow.value =
                _userPreferencesFlow.value.copy(isRssFeedContentNotificationEnabled = enabled)
        }

        override suspend fun setAnnouncementAuthBannerDismissed(dismissed: Boolean) {
            _userPreferencesFlow.value =
                _userPreferencesFlow.value.copy(isAnnouncementAuthBannerDismissed = dismissed)
        }

        override suspend fun setSecurityEnabled(enabled: Boolean) {
            _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isSecurityEnabled = enabled)
        }

        override suspend fun clearAll() {
            _userPreferencesFlow.value = UserPreferences()
        }
    }

    companion object {
        /**
         * Create an announcement repository with empty fake DAO.
         */
        private fun createAnnouncementRepository(): AnnouncementRepository = AnnouncementRepository(FakeAnnouncementDao())

        /**
         * Create a blog post repository with empty fake DAO.
         */
        private fun createBlogPostRepository(): BlogPostRepository = BlogPostRepository(FakeBlogPostDao())

        /**
         * Create a test announcement entity.
         */
        private fun createAnnouncement(
            id: String,
            isRead: Boolean = false,
        ): AnnouncementEntity =
            AnnouncementEntity(
                id = id,
                title = "Test Announcement $id",
                summary = "Test description",
                link = "https://example.com/$id",
                publishedDate = Instant.now(),
                fetchedAt = Instant.now(),
                isRead = isRead,
            )

        /**
         * Create a test blog post entity.
         */
        private fun createBlogPost(
            id: String,
            isRead: Boolean = false,
        ): BlogPostEntity =
            BlogPostEntity(
                id = id,
                title = "Test Blog Post $id",
                summary = "Test summary",
                link = "https://example.com/blog/$id",
                publishedDate = Instant.now(),
                authorName = "Test Author",
                category = "Test",
                fetchedAt = Instant.now(),
                featuredImageUrl = null,
                isRead = isRead,
                isFavorite = false,
                lastReadAt = null,
                readingProgressPercent = 0f,
            )
    }
}
