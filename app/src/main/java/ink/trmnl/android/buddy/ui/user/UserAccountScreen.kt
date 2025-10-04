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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.slack.circuit.codegen.annotations.CircuitInject
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
import ink.trmnl.android.buddy.ui.welcome.WelcomeScreen
import ink.trmnl.android.buddy.util.GravatarUtils
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
        var user by remember { mutableStateOf<User?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var showLogoutDialog by remember { mutableStateOf(false) }
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
 * Redacts an API key to show only first 4 and last 4 characters.
 * Example: "user_abc123xyz789" becomes "user****789"
 */
private fun redactApiKey(apiKey: String): String =
    when {
        apiKey.length <= 8 -> "****" // Too short, fully redact
        else -> {
            val prefix = apiKey.take(4)
            val suffix = apiKey.takeLast(4)
            val middleLength = (apiKey.length - 8).coerceAtLeast(4)
            "$prefix${"*".repeat(middleLength)}$suffix"
        }
    }

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
            state.isLoading -> {
                // Loading state
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
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

            state.errorMessage != null -> {
                // Error state
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
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
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.user != null -> {
                // Success state - show user info
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background watermark logo
                    Image(
                        painter = painterResource(R.drawable.trmnl_logo_semi_transparent),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(600.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = 200.dp),
                        contentScale = ContentScale.Fit,
                    )

                    // Content
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // User profile card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                // Gravatar avatar with fade-in effect
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalContext.current)
                                            .data(GravatarUtils.getGravatarUrl(state.user.email, size = 160))
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

                                // Name and email column
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = state.user.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = state.user.email,
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

                        // Personal Information Section
                        Text(
                            text = "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

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
                                    supportingContent = { Text(state.user.firstName) },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                                ListItem(
                                    headlineContent = { Text("Last Name") },
                                    supportingContent = { Text(state.user.lastName) },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                                ListItem(
                                    headlineContent = { Text("Email") },
                                    supportingContent = { Text(state.user.email) },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                            }
                        }

                        // Locale & Timezone Section
                        Text(
                            text = "Locale & Timezone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

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
                                    supportingContent = { Text(state.user.locale) },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                                ListItem(
                                    headlineContent = { Text("Time Zone") },
                                    supportingContent = { Text(state.user.timeZone) },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                                ListItem(
                                    headlineContent = { Text("IANA Timezone") },
                                    supportingContent = { Text(state.user.timeZoneIana) },
                                    colors =
                                        ListItemDefaults.colors(
                                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                        ),
                                )
                                ListItem(
                                    headlineContent = { Text("UTC Offset") },
                                    supportingContent = {
                                        val hours = state.user.utcOffset / 3600
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

                        // API Key Section
                        Text(
                            text = "API Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

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
                                        text = redactApiKey(state.user.apiKey),
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

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Logout Confirmation Dialog
        if (state.showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { state.eventSink(UserAccountScreen.Event.DismissLogoutDialog) },
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
                    TextButton(
                        onClick = { state.eventSink(UserAccountScreen.Event.ConfirmLogout) },
                    ) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { state.eventSink(UserAccountScreen.Event.DismissLogoutDialog) },
                    ) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}
