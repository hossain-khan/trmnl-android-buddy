package ink.trmnl.android.buddy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.R
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Extras section for accessing additional features like device catalog and recipes.
 */
@Composable
fun ExtrasSection(
    onDeviceCatalogClick: () -> Unit,
    onRecipesCatalogClick: () -> Unit,
    onContentHubClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.design_services_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Extras",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column {
                // Supported Device Catalog
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Supported Device Catalog",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "View all supported TRMNL device models and specifications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.devices_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp),
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Navigate to Supported Device Catalog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    modifier = Modifier.clickable { onDeviceCatalogClick() },
                )

                // Recipes Catalog
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Recipes Catalog",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Browse and discover popular plugin recipes and configurations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.chef_hat_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp),
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Navigate to Recipes Catalog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    modifier = Modifier.clickable { onRecipesCatalogClick() },
                )

                // Content Hub
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Content Hub",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "View announcements and blog posts from TRMNL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.campaign_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp),
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                            contentDescription = "Navigate to Content Hub",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors =
                        ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    modifier = Modifier.clickable { onContentHubClick() },
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
private fun ExtrasSectionPreview() {
    TrmnlBuddyAppTheme {
        ExtrasSection(
            onDeviceCatalogClick = {},
            onRecipesCatalogClick = {},
            onContentHubClick = {},
        )
    }
}
