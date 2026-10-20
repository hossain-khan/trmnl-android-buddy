package ink.trmnl.android.buddy.ui.recipescatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.api.models.AuthorBio
import ink.trmnl.android.buddy.api.models.Recipe
import ink.trmnl.android.buddy.api.models.RecipeStats
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme
import ink.trmnl.android.buddy.ui.utils.SmartInvertTransformation
import ink.trmnl.android.buddy.ui.utils.htmlToAnnotatedString

/**
 * Bottom sheet displaying detailed information about a recipe.
 *
 * Shows the recipe screenshot, name, description, author information, and statistics.
 *
 * @param recipe The recipe to display details for
 * @param sheetState State of the bottom sheet
 * @param onDismiss Callback when the bottom sheet is dismissed
 * @param onInstallClick Callback when the install button is clicked
 * @param isInstalling Whether installation is currently in progress
 * @param targetDeviceName Optional pre-selected device name for install button label
 * @param installErrorMessage Error message to display inline if recipe installation failed
 * @param onDismissInstallError Callback to dismiss the inline installation error banner
 * @param modifier Optional modifier for the component
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailBottomSheet(
    recipe: Recipe,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onInstallClick: () -> Unit = {},
    isInstalling: Boolean = false,
    targetDeviceName: String? = null,
    installErrorMessage: String? = null,
    onDismissInstallError: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
        ) {
            // Recipe name
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Stats
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${recipe.stats.installs} installs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fork_right_24dp_999999_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "${recipe.stats.forks} forks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Categories
            val categories =
                recipe.authorBio
                    ?.category
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()

            if (categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { category ->
                        Text(
                            text = category.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier =
                                Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                    ).padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Install button
            Button(
                onClick = onInstallClick,
                enabled = !isInstalling,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp),
            ) {
                if (isInstalling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Installing…")
                } else {
                    Icon(
                        painter = painterResource(R.drawable.download_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val label =
                        if (targetDeviceName != null) {
                            "Install to $targetDeviceName"
                        } else {
                            "Install to Device…"
                        }
                    Text(label)
                }
            }

            // Inline install error banner
            if (installErrorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.info_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "Installation Failed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            if (onDismissInstallError != null) {
                                IconButton(
                                    onClick = onDismissInstallError,
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close_24dp_e3e3e3_fill0_wght400_grad0_opsz24),
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = installErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )

                        val uriHandler = LocalUriHandler.current
                        if (installErrorMessage.contains("https://trmnl.com/account") ||
                            installErrorMessage.contains("account", ignoreCase = true)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { uriHandler.openUri("https://trmnl.com/account") },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Open TRMNL Account")
                            }
                        }
                    }
                }
            }

            // Recipe screenshot
            if (recipe.screenshotUrl != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val isDarkMode = isSystemInDarkTheme()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    SubcomposeAsyncImage(
                        model =
                            ImageRequest
                                .Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(recipe.screenshotUrl)
                                .transformations(SmartInvertTransformation(isDarkMode))
                                .build(),
                        contentDescription = "Screenshot of ${recipe.name}",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.FillWidth,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        error = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.widgets_24dp_e8eaed_fill0_wght200_grad0_opsz24),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Screenshot unavailable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }

            // Description
            val description = recipe.authorBio?.description
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Render HTML description with clickable links
                val uriHandler = LocalUriHandler.current
                val linkColor = MaterialTheme.colorScheme.primary
                val annotatedDescription =
                    remember(description, linkColor) {
                        htmlToAnnotatedString(
                            html = description,
                            linkColor = linkColor,
                        )
                    }

                ClickableText(
                    text = annotatedDescription,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    onClick = { offset ->
                        annotatedDescription
                            .getStringAnnotations(
                                tag = "URL",
                                start = offset,
                                end = offset,
                            ).firstOrNull()
                            ?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    },
                )
            }

            // Author info
            val authorBio = recipe.authorBio
            if (authorBio != null) {
                val hasAuthorInfo =
                    !authorBio.name.isNullOrBlank() ||
                        !authorBio.keyname.isNullOrBlank()

                if (hasAuthorInfo) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Author",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val authorName = authorBio.name
                    if (authorName != null && authorName.isNotBlank()) {
                        Text(
                            text = authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// Composable Previews
// ============================================

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun RecipeDetailBottomSheetPreview() {
    TrmnlBuddyAppTheme {
        RecipeDetailBottomSheet(
            recipe =
                Recipe(
                    id = 11023,
                    name = "Simple Calendar",
                    iconUrl = "https://trmnl-public.s3.us-east-2.amazonaws.com/1xsrzlkit2xlu9uyqbp8pevppwpl",
                    screenshotUrl = "https://trmnl.s3.us-east-2.amazonaws.com/screenshot.png",
                    authorBio =
                        AuthorBio(
                            keyname = "doesnt_matter",
                            name = "About This Plugin",
                            category = "calendar,custom",
                            fieldType = "author_bio",
                            description =
                                "Your customizable desktop companion. Supports multiple languages, " +
                                    "worldwide holidays, and secondary calendars.",
                        ),
                    stats = RecipeStats(installs = 68, forks = 1370),
                ),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun RecipeDetailBottomSheetNoScreenshotPreview() {
    TrmnlBuddyAppTheme {
        RecipeDetailBottomSheet(
            recipe =
                Recipe(
                    id = 619,
                    name = "Dad Jokes",
                    iconUrl = null,
                    screenshotUrl = null,
                    authorBio =
                        AuthorBio(
                            keyname = "doesnt_matter",
                            name = "About This Plugin",
                            category = "humor,entertainment",
                            fieldType = "author_bio",
                            description = "Get a daily dose of dad jokes delivered to your TRMNL display!",
                        ),
                    stats = RecipeStats(installs = 1008, forks = 31),
                ),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun RecipeDetailBottomSheetErrorPreview() {
    TrmnlBuddyAppTheme {
        RecipeDetailBottomSheet(
            recipe =
                Recipe(
                    id = 11023,
                    name = "Simple Calendar",
                    iconUrl = null,
                    screenshotUrl = null,
                    authorBio = null,
                    stats = RecipeStats(installs = 68, forks = 1370),
                ),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = {},
            installErrorMessage =
                "This is a legacy API key, which reaches only the endpoints it had before API keys held capabilities. Create an API key with the capabilities you need at https://trmnl.com/account.",
            onDismissInstallError = {},
        )
    }
}
