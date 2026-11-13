package ink.trmnl.android.buddy.ui.accesstoken

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalUriHandler
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
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.components.TrmnlTitle
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

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

            LegacyTokenWarningCard(
                visible = state.isLegacyToken,
                isLoading = state.isLoading,
            )

            SaveTokenButton(
                isLoading = state.isLoading,
                isEnabled = state.token.isNotBlank(),
                isLegacyToken = state.isLegacyToken,
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
                    url = "https://trmnl.com/account",
                    styles = linkStyle,
                ),
            ) {
                append("account settings")
            }
            append(". ")

            withLink(
                LinkAnnotation.Url(
                    url = "https://help.trmnl.com/en/articles/11195228-user-level-api-keys",
                    styles = linkStyle,
                ),
            ) {
                append("Learn more")
            }
        }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text =
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("Recommended capabilities: ")
                    }
                    append("Read, Content, Profile, and Whole account (no limit).")
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
        placeholder = { Text("trmnl_abcxyz...") },
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
 * Warning card shown when a legacy `user_` token is detected, guiding the user
 * to create a capability-scoped `trmnl_` API key instead.
 */
@Composable
private fun LegacyTokenWarningCard(
    visible: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.warning_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = "Legacy API Key Detected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    text =
                        "Keys starting with \"user_\" are legacy tokens and only reach older endpoints. " +
                            "Newer features like Device Diagnostics, Timeline & Logs, and Account Info " +
                            "require a capability-scoped \"trmnl_\" key.",
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    text =
                        buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                append("Recommended capabilities on trmnl.com/account:\n")
                            }
                            append("• Read — devices, logs, playlists & diagnostics\n")
                            append("• Content — playlist & mashup updates\n")
                            append("• Profile — account info (GET /api/me)\n")
                            append("• Access control — Whole account (no limit)")
                        },
                    style = MaterialTheme.typography.bodySmall,
                )

                Button(
                    onClick = { uriHandler.openUri("https://trmnl.com/account") },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Create trmnl_ Key")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        painter = painterResource(R.drawable.open_in_new_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * Save token button with loading state.
 */
@Composable
private fun SaveTokenButton(
    isLoading: Boolean,
    isEnabled: Boolean,
    isLegacyToken: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonLabel =
        when {
            isLoading -> "Saving..."
            isLegacyToken -> "Continue with Legacy Key"
            else -> "Save Token"
        }

    if (isLegacyToken) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = isEnabled && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(buttonLabel)
        }
    } else {
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
            Text(buttonLabel)
        }
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
                    token = "trmnl_abc123xyz456",
                    isLoading = false,
                    errorMessage = null,
                    isLegacyToken = false,
                    eventSink = {},
                ),
        )
    }
}

/**
 * Preview of AccessTokenScreen with legacy `user_` token warning shown.
 */
@PreviewLightDark
@Composable
private fun AccessTokenContentLegacyTokenPreview() {
    TrmnlBuddyAppTheme {
        AccessTokenContent(
            state =
                AccessTokenScreen.State(
                    token = "user_abc123xyz456",
                    isLoading = false,
                    errorMessage = null,
                    isLegacyToken = true,
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
