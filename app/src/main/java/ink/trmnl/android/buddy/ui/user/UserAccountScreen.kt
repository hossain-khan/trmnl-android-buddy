package ink.trmnl.android.buddy.ui.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.eithernet.ApiResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.Inject
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.TrmnlDeviceRepository
import ink.trmnl.android.buddy.api.models.User
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.welcome.WelcomeScreen
import ink.trmnl.android.buddy.util.GravatarUtils
import ink.trmnl.android.buddy.util.PrivacyUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Screen for displaying user account information.
 * Shows user profile data from the /me endpoint.
 */
@Parcelize
data object UserAccountScreen : Screen {
    data class State(
        val user: User? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val showLogoutDialog: Boolean = false,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object Refresh : Event()

        data object LogoutClicked : Event()

        data object ConfirmLogout : Event()

        data object DismissLogoutDialog : Event()
    }
}

/**
 * Presenter for UserAccountScreen.
 * Fetches user info from API and manages state.
 */
@Inject
class UserAccountPresenter(
    @Assisted private val navigator: Navigator,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
) : Presenter<UserAccountScreen.State> {
    @Composable
    override fun present(): UserAccountScreen.State {
        var user by rememberRetained { mutableStateOf<User?>(null) }
        var isLoading by rememberRetained { mutableStateOf(true) }
        var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
        var showLogoutDialog by rememberRetained { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()

        // Fetch user info on screen load
        LaunchedEffect(Unit) {
            loadUserInfo(
                onLoading = { isLoading = it },
                onSuccess = { loadedUser ->
                    user = loadedUser
                    errorMessage = null
                },
                onError = { error ->
                    errorMessage = error
                },
            )
        }

        return UserAccountScreen.State(
            user = user,
            isLoading = isLoading,
            errorMessage = errorMessage,
            showLogoutDialog = showLogoutDialog,
        ) { event ->
            when (event) {
                UserAccountScreen.Event.BackClicked -> {
                    navigator.pop()
                }
                UserAccountScreen.Event.Refresh -> {
                    coroutineScope.launch {
                        loadUserInfo(
                            onLoading = { isLoading = it },
                            onSuccess = { loadedUser ->
                                user = loadedUser
                                errorMessage = null
                            },
                            onError = { error ->
                                errorMessage = error
                            },
                        )
                    }
                }
                UserAccountScreen.Event.LogoutClicked -> {
                    showLogoutDialog = true
                }
                UserAccountScreen.Event.DismissLogoutDialog -> {
                    showLogoutDialog = false
                }
                UserAccountScreen.Event.ConfirmLogout -> {
                    showLogoutDialog = false
                    coroutineScope.launch {
                        // Clear all preferences (API token and device tokens)
                        userPreferencesRepository.clearAll()
                        deviceTokenRepository.clearAll()
                        // Navigate to welcome screen and clear back stack
                        navigator.resetRoot(WelcomeScreen)
                    }
                }
            }
        }
    }

    private suspend fun loadUserInfo(
        onLoading: (Boolean) -> Unit,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit,
    ) {
        onLoading(true)

        // Get API token from preferences
        val preferences = userPreferencesRepository.userPreferencesFlow.first()
        val apiToken = preferences.apiToken

        if (apiToken.isNullOrBlank()) {
            onLoading(false)
            onError("API token not found. Please sign in again.")
            return
        }

        // Create repository with API token
        val repository =
            TrmnlDeviceRepository(
                apiService =
                    ink.trmnl.android.buddy.api.TrmnlApiClient
                        .create(),
                apiKey = apiToken,
            )

        // Fetch user info
        when (val result = repository.userInfo()) {
            is ApiResult.Success -> {
                onSuccess(result.value)
                onLoading(false)
            }
            is ApiResult.Failure.HttpFailure -> {
                onLoading(false)
                onError("HTTP Error: ${result.code}")
            }
            is ApiResult.Failure.NetworkFailure -> {
                onLoading(false)
                onError("Network error. Please check your connection.")
            }
            is ApiResult.Failure.ApiFailure -> {
                onLoading(false)
                onError("API Error: ${result.error}")
            }
            is ApiResult.Failure.UnknownFailure -> {
                onLoading(false)
                onError("Unknown error occurred")
            }
        }
    }

    @CircuitInject(UserAccountScreen::class, AppScope::class)
    @AssistedFactory
    interface Factory {
        fun create(navigator: Navigator): UserAccountPresenter
    }
}

/**
 * Alpha transparency value for cards to allow background logo watermark to show through.
 */
private const val CARD_BACKGROUND_ALPHA = 0.7f

/**
 * UI content for UserAccountScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(UserAccountScreen::class, AppScope::class)
@Composable
fun UserAccountContent(
    state: UserAccountScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(UserAccountScreen.Event.BackClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { state.eventSink(UserAccountScreen.Event.LogoutClicked) }) {
                        Icon(
                            painter = painterResource(R.drawable.account_circle_off_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            state.errorMessage != null ->
                ErrorState(
                    errorMessage = state.errorMessage,
                    modifier = Modifier.padding(innerPadding),
                )
            state.user != null ->
                UserAccountSuccessContent(
                    user = state.user,
                    modifier = Modifier.padding(innerPadding),
                )
        }

        if (state.showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = { state.eventSink(UserAccountScreen.Event.ConfirmLogout) },
                onDismiss = { state.eventSink(UserAccountScreen.Event.DismissLogoutDialog) },
            )
        }
    }
}

/**
 * Loading state composable.
 */
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text("Loading account info...")
        }
    }
}

