package ink.trmnl.android.buddy.ui.announcements

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
import ink.trmnl.android.buddy.content.db.AnnouncementDao
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Unit tests for AnnouncementsScreen presenter.
 *
 * Tests cover:
 * - Initial loading and data fetch from repository
 * - Filter selection (All, Unread, Read)
 * - User interactions (click, mark as read, mark all as read)
 * - Authentication banner display and dismissal
 * - Navigation events
 * - Embedded mode (hide top bar)
 */
@RunWith(RobolectricTestRunner::class)
class AnnouncementsScreenTest {
    private fun createTestRepository(announcements: List<AnnouncementEntity> = emptyList()): AnnouncementRepository {
        val fakeDao = FakeAnnouncementDao()
        fakeDao.seedData(announcements)
        return AnnouncementRepository(fakeDao)
    }

    @Test
    fun `presenter loads announcements on initial composition`() =
        runTest {
            // Given
            val announcements = createSampleAnnouncements(count = 3)
            val announcementRepository = createTestRepository(announcements)
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait until we get announcements loaded
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading || loadedState.announcements.isEmpty())

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.announcements).hasSize(3)
                assertThat(loadedState.filter).isEqualTo(AnnouncementsScreen.Filter.ALL)
            }
        }

    @Test
    fun `presenter shows empty state when no announcements`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait until loading is complete
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.isLoading)

                assertThat(loadedState.isLoading).isFalse()
                assertThat(loadedState.announcements).isEmpty()
            }
        }

    @Test
    fun `presenter filters unread announcements correctly`() =
        runTest {
            // Given
            val announcements =
                listOf(
                    createAnnouncement(id = "1", isRead = false),
                    createAnnouncement(id = "2", isRead = true),
                    createAnnouncement(id = "3", isRead = false),
                )
            val announcementRepository = createTestRepository(announcements)
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                // Change filter to UNREAD
                loadedState.eventSink(AnnouncementsScreen.Event.FilterChanged(AnnouncementsScreen.Filter.UNREAD))

                // Wait for filtered state
                var filteredState: AnnouncementsScreen.State
                do {
                    filteredState = awaitItem()
                } while (filteredState.filter != AnnouncementsScreen.Filter.UNREAD)

                assertThat(filteredState.filter).isEqualTo(AnnouncementsScreen.Filter.UNREAD)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter filters read announcements correctly`() =
        runTest {
            // Given
            val announcements =
                listOf(
                    createAnnouncement(id = "1", isRead = false),
                    createAnnouncement(id = "2", isRead = true),
                    createAnnouncement(id = "3", isRead = false),
                )
            val announcementRepository = createTestRepository(announcements)
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                // Change filter to READ
                loadedState.eventSink(AnnouncementsScreen.Event.FilterChanged(AnnouncementsScreen.Filter.READ))

                // Wait for filtered state
                var filteredState: AnnouncementsScreen.State
                do {
                    filteredState = awaitItem()
                } while (filteredState.filter != AnnouncementsScreen.Filter.READ)

                assertThat(filteredState.filter).isEqualTo(AnnouncementsScreen.Filter.READ)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter updates unread count correctly`() =
        runTest {
            // Given
            val announcements =
                listOf(
                    createAnnouncement(id = "1", isRead = false),
                    createAnnouncement(id = "2", isRead = true),
                    createAnnouncement(id = "3", isRead = false),
                )
            val announcementRepository = createTestRepository(announcements)
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load with unread count
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                // Should have 2 unread announcements
                assertThat(loadedState.unreadCount).isEqualTo(2)
            }
        }

    @Test
    fun `presenter marks announcement as read when clicked`() =
        runTest {
            // Given
            val announcement = createAnnouncement(id = "1", isRead = false)
            val announcementRepository = createTestRepository(listOf(announcement))
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                assertThat(loadedState.unreadCount).isEqualTo(1)

                // Click announcement
                loadedState.eventSink(AnnouncementsScreen.Event.AnnouncementClicked(announcement))

                // Wait for update - announcement should now be marked as read
                var updatedState: AnnouncementsScreen.State
                do {
                    updatedState = awaitItem()
                } while (updatedState.unreadCount > 0)

                // Verify unread count decreased
                assertThat(updatedState.unreadCount).isEqualTo(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter toggles read status from unread to read`() =
        runTest {
            // Given
            val announcement = createAnnouncement(id = "1", isRead = false)
            val announcementRepository = createTestRepository(listOf(announcement))
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                val initialUnreadCount = loadedState.unreadCount
                assertThat(initialUnreadCount).isEqualTo(1)

                // Toggle read status
                loadedState.eventSink(AnnouncementsScreen.Event.ToggleReadStatus(announcement))

                // Wait for update - unread count should decrease
                var updatedState: AnnouncementsScreen.State
                do {
                    updatedState = awaitItem()
                } while (updatedState.unreadCount == initialUnreadCount)

                assertThat(updatedState.unreadCount).isEqualTo(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter toggles read status from read to unread`() =
        runTest {
            // Given
            val announcement = createAnnouncement(id = "1", isRead = true)
            val announcementRepository = createTestRepository(listOf(announcement))
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                val initialUnreadCount = loadedState.unreadCount
                assertThat(initialUnreadCount).isEqualTo(0)

                // Toggle read status
                loadedState.eventSink(AnnouncementsScreen.Event.ToggleReadStatus(announcement))

                // Wait for update - unread count should increase
                var updatedState: AnnouncementsScreen.State
                do {
                    updatedState = awaitItem()
                } while (updatedState.unreadCount == initialUnreadCount)

                assertThat(updatedState.unreadCount).isEqualTo(1)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter marks all announcements as read`() =
        runTest {
            // Given
            val announcements =
                listOf(
                    createAnnouncement(id = "1", isRead = false),
                    createAnnouncement(id = "2", isRead = false),
                    createAnnouncement(id = "3", isRead = false),
                )
            val announcementRepository = createTestRepository(announcements)
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial load
                var loadedState: AnnouncementsScreen.State
                do {
                    loadedState = awaitItem()
                } while (loadedState.announcements.isEmpty())

                assertThat(loadedState.unreadCount).isEqualTo(3)

                // Mark all as read
                loadedState.eventSink(AnnouncementsScreen.Event.MarkAllAsRead)

                // Wait for update - unread count should go to zero
                var updatedState: AnnouncementsScreen.State
                do {
                    updatedState = awaitItem()
                } while (updatedState.unreadCount > 0)

                assertThat(updatedState.unreadCount).isEqualTo(0)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows authentication banner when not dismissed`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isAnnouncementAuthBannerDismissed = false,
                        ),
                )
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for state with auth banner
                var state: AnnouncementsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.showAuthBanner).isTrue()
            }
        }

    @Test
    fun `presenter hides authentication banner when dismissed`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository =
                FakeUserPreferencesRepository(
                    initialPreferences =
                        UserPreferences(
                            isAnnouncementAuthBannerDismissed = true,
                        ),
                )
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for state
                var state: AnnouncementsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.showAuthBanner).isFalse()
            }
        }

    @Test
    fun `presenter dismisses authentication banner on user action`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial state
                var state: AnnouncementsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Dismiss banner
                state.eventSink(AnnouncementsScreen.Event.DismissAuthBanner)

                // Verify banner was dismissed in preferences
                assertThat(
                    userPreferencesRepository.userPreferencesFlow.first().isAnnouncementAuthBannerDismissed,
                ).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter navigates back on back clicked`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen())
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial state
                var state: AnnouncementsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                // Click back
                state.eventSink(AnnouncementsScreen.Event.BackClicked)

                // Verify navigation occurred
                assertThat(navigator.awaitPop()).isNotNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter shows top bar by default`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen(isEmbedded = false))
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(isEmbedded = false),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial state
                var state: AnnouncementsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.showTopBar).isTrue()
            }
        }

    @Test
    fun `presenter hides top bar when embedded`() =
        runTest {
            // Given
            val announcementRepository = createTestRepository()
            val userPreferencesRepository = FakeUserPreferencesRepository()
            val navigator = FakeNavigator(AnnouncementsScreen(isEmbedded = true))
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val presenter =
                AnnouncementsPresenter(
                    screen = AnnouncementsScreen(isEmbedded = true),
                    navigator = navigator,
                    context = context,
                    announcementRepository = announcementRepository,
                    userPreferencesRepository = userPreferencesRepository,
                )

            // When/Then
            presenter.test {
                // Wait for initial state
                var state: AnnouncementsScreen.State
                do {
                    state = awaitItem()
                } while (state.isLoading)

                assertThat(state.showTopBar).isFalse()
            }
        }
}

