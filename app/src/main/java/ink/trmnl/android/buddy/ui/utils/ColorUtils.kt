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

/**
 * Analyzes a bitmap to determine if it's predominantly dark.
 * Samples pixels across the image and calculates the percentage of dark pixels.
 *
 * @param bitmap The bitmap to analyze
 * @param darknessThreshold The brightness threshold below which a pixel is considered dark (0-255). Default is 127.
 * @param sampleSize The number of pixels to sample. Higher values are more accurate but slower. Default is 100.
 * @return The percentage of dark pixels (0.0 to 1.0)
 */
fun calculateDarkPixelPercentage(
    bitmap: Bitmap,
    darknessThreshold: Int = 127,
    sampleSize: Int = 100,
): Float {
    val width = bitmap.width
    val height = bitmap.height

    if (width == 0 || height == 0) return 0f

    var darkPixelCount = 0
    val actualSampleSize = minOf(sampleSize, width * height)

    // Calculate step sizes to evenly sample across the image
    val stepX = maxOf(1, width / kotlin.math.sqrt(actualSampleSize.toDouble()).toInt())
    val stepY = maxOf(1, height / kotlin.math.sqrt(actualSampleSize.toDouble()).toInt())

    var totalSampled = 0

    for (y in 0 until height step stepY) {
        for (x in 0 until width step stepX) {
            val pixel = bitmap.getPixel(x, y)

            // Extract RGB components
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val alpha = (pixel shr 24) and 0xFF

            // Skip fully transparent pixels
            if (alpha < 10) continue

            // Calculate perceived brightness using standard luminance formula
            // Human eye is more sensitive to green, less to blue
            val brightness = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()

            if (brightness < darknessThreshold) {
                darkPixelCount++
            }
            totalSampled++
        }
    }

    return if (totalSampled > 0) {
        darkPixelCount.toFloat() / totalSampled.toFloat()
    } else {
        0f
    }
}

/**
 * Checks if a bitmap is predominantly dark (80% or more dark pixels).
 *
 * @param bitmap The bitmap to analyze
 * @param darkPercentageThreshold The threshold percentage (0.0 to 1.0) of dark pixels required. Default is 0.8 (80%).
 * @param brightnessThreshold The brightness threshold below which a pixel is considered dark (0-255). Default is 127.
 * @param sampleSize The number of pixels to sample. Default is 100.
 * @return true if the image is predominantly dark, false otherwise
 */
fun isBitmapPredominantlyDark(
    bitmap: Bitmap,
    darkPercentageThreshold: Float = 0.8f,
    brightnessThreshold: Int = 127,
    sampleSize: Int = 100,
): Boolean {
    val darkPercentage = calculateDarkPixelPercentage(bitmap, brightnessThreshold, sampleSize)
    return darkPercentage >= darkPercentageThreshold
}
