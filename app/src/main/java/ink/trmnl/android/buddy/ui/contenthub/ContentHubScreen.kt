package ink.trmnl.android.buddy.ui.contenthub

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.retained.produceRetainedState
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
import ink.trmnl.android.buddy.content.repository.AnnouncementRepository
import ink.trmnl.android.buddy.content.repository.BlogPostRepository
import ink.trmnl.android.buddy.di.ApplicationContext
import ink.trmnl.android.buddy.ui.announcements.AnnouncementsScreen
import ink.trmnl.android.buddy.ui.blogposts.BlogPostsScreen
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.parcelize.Parcelize

/**
 * Content Hub Screen with bottom navigation for Announcements and Blog Posts.
 *
 * Features:
 * - Bottom navigation bar with two tabs
 * - Announcements tab shows existing AnnouncementsScreen
 * - Blog Posts tab shows BlogPostsScreen
 * - Material 3 design with proper theming
 * - Single TopAppBar with tab-specific actions (no nested toolbars)
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
        val announcementsUnreadCount: Int = 0,
        val blogPostsUnreadCount: Int = 0,
        val announcementsState: AnnouncementsScreenState? = null,
        val blogPostsState: BlogPostsScreenState? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data class TabSelected(
            val tab: Tab,
        ) : Event()
    }

    // Placeholder for announcements state (to be provided by embedded presenter)
    data class AnnouncementsScreenState(
        val unreadCount: Int = 0,
        val filter: ink.trmnl.android.buddy.ui.announcements.AnnouncementsScreen.Filter =
            ink.trmnl.android.buddy.ui.announcements.AnnouncementsScreen.Filter.ALL,
    )

    // Placeholder for blog posts state (to be provided by embedded presenter)
    data class BlogPostsScreenState(
        val selectedCategory: String? = null,
        val availableCategories: List<String> = emptyList(),
    )
}

/**
 * Presenter for ContentHubScreen.
 * Manages tab selection state and fetches unread counts for each tab.
 */
