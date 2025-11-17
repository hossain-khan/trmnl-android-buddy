package ink.trmnl.android.buddy.ui.announcements

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.content.db.AnnouncementEntity
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.util.BrowserUtils
import kotlinx.coroutines.launch

/**
 * Presenter for AnnouncementsScreen.
 */
@Inject
class AnnouncementsPresenter
    constructor(
        @Assisted private val screen: AnnouncementsScreen,
        @Assisted private val navigator: Navigator,
        @ApplicationContext private val context: Context,
        private val announcementRepository: AnnouncementRepository,
        private val userPreferencesRepository: ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository,
    ) : Presenter<AnnouncementsScreen.State> {
        @Composable
        override fun present(): AnnouncementsScreen.State {
            var announcements by rememberRetained { mutableStateOf<List<AnnouncementEntity>>(emptyList()) }
            var isLoading by rememberRetained { mutableStateOf(true) }
            var isRefreshing by rememberRetained { mutableStateOf(false) }
            var filter by rememberRetained { mutableStateOf(AnnouncementsScreen.Filter.ALL) }
            var unreadCount by rememberRetained { mutableStateOf(0) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            var showAuthBanner by rememberRetained { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            // Capture theme colors for Custom Tabs
            val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
            val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()

            // Collect announcements from repository
            LaunchedEffect(filter) {
                val flow =
                    when (filter) {
                        AnnouncementsScreen.Filter.ALL -> announcementRepository.getAllAnnouncements()
                        AnnouncementsScreen.Filter.UNREAD -> announcementRepository.getUnreadAnnouncements()
                        AnnouncementsScreen.Filter.READ -> announcementRepository.getReadAnnouncements()
                    }

                flow.collect { latestAnnouncements ->
                    announcements = latestAnnouncements
                    isLoading = false
                    isRefreshing = false
                }
            }

            // Collect unread count
            LaunchedEffect(Unit) {
                announcementRepository.getUnreadCount().collect { count ->
                    unreadCount = count
                }
            }

            // Collect auth banner visibility preference
            LaunchedEffect(Unit) {
                userPreferencesRepository.userPreferencesFlow.collect { preferences ->
                    showAuthBanner = !preferences.isAnnouncementAuthBannerDismissed
                }
            }

            return AnnouncementsScreen.State(
                announcements = announcements,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                filter = filter,
                unreadCount = unreadCount,
                errorMessage = errorMessage,
                showTopBar = !screen.isEmbedded, // Hide top bar when embedded
                showAuthBanner = showAuthBanner,
            ) { event ->
                when (event) {
                    AnnouncementsScreen.Event.BackClicked -> {
                        navigator.pop()
                    }

                    AnnouncementsScreen.Event.Refresh -> {
                        isRefreshing = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = announcementRepository.refreshAnnouncements()
                            if (result.isFailure) {
                                errorMessage = "Failed to refresh: ${result.exceptionOrNull()?.message}"
                            }
                            isRefreshing = false
                        }
                    }

                    is AnnouncementsScreen.Event.FilterChanged -> {
                        filter = event.filter
                        // Don't set isLoading = true here, as it would unmount the list
                        // and prevent smooth animations. The LaunchedEffect(filter) will
                        // handle updating the announcements list automatically.
                    }

                    is AnnouncementsScreen.Event.AnnouncementClicked -> {
                        // Open announcement in Chrome Custom Tabs
                        BrowserUtils.openUrlInCustomTab(
                            context = context,
                            url = event.announcement.link,
                            toolbarColor = primaryColor,
                            secondaryColor = surfaceColor,
                        )
                        // Mark announcement as read
                        coroutineScope.launch {
                            announcementRepository.markAsRead(event.announcement.id)
                        }
                    }

                    is AnnouncementsScreen.Event.ToggleReadStatus -> {
                        coroutineScope.launch {
                            if (event.announcement.isRead) {
                                announcementRepository.markAsUnread(event.announcement.id)
                            } else {
                                announcementRepository.markAsRead(event.announcement.id)
                            }
                        }
                    }

                    AnnouncementsScreen.Event.MarkAllAsRead -> {
                        coroutineScope.launch {
                            announcementRepository.markAllAsRead()
                        }
                    }

                    AnnouncementsScreen.Event.DismissAuthBanner -> {
                        coroutineScope.launch {
                            userPreferencesRepository.setAnnouncementAuthBannerDismissed(true)
                        }
                    }
                }
            }
        }

        @CircuitInject(AnnouncementsScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(
                screen: AnnouncementsScreen,
                navigator: Navigator,
            ): AnnouncementsPresenter
        }
    }
