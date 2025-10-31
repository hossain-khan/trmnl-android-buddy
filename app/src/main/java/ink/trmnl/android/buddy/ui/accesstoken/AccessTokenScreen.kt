package ink.trmnl.android.buddy.ui.accesstoken

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
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
import ink.trmnl.android.buddy.data.preferences.UserPreferencesRepository
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.devices.TrmnlDevicesScreen
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

/**
 * Screen for entering and saving TRMNL API token.
 */
@Parcelize
data object AccessTokenScreen : Screen {
    data class State(
        val token: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val eventSink: (Event) -> Unit = {},
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class TokenChanged(
            val token: String,
        ) : Event()

        data object SaveClicked : Event()

        data object BackClicked : Event()
    }
}

/**
 * Presenter for AccessTokenScreen.
 * Handles token input, validation, and saving to DataStore.
 */
@Inject
class AccessTokenPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val userPreferencesRepository: UserPreferencesRepository,
    ) : Presenter<AccessTokenScreen.State> {
        @Composable
        override fun present(): AccessTokenScreen.State {
            var token by rememberRetained { mutableStateOf("") }
            var isLoading by rememberRetained { mutableStateOf(false) }
            var errorMessage by rememberRetained { mutableStateOf<String?>(null) }
            val coroutineScope = rememberCoroutineScope()

            return AccessTokenScreen.State(
                token = token,
                isLoading = isLoading,
                errorMessage = errorMessage,
            ) { event ->
                when (event) {
                    is AccessTokenScreen.Event.TokenChanged -> {
                        token = event.token
                        errorMessage = null // Clear error when user types
                    }

                    AccessTokenScreen.Event.SaveClicked -> {
                        when {
                            token.isBlank() -> {
                                errorMessage = "Token cannot be empty"
                            }

                            token.length < 10 -> {
                                errorMessage = "Token appears to be too short"
                            }

                            else -> {
                                isLoading = true
                                errorMessage = null

                                coroutineScope.launch {
                                    try {
                                        // Save the token
                                        userPreferencesRepository.saveApiToken(token.trim())

                                        // Mark onboarding as completed
                                        userPreferencesRepository.setOnboardingCompleted()

                                        // Navigate to devices list screen (resetRoot to prevent back navigation)
                                        navigator.resetRoot(TrmnlDevicesScreen)
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to save token: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    }

                    AccessTokenScreen.Event.BackClicked -> {
                        navigator.pop()
                    }
                }
            }
        }

        @CircuitInject(AccessTokenScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): AccessTokenPresenter
        }
    }

/**
 * UI content for AccessTokenScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AccessTokenScreen::class, AppScope::class)
@Composable
fun AccessTokenContent(
    state: AccessTokenScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { TrmnlTitle("API Token Setup") },
                navigationIcon = {
                    IconButton(
                        onClick = { state.eventSink(AccessTokenScreen.Event.BackClicked) },
                        enabled = !state.isLoading,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                            contentDescription = "Back",
                        )
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
                    .padding(24.dp)
                    .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InstructionsSection()

            HelpLinksSection()

            ApiKeyRequirementsBanner()

            TokenInputField(
                token = state.token,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onTokenChange = { state.eventSink(AccessTokenScreen.Event.TokenChanged(it)) },
                onSaveClick = { state.eventSink(AccessTokenScreen.Event.SaveClicked) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            SaveTokenButton(
                isLoading = state.isLoading,
                isEnabled = state.token.isNotBlank(),
                onClick = { state.eventSink(AccessTokenScreen.Event.SaveClicked) },
            )

            Spacer(modifier = Modifier.weight(1f))

            SecurityHelpText(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/**
 * Instructions header section.
 */
@Composable
private fun InstructionsSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Enter your TRMNL User API Key",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text =
                "A User-level API Key is required to access account-level " +
                    "information like account-info, devices, playlists, and more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Closeable banner with API key requirement note.
 * See: https://discord.com/channels/1281055965508141100/1430384605546025073/1433867320089903264
 */
@Composable
private fun ApiKeyRequirementsBanner(modifier: Modifier = Modifier) {
    var visible by rememberRetained { mutableStateOf(true) }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(
                    painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Note:")
                            }
                            append(" At least one device in your TRMNL account ")
                            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("must have")
                            }
                            append(" a developer edition or BYOD license to enable User API Keys.")
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )

                IconButton(onClick = { visible = false }) {
                    Icon(
                        painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = "Close banner",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

/**
 * Help links section with clickable hyperlinks.
 */
@Composable
private fun HelpLinksSection(modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val linkStyle =
        TextLinkStyles(
            style =
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
        )
    val annotatedString =
        buildAnnotatedString {
            append("Generate your User API Key from your ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://usetrmnl.com/account",
                    styles = linkStyle,
                ),
            ) {
                append("account settings")
            }
            append(". ")

            withLink(
                LinkAnnotation.Url(
                    url = "https://help.usetrmnl.com/en/articles/11195228-user-level-api-keys",
                    styles = linkStyle,
                ),
            ) {
                append("Learn more")
            }
        }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/**
 * Token input field with visibility toggle.
 */
@Composable
private fun TokenInputField(
    token: String,
    isLoading: Boolean,
    errorMessage: String?,
    onTokenChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = token,
        onValueChange = onTokenChange,
        label = { Text("User API Key") },
        placeholder = { Text("user_abcxyz...") },
        modifier = modifier.fillMaxWidth(),
        enabled = !isLoading,
        isError = errorMessage != null,
        supportingText =
            if (errorMessage != null) {
                { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
        visualTransformation =
            if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    if (token.isNotBlank() && !isLoading) {
                        onSaveClick()
                    }
                },
            ),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    painter =
                        painterResource(
                            if (passwordVisible) {
                                R.drawable.visibility_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            } else {
                                R.drawable.visibility_off_24dp_e3e3e3_fill0_wght400_grad0_opsz24
                            },
                        ),
                    contentDescription = if (passwordVisible) "Hide token" else "Show token",
                )
            }
        },
        singleLine = true,
    )
}

