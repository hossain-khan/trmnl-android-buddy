package ink.trmnl.android.buddy.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import ink.trmnl.android.buddy.BuildConfig
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.Dimens
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import timber.log.Timber

/**
 * App information section showing version and links to GitHub.
 */
@Composable
fun AppInformationSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        Text(
            text = "App Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        ElevatedCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.paddingSmall),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = Dimens.elevationSmall),
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
                            contentDescription = "Version information",
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
                            try {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/hossain-khan/trmnl-android-buddy/releases/".toUri(),
                                    )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to open releases page")
                            }
                        },
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
                            try {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://github.com/hossain-khan/trmnl-android-buddy/issues".toUri(),
                                    )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to open GitHub repository")
                            }
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
private fun AppInformationSectionPreview() {
    TrmnlBuddyAppTheme {
        AppInformationSection()
    }
}
