package ink.trmnl.android.buddy.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Screen for app settings.
 */
@Parcelize
data object SettingsScreen : Screen {
    data class State(
        val isBatteryTrackingEnabled: Boolean = true,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object AccountClicked : Event()

        data class BatteryTrackingToggled(
            val enabled: Boolean,
        ) : Event()
    }
}

/**
 * Presenter for SettingsScreen.
 */
@Inject
class SettingsPresenter(
    @Assisted private val navigator: Navigator,
    private val userPreferencesRepository: UserPreferencesRepository,
) : Presenter<SettingsScreen.State> {
    @Composable
    override fun present(): SettingsScreen.State {
        val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
            initial =
                ink.trmnl.android.buddy.data.preferences
                    .UserPreferences(),
        )
        val coroutineScope = rememberCoroutineScope()

        return SettingsScreen.State(
            isBatteryTrackingEnabled = preferences.isBatteryTrackingEnabled,
        ) { event ->
            when (event) {
                SettingsScreen.Event.BackClicked -> {
                    navigator.pop()
                }
                SettingsScreen.Event.AccountClicked -> {
                    navigator.goTo(ink.trmnl.android.buddy.ui.user.UserAccountScreen)
                }
                is SettingsScreen.Event.BatteryTrackingToggled -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setBatteryTrackingEnabled(event.enabled)
                    }
                }
            }
        }
    }

    @CircuitInject(SettingsScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): SettingsPresenter
    }
}

/**
 * UI content for SettingsScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(SettingsScreen::class, AppScope::class)
@Composable
fun SettingsContent(
    state: SettingsScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Settings") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(SettingsScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { state.eventSink(SettingsScreen.Event.AccountClicked) },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.account_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Your Account",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Account")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Battery Tracking Section
            BatteryTrackingSection(
                isEnabled = state.isBatteryTrackingEnabled,
                onToggle = { enabled ->
                    state.eventSink(SettingsScreen.Event.BatteryTrackingToggled(enabled))
                },
            )

            // App Information Section
            AppInformationSection()
        }
    }
}

@Composable
private fun BatteryTrackingSection(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Battery History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = "Track Battery History",
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = {
                    Text(
                        text =
                            if (isEnabled) {
                                "Automatically collect battery data weekly to track device health over time"
                            } else {
                                "Battery history tracking is disabled. No data will be collected."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                    )
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        }
    }
}

@Composable
private fun AppInformationSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Text(
            text = "App Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.deviceinfo_thin_outline),
                            contentDescription = "GitHub",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = "Report Issues",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "View project on GitHub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.github_thin_outline),
                            contentDescription = "GitHub",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    modifier =
                        Modifier.clickable {
                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://github.com/hossain-khan/trmnl-android-buddy".toUri(),
                                )
                            context.startActivity(intent)
                        },
                )
            }
        }
    }
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun SettingsContentPreview() {
    TrmnlBuddyAppTheme {
        SettingsContent(
            state =
                SettingsScreen.State(
                    isBatteryTrackingEnabled = true,
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsContentDisabledPreview() {
    TrmnlBuddyAppTheme {
        SettingsContent(
            state =
                SettingsScreen.State(
                    isBatteryTrackingEnabled = false,
                    eventSink = {},
                ),
        )
    }
}
