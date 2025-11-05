package ink.trmnl.android.buddy.ui.utils

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * A Coil transformation that intelligently inverts dark images in dark mode.
 *
 * This transformation analyzes the input image to determine if it's predominantly dark
 * (80% or more dark pixels). If the image is dark and dark mode is enabled, it inverts
 * the colors to make the image more visible.
 *
 * This is particularly useful for app icons and logos that may be dark and hard to see
 * against a dark background.
 *
 * @param isDarkMode Whether dark mode is currently enabled
 * @param darkPercentageThreshold The threshold percentage (0.0 to 1.0) of dark pixels required for inversion. Default is 0.8 (80%).
 * @param brightnessThreshold The brightness threshold below which a pixel is considered dark (0-255). Default is 127.
 * @param sampleSize The number of pixels to sample when analyzing. Default is 100.
 */
class SmartInvertTransformation(
    private val isDarkMode: Boolean,
    private val darkPercentageThreshold: Float = 0.8f,
    private val brightnessThreshold: Int = 127,
    private val sampleSize: Int = 100,
) : Transformation() {
    override val cacheKey: String =
        "smart_invert_dark=$isDarkMode,threshold=$darkPercentageThreshold,brightness=$brightnessThreshold"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        // Only analyze and potentially transform in dark mode
        if (!isDarkMode) {
            return input
        }

        // Analyze the image to determine if it's predominantly dark
        val isPredominantlyDark =
            isBitmapPredominantlyDark(
                bitmap = input,
                darkPercentageThreshold = darkPercentageThreshold,
                brightnessThreshold = brightnessThreshold,
                sampleSize = sampleSize,
            )

        // If not predominantly dark, return the original image
        if (!isPredominantlyDark) {
            return input
        }

        // Create a new bitmap with inverted colors
        val output = Bitmap.createBitmap(input.width, input.height, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)

        // Create color inversion matrix
        val invertMatrix =
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
            )

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(invertMatrix)

        canvas.drawBitmap(input, 0f, 0f, paint)

        return output
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SmartInvertTransformation) return false

        if (isDarkMode != other.isDarkMode) return false
        if (darkPercentageThreshold != other.darkPercentageThreshold) return false
        if (brightnessThreshold != other.brightnessThreshold) return false
        if (sampleSize != other.sampleSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isDarkMode.hashCode()
        result = 31 * result + darkPercentageThreshold.hashCode()
        result = 31 * result + brightnessThreshold
        result = 31 * result + sampleSize
        return result
    }
}
