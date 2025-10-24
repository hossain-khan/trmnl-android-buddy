package ink.trmnl.android.buddy.ui.contenthub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.ui.announcements.AnnouncementsScreen
import ink.trmnl.android.buddy.ui.blogposts.BlogPostsScreen
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import kotlinx.parcelize.Parcelize

/**
 * Content Hub Screen with bottom navigation for Announcements and Blog Posts.
 *
 * Features:
 * - Bottom navigation bar with two tabs
 * - Announcements tab shows existing AnnouncementsScreen
 * - Blog Posts tab shows BlogPostsScreen (placeholder for now)
 * - Material 3 design with proper theming
 */
@Parcelize
data object ContentHubScreen : Screen {
    /**
     * Sealed class representing available tabs in the content hub.
     */
    enum class Tab {
        ANNOUNCEMENTS,
        BLOG_POSTS,
    }

    data class State(
        val selectedTab: Tab = Tab.ANNOUNCEMENTS,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class TabSelected(
            val tab: Tab,
        ) : Event()
    }
}

/**
 * Presenter for ContentHubScreen.
 * Manages tab selection state.
 */
@Inject
class ContentHubPresenter
    constructor(
        @Assisted private val navigator: Navigator,
    ) : Presenter<ContentHubScreen.State> {
        @Composable
        override fun present(): ContentHubScreen.State {
            var selectedTab by rememberRetained { mutableStateOf(ContentHubScreen.Tab.ANNOUNCEMENTS) }

            return ContentHubScreen.State(
                selectedTab = selectedTab,
            ) { event ->
                when (event) {
                    is ContentHubScreen.Event.TabSelected -> {
                        selectedTab = event.tab
                    }
                }
            }
        }

        @CircuitInject(ContentHubScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): ContentHubPresenter
        }
    }

/**
 * UI content for ContentHubScreen.
 * Displays bottom navigation and appropriate screen based on selected tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ContentHubScreen::class, AppScope::class)
@Composable
fun ContentHubContent(
    state: ContentHubScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    TrmnlTitle(
                        when (state.selectedTab) {
                            ContentHubScreen.Tab.ANNOUNCEMENTS -> "Announcements"
                            ContentHubScreen.Tab.BLOG_POSTS -> "Blog Posts"
                        },
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.selectedTab == ContentHubScreen.Tab.ANNOUNCEMENTS,
                    onClick = { state.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.ANNOUNCEMENTS)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.notification_important_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Announcements",
                        )
                    },
                    label = { Text("Announcements") },
                )

                NavigationBarItem(
                    selected = state.selectedTab == ContentHubScreen.Tab.BLOG_POSTS,
                    onClick = { state.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.BLOG_POSTS)) },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Blog Posts",
                        )
                    },
                    label = { Text("Blog Posts") },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (state.selectedTab) {
                ContentHubScreen.Tab.ANNOUNCEMENTS -> {
                    // Embed existing AnnouncementsScreen using Circuit
                    val backStack = rememberSaveableBackStack(root = AnnouncementsScreen)
                    val circuitNavigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
                    NavigableCircuitContent(
                        navigator = circuitNavigator,
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                ContentHubScreen.Tab.BLOG_POSTS -> {
                    // Embed BlogPostsScreen using Circuit
                    val backStack = rememberSaveableBackStack(root = BlogPostsScreen)
                    val circuitNavigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
                    NavigableCircuitContent(
                        navigator = circuitNavigator,
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