/**
 * Error state composable.
 */
@Composable
private fun ErrorState(
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.baseline_person_24),
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Success state content with user info.
 */
@Composable
private fun UserAccountSuccessContent(
    user: User,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundWatermark(modifier = Modifier.align(Alignment.CenterEnd))

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            UserProfileCard(user = user)
            PersonalInfoSection(user = user)
            LocaleTimezoneSection(user = user)
            ApiAccessSection(apiKey = user.apiKey)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Background watermark logo.
 */
@Composable
private fun BackgroundWatermark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.trmnl_logo_semi_transparent),
        contentDescription = null,
        modifier =
            modifier
                .size(600.dp)
                .offset(x = 200.dp),
        contentScale = ContentScale.Fit,
    )
}

/**
 * User profile card with avatar and name.
 */
@Composable
private fun UserProfileCard(
    user: User,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    androidx.compose.ui.graphics
                        .Color(colorResource(R.color.trmnl_orange_container).value),
                contentColor =
                    androidx.compose.ui.graphics
                        .Color(colorResource(R.color.trmnl_orange_on_container).value),
            ),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(GravatarUtils.getGravatarUrl(user.email, size = 160))
                        .crossfade(true)
                        .build(),
                contentDescription = "Profile avatar",
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.account_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                error = painterResource(R.drawable.account_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        androidx.compose.ui.graphics
                            .Color(
                                colorResource(R.color.trmnl_orange_on_container).value,
                            ).copy(alpha = CARD_BACKGROUND_ALPHA),
                )
            }
        }
    }
}

/**
 * Personal information section.
 */
@Composable
private fun PersonalInfoSection(
    user: User,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CARD_BACKGROUND_ALPHA),
                ),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("First Name") },
                    supportingContent = { Text(user.firstName) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
                ListItem(
                    headlineContent = { Text("Last Name") },
                    supportingContent = { Text(user.lastName) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
                ListItem(
                    headlineContent = { Text("Email") },
                    supportingContent = { Text(user.email) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            }
        }
    }
}

/**
 * Locale and timezone section.
 */
@Composable
private fun LocaleTimezoneSection(
    user: User,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Locale & Timezone",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CARD_BACKGROUND_ALPHA),
                ),
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Locale") },
                    supportingContent = { Text(user.locale) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
                ListItem(
                    headlineContent = { Text("Time Zone") },
                    supportingContent = { Text(user.timeZone) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
                ListItem(
                    headlineContent = { Text("IANA Timezone") },
                    supportingContent = { Text(user.timeZoneIana) },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
                ListItem(
                    headlineContent = { Text("UTC Offset") },
                    supportingContent = {
                        val hours = user.utcOffset / 3600
                        val sign = if (hours >= 0) "+" else ""
                        Text("UTC$sign$hours hours")
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                )
            }
        }
    }
}

/**
 * API access section.
 */
@Composable
private fun ApiAccessSection(
    apiKey: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "API Access",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CARD_BACKGROUND_ALPHA),
                ),
        ) {
            ListItem(
                headlineContent = { Text("API Key") },
                supportingContent = {
                    Text(
                        text = PrivacyUtils.redactApiKey(apiKey),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                colors =
                    ListItemDefaults.colors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
            )
        }
    }
}

