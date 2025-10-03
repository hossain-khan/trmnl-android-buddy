package ink.trmnl.android.buddy.ui.utils

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
        // Invert color matrix: multiplies RGB by -1 and adds 255
        // This transforms: new_color = -old_color + 255
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
    } else {
        null
    }
}
