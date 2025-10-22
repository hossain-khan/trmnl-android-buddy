package ink.trmnl.android.buddy.ui.utils

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Returns a color filter that inverts colors in dark mode for better visibility
 * of e-ink display images (which are typically black text on white background).
 *
 * @return ColorFilter that inverts colors in dark mode, null in light mode
 */
@Composable
fun rememberEInkColorFilter(): ColorFilter? {
    val isDarkMode = isSystemInDarkTheme()
    return if (isDarkMode) {
        getInvertColorFilter()
    } else {
        null
    }
}

/**
 * Returns a smart color filter that only inverts e-ink images in dark mode
 * if the image is predominantly light (typical e-ink with black text on white).
 *
 * Dark-heavy images (white text on black background) won't be inverted
 * as they're already suitable for dark mode.
 *
 * @param bitmap The image bitmap to analyze (optional)
 * @return ColorFilter that conditionally inverts colors, or null
 */
@Composable
fun rememberSmartEInkColorFilter(bitmap: Bitmap?): ColorFilter? {
    val isDarkMode = isSystemInDarkTheme()

    if (!isDarkMode) {
        return null
    }

    // If bitmap is available, analyze it
    if (bitmap != null) {
        val isDarkHeavy = ImageAnalyzer.isDarkHeavy(bitmap)
        // Only invert if image is light-heavy (typical e-ink)
        return if (!isDarkHeavy) {
            getInvertColorFilter()
        } else {
            null
        }
    }

    // Default behavior: invert in dark mode if bitmap not available
    return getInvertColorFilter()
}

/**
 * Creates and returns an invert color filter.
 * This transforms colors: new_color = -old_color + 255
 */
private fun getInvertColorFilter(): ColorFilter =
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                -1f,
                0f,
                0f,
                0f,
                255f,
                0f,
                -1f,
                0f,
                0f,
                255f,
                0f,
                0f,
                -1f,
                0f,
                255f,
                0f,
                0f,
                0f,
                1f,
                0f, // Keep alpha unchanged
            ),
        ),
    )
