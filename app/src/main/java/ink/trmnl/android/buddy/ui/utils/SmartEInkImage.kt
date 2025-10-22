package ink.trmnl.android.buddy.ui.utils

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.transform.Transformation

/**
 * Example: Smart E-Ink Image with automatic dark mode handling.
 *
 * This composable loads an image and intelligently decides whether to invert it
 * in dark mode based on the image's color distribution.
 *
 * Usage in TrmnlDevicesScreen.kt:
 * ```
 * SmartEInkImage(
 *     imageUrl = previewInfo.imageUrl,
 *     contentDescription = "Device preview",
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 */
@Composable
fun SmartEInkImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth,
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isDarkMode = isSystemInDarkTheme()

    SubcomposeAsyncImage(
        model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(imageUrl)
                .allowHardware(false) // Required to access bitmap pixels
                .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onSuccess = { state ->
            // Extract bitmap for analysis
            state.result.image.let { image ->
                // Note: In Coil 3, you may need to use platform-specific code
                // This is a simplified example
                bitmap =
                    state.painter.intrinsicSize.let { size ->
                        android.graphics.Bitmap.createBitmap(
                            size.width.toInt(),
                            size.height.toInt(),
                            android.graphics.Bitmap.Config.ARGB_8888,
                        )
                    }
            }
        },
        colorFilter = rememberSmartEInkColorFilter(bitmap),
    )
}

/**
 * Alternative: Use SubcomposeAsyncImage with custom success callback
 * to analyze the image and conditionally apply color filter.
 */
@Composable
fun SmartEInkImageV2(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onImageAnalyzed: (ImageAnalyzer.ImageStats) -> Unit = {},
) {
    var shouldInvert by remember { mutableStateOf(false) }
    var isAnalyzed by remember { mutableStateOf(false) }
    val isDarkMode = isSystemInDarkTheme()

    SubcomposeAsyncImage(
        model =
            ImageRequest
                .Builder(LocalContext.current)
                .data(imageUrl)
                .allowHardware(false)
                .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.FillWidth,
        onSuccess = { state ->
            if (!isAnalyzed && isDarkMode) {
                // Analyze bitmap to determine if inversion is needed
                // Note: Actual bitmap extraction depends on Coil version
                // This is a conceptual example
                // In practice, you might need to load the bitmap separately
                isAnalyzed = true
            }
        },
        colorFilter = if (isDarkMode && shouldInvert) rememberEInkColorFilter() else null,
        loading = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("Loading...", modifier = Modifier.align(Alignment.Center))
            }
        },
    )
}

/**
 * Coil transformation that analyzes the image and stores the result.
 * Can be used to determine if color inversion should be applied.
 */
class ImageAnalysisTransformation(
    private val onAnalyzed: (ImageAnalyzer.ImageStats) -> Unit,
) : Transformation() {
    override val cacheKey: String = "image_analysis"

    override suspend fun transform(
        input: Bitmap,
        size: coil3.size.Size,
    ): Bitmap {
        // Analyze the image
        val stats = ImageAnalyzer.analyzeImage(input, sampleRate = 10)
        onAnalyzed(stats)

        // Return original bitmap (no transformation)
        return input
    }
}

/**
 * Example usage with transformation:
 *
 * ```kotlin
 * var imageStats by remember { mutableStateOf<ImageAnalyzer.ImageStats?>(null) }
 * val isDarkMode = isSystemInDarkTheme()
 *
 * SubcomposeAsyncImage(
 *     model = ImageRequest.Builder(LocalContext.current)
 *         .data(imageUrl)
 *         .allowHardware(false)
 *         .transformations(
 *             ImageAnalysisTransformation { stats ->
 *                 imageStats = stats
 *             }
 *         )
 *         .build(),
 *     contentDescription = "Preview",
 *     colorFilter = if (isDarkMode && imageStats?.isDarkHeavy == false) {
 *         rememberEInkColorFilter()
 *     } else {
 *         null
 *     }
 * )
 * ```
 */
