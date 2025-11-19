package ink.trmnl.android.buddy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.data.preferences.UserPreferences
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.dev.DevelopmentScreen
import ink.trmnl.android.buddy.security.BiometricAuthHelper
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.contenthub.ContentHubScreen
import ink.trmnl.android.buddy.ui.devicecatalog.DeviceCatalogScreen
import ink.trmnl.android.buddy.ui.recipescatalog.RecipesCatalogScreen
import ink.trmnl.android.buddy.ui.theme.Dimens
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.user.UserAccountScreen
import ink.trmnl.android.buddy.work.WorkerScheduler
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Screen for app settings.
 */
@Parcelize
data object SettingsScreen : Screen {
    data class State(
        val isBatteryTrackingEnabled: Boolean = true,
        val isLowBatteryNotificationEnabled: Boolean = false,
        val lowBatteryThresholdPercent: Int = 20,
        val isRssFeedContentEnabled: Boolean = false,
        val isRssFeedContentNotificationEnabled: Boolean = false,
        val isSecurityEnabled: Boolean = false,
        val isAuthenticationAvailable: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object AccountClicked : Event()

        data class BatteryTrackingToggled(
            val enabled: Boolean,
        ) : Event()

        data class LowBatteryNotificationToggled(
            val enabled: Boolean,
        ) : Event()

        data class LowBatteryThresholdChanged(
            val percent: Int,
        ) : Event()

        data class RssFeedContentToggled(
            val enabled: Boolean,
        ) : Event()

        data class RssFeedContentNotificationToggled(
            val enabled: Boolean,
        ) : Event()

        data class SecurityToggled(
            val enabled: Boolean,
        ) : Event()

        data object DevelopmentClicked : Event()

        data object DeviceCatalogClicked : Event()

        data object RecipesCatalogClicked : Event()

        data object ContentHubClicked : Event()
    }
}

/**
 * Presenter for SettingsScreen.
 */
