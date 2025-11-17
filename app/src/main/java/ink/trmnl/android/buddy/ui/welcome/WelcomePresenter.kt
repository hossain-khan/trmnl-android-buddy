package ink.trmnl.android.buddy.ui.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.produceRetainedState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import ink.trmnl.android.buddy.ui.auth.AuthenticationScreen
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import ink.trmnl.android.buddy.ui.welcome.WelcomeScreen.Event.GetStartedClicked
import ink.trmnl.android.buddy.ui.welcome.WelcomeScreen.Event.ViewUpdatesClicked
import kotlinx.coroutines.flow.first

/**
 * Presenter for WelcomeScreen.
 * Handles navigation logic based on whether user has configured API token.
 */
@Inject
class WelcomePresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val announcementRepository: AnnouncementRepository,
        private val blogPostRepository: BlogPostRepository,
    ) : Presenter<WelcomeScreen.State> {
        @Composable
        override fun present(): WelcomeScreen.State {
            // Check if user has API token on first load
            val userPreferences by produceRetainedState<UserPreferences?>(
                initialValue = null,
            ) {
                value = userPreferencesRepository.userPreferencesFlow.first()
            }

            val isRssFeedContentEnabled = userPreferences?.isRssFeedContentEnabled ?: true

            // Fetch recent content (announcements + blog posts) only if RSS feed content is enabled
            val recentAnnouncementsCount by produceRetainedState(
                initialValue = 0,
                key1 = isRssFeedContentEnabled,
            ) {
                if (isRssFeedContentEnabled) {
                    announcementRepository.getAllAnnouncements().first().take(3).also {
                        value = it.size
                    }
                } else {
                    value = 0
                }
            }

            val recentBlogPostsCount by produceRetainedState(
                initialValue = 0,
                key1 = isRssFeedContentEnabled,
            ) {
                if (isRssFeedContentEnabled) {
                    blogPostRepository.getAllBlogPosts().first().take(3).also {
                        value = it.size
                    }
                } else {
                    value = 0
                }
            }

            // Fetch unread counts only if RSS feed content is enabled
            val unreadAnnouncementsCount by produceRetainedState(
                initialValue = 0,
                key1 = isRssFeedContentEnabled,
            ) {
                if (isRssFeedContentEnabled) {
                    announcementRepository.getUnreadCount().first().also {
                        value = it
                    }
                } else {
                    value = 0
                }
            }

            val unreadBlogPostsCount by produceRetainedState(
                initialValue = 0,
                key1 = isRssFeedContentEnabled,
            ) {
                if (isRssFeedContentEnabled) {
                    blogPostRepository.getUnreadCount().first().also {
                        value = it
                    }
                } else {
                    value = 0
                }
            }

            val totalContentCount = recentAnnouncementsCount + recentBlogPostsCount
            val totalUnreadCount = unreadAnnouncementsCount + unreadBlogPostsCount
            val hasRecentContent = isRssFeedContentEnabled && totalContentCount > 0

            return WelcomeScreen.State(
                isLoading = userPreferences == null,
                hasExistingToken = !userPreferences?.apiToken.isNullOrBlank(),
                hasRecentContent = hasRecentContent,
                recentContentCount = totalContentCount,
                unreadContentCount = totalUnreadCount,
            ) { event ->
                when (event) {
                    GetStartedClicked -> {
                        // Navigate based on whether API token exists
                        val prefs = userPreferences
                        if (prefs?.apiToken.isNullOrBlank()) {
                            navigator.goTo(AccessTokenScreen)
                        } else {
                            // Check if security is enabled
                            if (prefs.isSecurityEnabled) {
                                // Navigate to authentication screen
                                navigator.resetRoot(AuthenticationScreen)
                            } else {
                                // Navigate to devices list screen (resetRoot to prevent back navigation)
                                navigator.resetRoot(TrmnlDevicesScreen)
                            }
                        }
                    }

                    ViewUpdatesClicked -> {
                        // Navigate to ContentHubScreen (public content, no token required)
                        navigator.goTo(ContentHubScreen)
                    }
                }
            }
        }

        @CircuitInject(WelcomeScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): WelcomePresenter
        }
    }