@Inject
class ContentHubPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val announcementRepository: AnnouncementRepository,
        private val blogPostRepository: BlogPostRepository,
    ) : Presenter<ContentHubScreen.State> {
        @Composable
        override fun present(): ContentHubScreen.State {
            var selectedTab by rememberRetained { mutableStateOf(ContentHubScreen.Tab.ANNOUNCEMENTS) }

            // Fetch unread counts from repositories
            val announcementsUnreadCount by produceRetainedState(initialValue = 0) {
                announcementRepository.getUnreadCount().collect { count ->
                    value = count
                }
            }

            val blogPostsUnreadCount by produceRetainedState(initialValue = 0) {
                blogPostRepository.getUnreadCount().collect { count ->
                    value = count
                }
            }

            return ContentHubScreen.State(
                selectedTab = selectedTab,
                announcementsUnreadCount = announcementsUnreadCount,
                blogPostsUnreadCount = blogPostsUnreadCount,
            ) { event ->
                when (event) {
                    ContentHubScreen.Event.BackClicked -> {
                        navigator.pop()
                    }

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
 * Shows tab-specific TopAppBar actions to avoid nested toolbars.
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
                    when (state.selectedTab) {
                        ContentHubScreen.Tab.ANNOUNCEMENTS -> {
                            // Show announcements title with unread badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TrmnlTitle("Announcements")
                                state.announcementsState?.let { announcementsState ->
                                    if (announcementsState.unreadCount > 0) {
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = announcementsState.unreadCount.toString(),
                                                modifier =
                                                    Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 2.dp,
                                                    ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ContentHubScreen.Tab.BLOG_POSTS -> {
                            // Show blog posts title with category if selected
                            TrmnlTitle(
                                state.blogPostsState?.selectedCategory?.let {
                                    "Blog Posts - $it"
                                } ?: "Blog Posts",
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(ContentHubScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Show tab-specific actions
                    when (state.selectedTab) {
                        ContentHubScreen.Tab.ANNOUNCEMENTS -> {
                            // No additional actions for announcements
                        }

                        ContentHubScreen.Tab.BLOG_POSTS -> {
                            // Category filter for blog posts
                            state.blogPostsState?.let { blogPostsState ->
                                var showCategoryMenu by androidx.compose.runtime.remember {
                                    androidx.compose.runtime.mutableStateOf(false)
                                }

                                Box {
                                    IconButton(onClick = { showCategoryMenu = true }) {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                                                ),
                                            contentDescription = "Filter by category",
                                        )
                                    }

                                    androidx.compose.material3.DropdownMenu(
                                        expanded = showCategoryMenu,
                                        onDismissRequest = { showCategoryMenu = false },
                                    ) {
                                        // This will be populated by the embedded screen
                                        // For now, just a placeholder
                                    }
                                }
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.selectedTab == ContentHubScreen.Tab.ANNOUNCEMENTS,
                    onClick = { state.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.ANNOUNCEMENTS)) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.announcementsUnreadCount > 0) {
                                    androidx.compose.material3.Badge {
                                        Text(text = state.announcementsUnreadCount.toString())
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Announcements",
                            )
                        }
                    },
                    label = { Text("Announcements") },
                )

                NavigationBarItem(
                    selected = state.selectedTab == ContentHubScreen.Tab.BLOG_POSTS,
                    onClick = { state.eventSink(ContentHubScreen.Event.TabSelected(ContentHubScreen.Tab.BLOG_POSTS)) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.blogPostsUnreadCount > 0) {
                                    androidx.compose.material3.Badge {
                                        Text(text = state.blogPostsUnreadCount.toString())
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.newspaper_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Blog Posts",
                            )
                        }
                    },
                    label = { Text("Blog Posts") },
                )
            }
        },
    ) { innerPadding ->
        // Content area with crossfade animation for smooth tab transitions
        Crossfade(
            targetState = state.selectedTab,
            animationSpec = tween(durationMillis = 300),
            label = "Content Hub Tab Transition",
        ) { selectedTab ->
            when (selectedTab) {
                ContentHubScreen.Tab.ANNOUNCEMENTS -> {
                    // Embed existing AnnouncementsScreen using Circuit
                    val backStack = rememberSaveableBackStack(root = AnnouncementsScreen(isEmbedded = true))
                    val circuitNavigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
                    NavigableCircuitContent(
                        navigator = circuitNavigator,
                        backStack = backStack,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                    )
                }

                ContentHubScreen.Tab.BLOG_POSTS -> {
                    // Embed BlogPostsScreen using Circuit
                    val backStack = rememberSaveableBackStack(root = BlogPostsScreen(isEmbedded = true))
                    val circuitNavigator = rememberCircuitNavigator(backStack = backStack, onRootPop = {})
                    NavigableCircuitContent(
                        navigator = circuitNavigator,
                        backStack = backStack,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                    )
                }
            }
        }
    }
}

// ============================================================================
// Compose Previews
// ============================================================================

/**
 * Preview-only version of ContentHubContent that doesn't require Circuit navigation.
 * Used for Compose previews to show the UI structure without embedded screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentHubContentPreview(
    state: ContentHubScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    when (state.selectedTab) {
                        ContentHubScreen.Tab.ANNOUNCEMENTS -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TrmnlTitle("Announcements")
                                state.announcementsState?.let { announcementsState ->
                                    if (announcementsState.unreadCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Text(
                                                text = announcementsState.unreadCount.toString(),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ContentHubScreen.Tab.BLOG_POSTS -> {
                            TrmnlTitle(
                                state.blogPostsState?.selectedCategory?.let {
                                    "Blog Posts - $it"
                                } ?: "Blog Posts",
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    when (state.selectedTab) {
                        ContentHubScreen.Tab.ANNOUNCEMENTS -> {
                            // No additional actions
                        }

                        ContentHubScreen.Tab.BLOG_POSTS -> {
                            IconButton(onClick = { }) {
                                Icon(
                                    painter = painterResource(R.drawable.list_alt_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                    contentDescription = "Filter by category",
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.selectedTab == ContentHubScreen.Tab.ANNOUNCEMENTS,
                    onClick = { },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.announcementsUnreadCount > 0) {
                                    androidx.compose.material3.Badge {
                                        Text(text = state.announcementsUnreadCount.toString())
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Announcements",
                            )
                        }
                    },
                    label = { Text("Announcements") },
                )

                NavigationBarItem(
                    selected = state.selectedTab == ContentHubScreen.Tab.BLOG_POSTS,
                    onClick = { },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (state.blogPostsUnreadCount > 0) {
                                    androidx.compose.material3.Badge {
                                        Text(text = state.blogPostsUnreadCount.toString())
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.newspaper_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Blog Posts",
                            )
                        }
                    },
                    label = { Text("Blog Posts") },
                )
            }
        },
    ) { innerPadding ->
        // Placeholder content area for preview
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    // For debug only - the tab screens are overridden here
                    .background(Color.Red.copy(alpha = 0.1f))
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            when (state.selectedTab) {
                                ContentHubScreen.Tab.ANNOUNCEMENTS ->
                                    R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24

                                ContentHubScreen.Tab.BLOG_POSTS ->
                                    R.drawable.newspaper_24dp_e8eaed_fill0_wght400_grad0_opsz24
                            },
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text =
                        when (state.selectedTab) {
                            ContentHubScreen.Tab.ANNOUNCEMENTS -> "Announcements Content"
                            ContentHubScreen.Tab.BLOG_POSTS -> "Blog Posts Content"
                        },
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@PreviewLightDark
@Preview(name = "Content Hub - Announcements Tab")
@Composable
private fun ContentHubAnnouncementsTabPreview() {
    TrmnlBuddyAppTheme {
        ContentHubContentPreview(
            state =
                ContentHubScreen.State(
                    selectedTab = ContentHubScreen.Tab.ANNOUNCEMENTS,
                    announcementsState = ContentHubScreen.AnnouncementsScreenState(),
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Content Hub - Announcements with Unread Badge")
@Composable
private fun ContentHubAnnouncementsUnreadPreview() {
    TrmnlBuddyAppTheme {
        ContentHubContentPreview(
            state =
                ContentHubScreen.State(
                    selectedTab = ContentHubScreen.Tab.ANNOUNCEMENTS,
                    announcementsUnreadCount = 5,
                    blogPostsUnreadCount = 3,
                    announcementsState = ContentHubScreen.AnnouncementsScreenState(unreadCount = 5),
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Content Hub - Blog Posts Tab")
@Composable
private fun ContentHubBlogPostsTabPreview() {
    TrmnlBuddyAppTheme {
        ContentHubContentPreview(
            state =
                ContentHubScreen.State(
                    selectedTab = ContentHubScreen.Tab.BLOG_POSTS,
                    blogPostsState = ContentHubScreen.BlogPostsScreenState(),
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Content Hub - Blog Posts with Category")
@Composable
private fun ContentHubBlogPostsCategoryPreview() {
    TrmnlBuddyAppTheme {
        ContentHubContentPreview(
            state =
                ContentHubScreen.State(
                    selectedTab = ContentHubScreen.Tab.BLOG_POSTS,
                    blogPostsState =
                        ContentHubScreen.BlogPostsScreenState(
                            selectedCategory = "Product Updates",
                            availableCategories = listOf("Product Updates", "Community", "Engineering"),
                        ),
                ),
        )
    }
}

@PreviewLightDark
@Preview(name = "Navigation Bar Only")
@Composable
private fun ContentHubNavigationBarPreview() {
    TrmnlBuddyAppTheme {
        Surface {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = {
                        BadgedBox(
                            badge = {
                                androidx.compose.material3.Badge {
                                    Text(text = "3")
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Announcements",
                            )
                        }
                    },
                    label = { Text("Announcements") },
                )

                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = {
                        BadgedBox(
                            badge = {
                                androidx.compose.material3.Badge {
                                    Text(text = "5")
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.newspaper_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Blog Posts",
                            )
                        }
                    },
                    label = { Text("Blog Posts") },
                )
            }
        }
    }
}