/**
 * Fake implementation of AnnouncementDao for testing.
 */
private class FakeAnnouncementDao : AnnouncementDao {
    private val announcements = mutableListOf<AnnouncementEntity>()
    private val allFlow = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    private val unreadFlow = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    private val readFlow = MutableStateFlow<List<AnnouncementEntity>>(emptyList())
    private val unreadCountFlow = MutableStateFlow(0)

    override fun getAll(): Flow<List<AnnouncementEntity>> = allFlow

    override fun getLatest(limit: Int): Flow<List<AnnouncementEntity>> =
        MutableStateFlow(announcements.sortedByDescending { it.publishedDate }.take(limit))

    override fun getUnread(): Flow<List<AnnouncementEntity>> = unreadFlow

    override fun getRead(): Flow<List<AnnouncementEntity>> = readFlow

    override suspend fun insertAll(announcements: List<AnnouncementEntity>) {
        this.announcements.clear()
        this.announcements.addAll(announcements)
        updateFlows()
    }

    override suspend fun markAsRead(id: String) {
        val index = announcements.indexOfFirst { it.id == id }
        if (index >= 0) {
            announcements[index] = announcements[index].copy(isRead = true)
            updateFlows()
        }
    }

    override suspend fun markAsUnread(id: String) {
        val index = announcements.indexOfFirst { it.id == id }
        if (index >= 0) {
            announcements[index] = announcements[index].copy(isRead = false)
            updateFlows()
        }
    }

