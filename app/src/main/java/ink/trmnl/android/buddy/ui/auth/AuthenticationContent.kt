package ink.trmnl.android.buddy.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * UI content for AuthenticationScreen.
 */
@CircuitInject(AuthenticationScreen::class, AppScope::class)
@Composable
fun AuthenticationContent(
    state: AuthenticationScreen.State,
    modifier: Modifier = Modifier,
) {
    // Note: BiometricPrompt requires explicit user interaction (button click) per Android guidelines
    // We cannot auto-trigger authenticate() - user must click the button first
    // See: https://developer.android.com/identity/sign-in/biometric-auth

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isAuthenticationAvailable) {
                AuthenticationCard(
                    showRetryPrompt = state.showRetryPrompt,
                    onAuthenticateClick = {
                        state.eventSink(AuthenticationScreen.Event.AuthenticateRequested)
                    },
                    onCancelClick = {
                        state.eventSink(AuthenticationScreen.Event.CancelAuthentication)
                    },
                )
            } else {
                NoAuthenticationAvailableCard(
                    onDisableSecurity = {
                        state.eventSink(AuthenticationScreen.Event.CancelAuthentication)
                    },
                )
            }
        }
    }
}

/**
 * Card shown when authentication is available.
 * Following Android guidelines: BiometricPrompt requires explicit user interaction (button click).
 */
@Composable
private fun AuthenticationCard(
    showRetryPrompt: Boolean,
    onAuthenticateClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.fingerprint_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "Authentication",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Authentication Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Use your fingerprint, face, or device PIN to unlock your TRMNL dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Always show authenticate button - BiometricPrompt requires user interaction
            Button(
                onClick = onAuthenticateClick,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.fingerprint_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Unlock")
            }
        }
    }
}

/**
 * Card shown when no authentication method is available on the device.
 */
@Composable
private fun NoAuthenticationAvailableCard(
    onDisableSecurity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.password_2_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = "No authentication available",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )

            Text(
                text = "No Authentication Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text =
                    "Your device doesn't have a screen lock (PIN, pattern, password, or biometric) set up. " +
                        "Please set up a screen lock in your device settings to use this feature.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDisableSecurity,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text("Disable Security")
            }
        }
    }
}

// ========== Previews ==========

@Preview(name = "Authentication Card - Initial")
@Composable
private fun AuthenticationCardPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationCard(
            showRetryPrompt = false,
            onAuthenticateClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "Authentication Card - Retry")
@Composable
private fun AuthenticationCardRetryPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationCard(
            showRetryPrompt = true,
            onAuthenticateClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "No Authentication Available")
@Composable
private fun NoAuthenticationAvailableCardPreview() {
    TrmnlBuddyAppTheme {
        NoAuthenticationAvailableCard(
            onDisableSecurity = {},
        )
    }
}

@Preview(name = "Full Screen - Available")
@Composable
private fun AuthenticationContentPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isAuthenticationAvailable = true,
                    showRetryPrompt = false,
                ),
        )
    }
}

@Preview(name = "Full Screen - Retry")
@Composable
private fun AuthenticationContentRetryPreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isAuthenticationAvailable = true,
                    showRetryPrompt = true,
                ),
        )
    }
}

@Preview(name = "Full Screen - Not Available")
@Composable
private fun AuthenticationContentNotAvailablePreview() {
    TrmnlBuddyAppTheme {
        AuthenticationContent(
            state =
                AuthenticationScreen.State(
                    isAuthenticationAvailable = false,
                    showRetryPrompt = false,
                ),
        )
    }
}
