package ink.trmnl.android.buddy.ui.recipescatalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import ink.trmnl.android.buddy.ui.theme.TrmnlBuddyAppTheme

/**
 * Button component for loading more recipes in pagination.
 *
 * Shows either a loading indicator or "Load More" text based on loading state.
 * Uses Material 3 Card for consistent styling with the rest of the UI.
 *
 * @param isLoading True when loading next page, shows spinner
 * @param onClick Callback when the button is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun LoadMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading, onClick = onClick)
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "Load More",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
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
private fun LoadMoreButtonPreview() {
    TrmnlBuddyAppTheme {
        LoadMoreButton(
            isLoading = false,
            onClick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoadMoreButtonLoadingPreview() {
    TrmnlBuddyAppTheme {
        LoadMoreButton(
            isLoading = true,
            onClick = {},
        )
    }
}
