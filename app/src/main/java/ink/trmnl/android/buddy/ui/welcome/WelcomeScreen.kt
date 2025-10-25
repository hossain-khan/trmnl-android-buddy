package ink.trmnl.android.buddy.ui.welcome

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.produceRetainedState
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
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import ink.trmnl.android.buddy.ui.theme.ebGaramondFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.parcelize.Parcelize

/**
 * Welcome screen - the first screen shown to users.
 * Checks if API token exists and routes accordingly.
 */
@Parcelize
data object WelcomeScreen : Screen {
    data class State(
        val isLoading: Boolean = true,
        val hasExistingToken: Boolean = false,
        val hasRecentContent: Boolean = false,
        val recentContentCount: Int = 0,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GetStartedClicked : Event()

        data object ViewUpdatesClicked : Event()
    }
}

/**
 * Presenter for WelcomeScreen.
 * Handles navigation logic based on whether user has configured API token.
 */
@Inject
class WelcomePresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val announcementRepository: ink.trmnl.android.buddy.content.repository.AnnouncementRepository,
        private val blogPostRepository: ink.trmnl.android.buddy.content.repository.BlogPostRepository,
    ) : Presenter<WelcomeScreen.State> {
        @Composable
        override fun present(): WelcomeScreen.State {
            // Check if user has API token on first load
            val userPreferences by produceRetainedState<UserPreferences?>(
                initialValue = null,
            ) {
                value = userPreferencesRepository.userPreferencesFlow.first()
            }

            // Fetch recent content (announcements + blog posts)
            val recentAnnouncementsCount by produceRetainedState(initialValue = 0) {
                announcementRepository.getAllAnnouncements().first().take(3).also {
                    value = it.size
                }
            }

            val recentBlogPostsCount by produceRetainedState(initialValue = 0) {
                blogPostRepository.getAllBlogPosts().first().take(3).also {
                    value = it.size
                }
            }

            val totalContentCount = recentAnnouncementsCount + recentBlogPostsCount
            val hasRecentContent = totalContentCount > 0

            return WelcomeScreen.State(
                isLoading = userPreferences == null,
                hasExistingToken = !userPreferences?.apiToken.isNullOrBlank(),
                hasRecentContent = hasRecentContent,
                recentContentCount = totalContentCount,
            ) { event ->
                when (event) {
                    WelcomeScreen.Event.GetStartedClicked -> {
                        // Navigate based on whether API token exists
                        if (userPreferences?.apiToken.isNullOrBlank()) {
                            navigator.goTo(AccessTokenScreen)
                        } else {
                            // Navigate to devices list screen (resetRoot to prevent back navigation)
                            navigator.resetRoot(TrmnlDevicesScreen)
                        }
                    }

                    WelcomeScreen.Event.ViewUpdatesClicked -> {
                        // Navigate to ContentHubScreen (public content, no token required)
                        navigator.goTo(ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen)
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

/**
 * UI content for WelcomeScreen.
 */
@CircuitInject(WelcomeScreen::class, AppScope::class)
@Composable
fun WelcomeContent(
    state: WelcomeScreen.State,
    modifier: Modifier = Modifier,
) {
    var showWelcomeBack by remember { mutableStateOf(false) }

    // Show "Welcome back" message after 800ms delay if user has token
    LaunchedEffect(state.hasExistingToken) {
        if (state.hasExistingToken && !state.isLoading) {
            delay(800)
            showWelcomeBack = true
        }
    }

    // Animate alpha for fade-in effect with slow tween animation
    val alpha by animateFloatAsState(
        targetValue = if (showWelcomeBack && state.hasExistingToken) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "welcomeBackAlpha",
    )

    val isDarkTheme = isSystemInDarkTheme()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // App icon or logo
                Image(
                    painter = painterResource(id = R.drawable.trmnl_logo_plain),
                    contentDescription = "TRMNL Buddy Logo",
                    modifier = Modifier.size(120.dp),
                    colorFilter = ColorFilter.tint(Color(colorResource(id = R.color.trmnl_orange).value)),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "TRMNL Buddy",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ebGaramondFontFamily,
                    textAlign = TextAlign.Center,
                )

                // Subtitle
                Text(
                    text = "Monitor your devices on the go!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Get Started button
                OutlinedButton(
                    onClick = { state.eventSink(WelcomeScreen.Event.GetStartedClicked) },
                    enabled = !state.isLoading,
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                // "What's New" section - only show if there's recent content
                if (state.hasRecentContent && !state.isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    WhatsNewSection(
                        contentCount = state.recentContentCount,
                        onClick = { state.eventSink(WelcomeScreen.Event.ViewUpdatesClicked) },
                    )
                }

                // Welcome back message area (fixed height to prevent layout shifts)
                // Only show if "What's New" is NOT shown (save space)
                if (!state.hasRecentContent && state.hasExistingToken) {
                    Box(
                        modifier =
                            Modifier
                                .height(32.dp)
                                .graphicsLayer { this.alpha = alpha },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (showWelcomeBack) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Welcome back!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    painter = painterResource(id = R.drawable.emoji_people_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // TRMNL "Works With" Badge - bottom right corner
            Image(
                painter =
                    painterResource(
                        id =
                            if (isDarkTheme) {
                                R.drawable.trmnl_badge_works_with_dark
                            } else {
                                R.drawable.trmnl_badge_works_with_light
                            },
                    ),
                contentDescription = "Works with TRMNL",
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

/**
 * What's New section with shimmer effect on the text.
 * Shows a text button that shimmers once to draw attention.
 */
@Composable
private fun WhatsNewSection(
    contentCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shimmerPlayed by remember { mutableStateOf(false) }

    // Shimmer animation - plays once
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "shimmer",
        )

    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmerAlpha",
    )

    // Stop shimmer after 2 seconds (2 cycles)
    LaunchedEffect(Unit) {
        delay(2000)
        shimmerPlayed = true
    }

    val alpha = if (shimmerPlayed) 1f else shimmerAlpha

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "What's New",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Text button with shimmer effect
        androidx.compose.material3.TextButton(
            onClick = onClick,
            modifier = Modifier.graphicsLayer { this.alpha = alpha },
        ) {
            Text(
                text = "$contentCount new ${if (contentCount == 1) "update" else "updates"} from TRMNL →",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
