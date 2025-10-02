package ink.trmnl.android.buddy.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
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
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()

        data object Refresh : Event()
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
) : Presenter<UserAccountScreen.State> {
    @Composable
    override fun present(): UserAccountScreen.State {
        var user by remember { mutableStateOf<User?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
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
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.account_circle_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                contentDescription = "Profile",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.user.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.user.email,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
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
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("First Name") },
                                supportingContent = { Text(state.user.firstName) },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                            )
                            ListItem(
                                headlineContent = { Text("Last Name") },
                                supportingContent = { Text(state.user.lastName) },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                            )
                            ListItem(
                                headlineContent = { Text("Email") },
                                supportingContent = { Text(state.user.email) },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
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
                    ) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Locale") },
                                supportingContent = { Text(state.user.locale) },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                            )
                            ListItem(
                                headlineContent = { Text("Time Zone") },
                                supportingContent = { Text(state.user.timeZone) },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                            )
                            ListItem(
                                headlineContent = { Text("IANA Timezone") },
                                supportingContent = { Text(state.user.timeZoneIana) },
                                colors =
                                    ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surface,
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
                                        containerColor = MaterialTheme.colorScheme.surface,
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
                    ) {
                        ListItem(
                            headlineContent = { Text("API Key") },
                            supportingContent = {
                                Text(
                                    text = state.user.apiKey,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            },
                            colors =
                                ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