/**
 * Save token button with loading state.
 */
@Composable
private fun SaveTokenButton(
    isLoading: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = isEnabled && !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(if (isLoading) "Saving..." else "Save Token")
    }
}

/**
 * Security help text shown at the bottom.
 */
@Composable
private fun SecurityHelpText(modifier: Modifier = Modifier) {
    Text(
        text = "Your token is stored securely on this device and is used to authenticate API requests to TRMNL.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

// ==================== Previews ====================

/**
 * Preview of AccessTokenScreen in default state (light and dark).
 */
@PreviewLightDark
@Composable
private fun AccessTokenContentPreview() {
    TrmnlBuddyAppTheme {
        AccessTokenContent(
            state =
                AccessTokenScreen.State(
                    token = "",
                    isLoading = false,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of AccessTokenScreen with token entered.
 */
@PreviewLightDark
@Composable
private fun AccessTokenContentWithTokenPreview() {
    TrmnlBuddyAppTheme {
        AccessTokenContent(
            state =
                AccessTokenScreen.State(
                    token = "user_abc123xyz456",
                    isLoading = false,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of AccessTokenScreen in loading state.
 */
@PreviewLightDark
@Composable
private fun AccessTokenContentLoadingPreview() {
    TrmnlBuddyAppTheme {
        AccessTokenContent(
            state =
                AccessTokenScreen.State(
                    token = "user_abc123xyz456",
                    isLoading = true,
                    errorMessage = null,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of AccessTokenScreen with error state.
 */
@PreviewLightDark
@Composable
private fun AccessTokenContentErrorPreview() {
    TrmnlBuddyAppTheme {
        AccessTokenContent(
            state =
                AccessTokenScreen.State(
                    token = "abc",
                    isLoading = false,
                    errorMessage = "Token appears to be too short",
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of AccessTokenScreen with empty token error.
 */
@Preview
@Composable
private fun AccessTokenContentEmptyErrorPreview() {
    TrmnlBuddyAppTheme {
        AccessTokenContent(
            state =
                AccessTokenScreen.State(
                    token = "",
                    isLoading = false,
                    errorMessage = "Token cannot be empty",
                    eventSink = {},
                ),
        )
    }
}