@Inject
class SettingsPresenter(
    @Assisted private val navigator: Navigator,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val workerScheduler: WorkerScheduler,
    private val biometricAuthHelper: BiometricAuthHelper,
) : Presenter<SettingsScreen.State> {
    @Composable
    override fun present(): SettingsScreen.State {
        val preferences by userPreferencesRepository.userPreferencesFlow.collectAsState(
            initial =
                UserPreferences(),
        )
        val coroutineScope = rememberCoroutineScope()

        // Check biometric availability using the injected helper.
        val isAuthenticationAvailable = biometricAuthHelper.isBiometricAvailable()

        return SettingsScreen.State(
            isBatteryTrackingEnabled = preferences.isBatteryTrackingEnabled,
            isLowBatteryNotificationEnabled = preferences.isLowBatteryNotificationEnabled,
            lowBatteryThresholdPercent = preferences.lowBatteryThresholdPercent,
            isRssFeedContentEnabled = preferences.isRssFeedContentEnabled,
            isRssFeedContentNotificationEnabled = preferences.isRssFeedContentNotificationEnabled,
            isSecurityEnabled = preferences.isSecurityEnabled,
            isAuthenticationAvailable = isAuthenticationAvailable,
        ) { event ->
            when (event) {
                SettingsScreen.Event.BackClicked -> {
                    navigator.pop()
                }
                SettingsScreen.Event.AccountClicked -> {
                    navigator.goTo(UserAccountScreen)
                }
                is SettingsScreen.Event.BatteryTrackingToggled -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setBatteryTrackingEnabled(event.enabled)
                    }
                }
                is SettingsScreen.Event.LowBatteryNotificationToggled -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setLowBatteryNotificationEnabled(event.enabled)
                        // Schedule or cancel the weekly battery check worker based on toggle state
                        if (event.enabled) {
                            workerScheduler.scheduleLowBatteryNotification()
                        } else {
                            workerScheduler.cancelLowBatteryNotification()
                        }
                    }
                }
                is SettingsScreen.Event.LowBatteryThresholdChanged -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setLowBatteryThreshold(event.percent)
                        // Reschedule worker with updated threshold (uses REPLACE policy)
                        workerScheduler.scheduleLowBatteryNotification()
                    }
                }
                is SettingsScreen.Event.RssFeedContentToggled -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setRssFeedContentEnabled(event.enabled)
                        // Schedule or cancel both announcement and blog post sync workers
                        if (event.enabled) {
                            workerScheduler.scheduleAnnouncementSync()
                            workerScheduler.scheduleBlogPostSync()
                        } else {
                            workerScheduler.cancelAnnouncementSync()
                            workerScheduler.cancelBlogPostSync()
                        }
                    }
                }
                is SettingsScreen.Event.RssFeedContentNotificationToggled -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setRssFeedContentNotificationEnabled(event.enabled)
                    }
                }
                is SettingsScreen.Event.SecurityToggled -> {
                    coroutineScope.launch {
                        userPreferencesRepository.setSecurityEnabled(event.enabled)
                    }
                }
                SettingsScreen.Event.DevelopmentClicked -> {
                    navigator.goTo(DevelopmentScreen)
                }
                SettingsScreen.Event.DeviceCatalogClicked -> {
                    navigator.goTo(DeviceCatalogScreen)
                }
                SettingsScreen.Event.RecipesCatalogClicked -> {
                    navigator.goTo(RecipesCatalogScreen)
                }
                SettingsScreen.Event.ContentHubClicked -> {
                    navigator.goTo(ContentHubScreen)
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("Settings") },
                scrollBehavior = scrollBehavior,
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
                            modifier = Modifier.size(Dimens.iconSizeSmall),
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
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            // RSS Feed Content Section (Announcements & Blog Posts)
            RssFeedContentSection(
                isEnabled = state.isRssFeedContentEnabled,
                isNotificationEnabled = state.isRssFeedContentNotificationEnabled,
                onToggle = { enabled ->
                    state.eventSink(SettingsScreen.Event.RssFeedContentToggled(enabled))
                },
                onNotificationToggle = { enabled ->
                    state.eventSink(SettingsScreen.Event.RssFeedContentNotificationToggled(enabled))
                },
            )

            // Security Section
            SecuritySection(
                isSecurityEnabled = state.isSecurityEnabled,
                isAuthenticationAvailable = state.isAuthenticationAvailable,
                onSecurityToggle = { enabled ->
                    state.eventSink(SettingsScreen.Event.SecurityToggled(enabled))
                },
            )

            // Battery Tracking Section
            BatteryTrackingSection(
                isEnabled = state.isBatteryTrackingEnabled,
                onToggle = { enabled ->
                    state.eventSink(SettingsScreen.Event.BatteryTrackingToggled(enabled))
                },
            )

            // Low Battery Notification Section
            LowBatteryNotificationSection(
                isEnabled = state.isLowBatteryNotificationEnabled,
                thresholdPercent = state.lowBatteryThresholdPercent,
                onToggle = { enabled ->
                    state.eventSink(SettingsScreen.Event.LowBatteryNotificationToggled(enabled))
                },
                onThresholdChange = { percent ->
                    state.eventSink(SettingsScreen.Event.LowBatteryThresholdChanged(percent))
                },
            )

            // Extras Section
            ExtrasSection(
                onDeviceCatalogClick = {
                    state.eventSink(SettingsScreen.Event.DeviceCatalogClicked)
                },
                onRecipesCatalogClick = {
                    state.eventSink(SettingsScreen.Event.RecipesCatalogClicked)
                },
                onContentHubClick = {
                    state.eventSink(SettingsScreen.Event.ContentHubClicked)
                },
            )

            // Development Section (Debug builds only)
            if (BuildConfig.DEBUG) {
                DevelopmentSection(
                    onClick = { state.eventSink(SettingsScreen.Event.DevelopmentClicked) },
                )
            }

            // App Information Section
            AppInformationSection()
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
                    isLowBatteryNotificationEnabled = false,
                    lowBatteryThresholdPercent = 20,
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
                    isLowBatteryNotificationEnabled = false,
                    lowBatteryThresholdPercent = 20,
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsContentWithNotificationEnabledPreview() {
    TrmnlBuddyAppTheme {
        SettingsContent(
            state =
                SettingsScreen.State(
                    isRssFeedContentEnabled = true,
                    isRssFeedContentNotificationEnabled = true,
                    isBatteryTrackingEnabled = true,
                    isLowBatteryNotificationEnabled = true,
                    lowBatteryThresholdPercent = 30,
                    eventSink = {},
                ),
        )
    }
}
