package ink.trmnl.android.buddy.ui.playlistitems

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import ink.trmnl.android.buddy.domain.models.PlaylistItemUi
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

private const val PLAYLIST_ITEM_PREVIEW_URL_TEMPLATE = "https://trmnl.com/api/playlists/items/%d/preview"
private const val PREVIEW_ASPECT_RATIO = 800f / 480f

/**
 * Bottom sheet displaying a live rendered preview image for a playlist item
 * fetched from `GET /api/playlists/items/{item_id}/preview`.
 */
@Composable
fun PlaylistItemPreviewBottomSheet(
    item: PlaylistItemUi,
    apiToken: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var reloadNonce by remember(item.id) { mutableIntStateOf(0) }
    val previewUrl = remember(item.id) { PLAYLIST_ITEM_PREVIEW_URL_TEMPLATE.format(item.id) }
    val zoomableState = rememberZoomableState(zoomSpec = ZoomSpec(maxZoomFactor = 4f))

    val imageRequest =
        remember(previewUrl, apiToken, reloadNonce) {
            val builder =
                ImageRequest
                    .Builder(context)
                    .data(previewUrl)
                    .memoryCacheKey("playlist_preview_${item.id}_$reloadNonce")
                    .diskCachePolicy(CachePolicy.DISABLED)

            if (!apiToken.isNullOrBlank()) {
                builder.httpHeaders(
                    NetworkHeaders
                        .Builder()
                        .set("Authorization", "Bearer $apiToken")
                        .build(),
                )
            }

            builder.build()
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
    ) {
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val subtitleParts =
            buildList {
                item.pluginName?.takeIf { it != item.displayName }?.let { add(it) }
                add(if (item.isVisible) "Visible in playlist" else "Hidden")
            }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitleParts.joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_ASPECT_RATIO)
                    .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            if (apiToken.isNullOrBlank()) {
                PreviewMessageBox(
                    message = "API token is required to load private playlist previews.",
                )
            } else {
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = "Preview of ${item.displayName}",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .zoomable(zoomableState),
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "Rendering preview…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    error = {
                        PreviewMessageBox(
                            message =
                                "Unable to load preview image. " +
                                    "Ensure this item has rendered content and your API key has access.",
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pinch to zoom • Live preview from TRMNL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { reloadNonce++ },
                modifier = Modifier.weight(1f),
            ) {
                Text("Reload")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun PreviewMessageBox(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