/**
 * Logout confirmation dialog.
 */
@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.account_circle_off_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = {
            Text("Logout")
        },
        text = {
            Text(
                "Are you sure you want to logout?\n\n" +
                    "This will remove your account access token and all device tokens from this app.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Logout", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

// ============================================
// Composable Previews
// ============================================

@PreviewLightDark
@Composable
private fun LoadingStatePreview() {
    TrmnlBuddyAppTheme {
        LoadingState()
    }
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    TrmnlBuddyAppTheme {
        ErrorState(errorMessage = "Network error. Please check your connection.")
    }
}

@PreviewLightDark
@Composable
private fun UserProfileCardPreview() {
    TrmnlBuddyAppTheme {
        UserProfileCard(
            user =
                User(
                    name = "John Doe",
                    email = "john.doe@example.com",
                    firstName = "John",
                    lastName = "Doe",
                    locale = "en",
                    timeZone = "Eastern Time (US & Canada)",
                    timeZoneIana = "America/New_York",
                    utcOffset = -14400,
                    apiKey = "user_abc123xyz789",
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PersonalInfoSectionPreview() {
    TrmnlBuddyAppTheme {
        PersonalInfoSection(
            user =
                User(
                    name = "Jane Smith",
                    email = "jane.smith@example.com",
                    firstName = "Jane",
                    lastName = "Smith",
                    locale = "en",
                    timeZone = "Pacific Time (US & Canada)",
                    timeZoneIana = "America/Los_Angeles",
                    utcOffset = -25200,
                    apiKey = "user_xyz789abc123",
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun LocaleTimezoneSectionPreview() {
    TrmnlBuddyAppTheme {
        LocaleTimezoneSection(
            user =
                User(
                    name = "Alice Johnson",
                    email = "alice@example.com",
                    firstName = "Alice",
                    lastName = "Johnson",
                    locale = "en-GB",
                    timeZone = "London",
                    timeZoneIana = "Europe/London",
                    utcOffset = 0,
                    apiKey = "user_londonkey123",
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun ApiAccessSectionPreview() {
    TrmnlBuddyAppTheme {
        ApiAccessSection(apiKey = "user_secretapikey123456789")
    }
}

@Preview
@Composable
private fun LogoutDialogPreview() {
    TrmnlBuddyAppTheme {
        LogoutConfirmationDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun UserAccountContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        UserAccountContent(
            state =
                UserAccountScreen.State(
                    user = null,
                    isLoading = true,
                    errorMessage = null,
                    showLogoutDialog = false,
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun UserAccountContentErrorPreview() {
    TrmnlBuddyAppTheme {
        UserAccountContent(
            state =
                UserAccountScreen.State(
                    user = null,
                    isLoading = false,
                    errorMessage = "Network error. Please check your connection.",
                    showLogoutDialog = false,
                    eventSink = {},
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun UserAccountContentSuccessPreview() {
    TrmnlBuddyAppTheme {
        UserAccountContent(
            state =
                UserAccountScreen.State(
                    user =
                        User(
                            name = "John Doe",
                            email = "john.doe@example.com",
                            firstName = "John",
                            lastName = "Doe",
                            locale = "en",
                            timeZone = "Eastern Time (US & Canada)",
                            timeZoneIana = "America/New_York",
                            utcOffset = -14400,
                            apiKey = "user_abc123xyz789defghi",
                        ),
                    isLoading = false,
                    errorMessage = null,
                    showLogoutDialog = false,
                    eventSink = {},
                ),
        )
    }
}