    override suspend fun markAllAsRead() {
        for (i in announcements.indices) {
            announcements[i] = announcements[i].copy(isRead = true)
        }
        updateFlows()
    }

    override suspend fun deleteOlderThan(threshold: Long) {
        announcements.removeAll { it.fetchedAt.epochSecond < threshold }
        updateFlows()
    }

    override fun getUnreadCount(): Flow<Int> = unreadCountFlow

    private fun updateFlows() {
        val sorted = announcements.sortedByDescending { it.publishedDate }
        allFlow.value = sorted
        unreadFlow.value = sorted.filter { !it.isRead }
        readFlow.value = sorted.filter { it.isRead }
        unreadCountFlow.value = announcements.count { !it.isRead }
    }

    /**
     * Test helper to seed initial data.
     */
    fun seedData(data: List<AnnouncementEntity>) {
        announcements.clear()
        announcements.addAll(data)
        updateFlows()
    }
}

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
        _userPreferencesFlow.value = _userPreferencesFlow.value.copy(isLowBatteryNotificationEnabled = enabled)
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

/**
 * Helper function to create a sample announcement for testing.
 */
private fun createAnnouncement(
    id: String,
    title: String = "Announcement $id",
    summary: String = "Summary for announcement $id",
    link: String = "https://usetrmnl.com/announcements/$id",
    publishedDate: Instant = Instant.now().minus(1, ChronoUnit.DAYS),
    isRead: Boolean = false,
    fetchedAt: Instant = Instant.now(),
): AnnouncementEntity =
    AnnouncementEntity(
        id = id,
        title = title,
        summary = summary,
        link = link,
        publishedDate = publishedDate,
        isRead = isRead,
        fetchedAt = fetchedAt,
    )

/**
 * Helper function to create multiple sample announcements for testing.
 */
private fun createSampleAnnouncements(
    count: Int,
    baseIsRead: Boolean = false,
): List<AnnouncementEntity> =
    (1..count).map { index ->
        createAnnouncement(
            id = index.toString(),
            title = "Announcement $index",
            isRead = baseIsRead,
        )
    }
