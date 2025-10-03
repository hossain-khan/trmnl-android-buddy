package ink.trmnl.android.buddy.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
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
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.accesstoken.AccessTokenScreen
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
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
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GetStartedClicked : Event()
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
    ) : Presenter<WelcomeScreen.State> {
        @Composable
        override fun present(): WelcomeScreen.State {
            // Check if user has API token on first load
            val userPreferences by androidx.compose.runtime.produceState<ink.trmnl.android.buddy.data.preferences.UserPreferences?>(
                initialValue = null,
            ) {
                value = userPreferencesRepository.userPreferencesFlow.first()
            }

            return WelcomeScreen.State(
                isLoading = userPreferences == null,
            ) { event ->
                when (event) {
                    WelcomeScreen.Event.GetStartedClicked -> {
                        // Navigate based on whether API token exists
                        if (userPreferences?.apiToken.isNullOrBlank()) {
                            navigator.goTo(AccessTokenScreen)
                        } else {
                            // Navigate to devices list screen
                            navigator.goTo(TrmnlDevicesScreen)
                        }
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
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
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
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "TRMNL Buddy",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
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
                Button(
                    onClick = { state.eventSink(WelcomeScreen.Event.GetStartedClicked) },
                    enabled = !state.isLoading,
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
