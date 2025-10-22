package ink.trmnl.android.buddy.ui.utils

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Utility object for analyzing image properties to determine optimal display settings.
 */
object ImageAnalyzer {
    /**
     * Analyzes an image to determine if it's predominantly dark (more dark pixels than light).
     *
     * This is useful for e-ink display images which can be either:
     * - Light-heavy: Black text on white background (typical e-ink)
     * - Dark-heavy: White text on black background or inverted images
     *
     * For 1-bit grayscale: Only pure black (0) and pure white (255)
     * For 2-bit grayscale: Black (0), dark gray (85), light gray (170), white (255)
     *
     * @param bitmap The bitmap to analyze
     * @param sampleRate Sample every Nth pixel (default: 10 for performance)
     * @param threshold Luminance threshold to consider a pixel "dark" (0-255, default: 128)
     * @return true if the image has more dark pixels than light pixels
     */
    fun isDarkHeavy(
        bitmap: Bitmap,
        sampleRate: Int = 10,
        threshold: Int = 128,
    ): Boolean {
        var darkPixels = 0
        var lightPixels = 0
        var sampledPixels = 0

        // Sample pixels across the image for performance
        for (y in 0 until bitmap.height step sampleRate) {
            for (x in 0 until bitmap.width step sampleRate) {
                val pixel = bitmap.getPixel(x, y)

                // Get luminance (brightness) of the pixel
                // For grayscale, R=G=B, so we can just use red channel
                val luminance = Color.red(pixel)

                if (luminance < threshold) {
                    darkPixels++
                } else {
                    lightPixels++
                }
                sampledPixels++
            }
        }

        // Consider dark-heavy if more than 50% of pixels are below threshold
        return darkPixels > lightPixels
    }

    /**
     * Calculates the average luminance (brightness) of an image.
     *
     * @param bitmap The bitmap to analyze
     * @param sampleRate Sample every Nth pixel (default: 10 for performance)
     * @return Average luminance value between 0 (pure black) and 255 (pure white)
     */
    fun getAverageLuminance(
        bitmap: Bitmap,
        sampleRate: Int = 10,
    ): Int {
        var totalLuminance = 0
        var sampledPixels = 0

        for (y in 0 until bitmap.height step sampleRate) {
            for (x in 0 until bitmap.width step sampleRate) {
                val pixel = bitmap.getPixel(x, y)
                totalLuminance += Color.red(pixel)
                sampledPixels++
            }
        }

        return if (sampledPixels > 0) totalLuminance / sampledPixels else 128
    }

    /**
     * Analyzes image statistics for detailed information.
     *
     * @param bitmap The bitmap to analyze
     * @param sampleRate Sample every Nth pixel (default: 10 for performance)
     * @return ImageStats containing detailed statistics
     */
    fun analyzeImage(
        bitmap: Bitmap,
        sampleRate: Int = 10,
    ): ImageStats {
        var totalLuminance = 0
        var darkPixels = 0
        var lightPixels = 0
        var minLuminance = 255
        var maxLuminance = 0
        var sampledPixels = 0

        for (y in 0 until bitmap.height step sampleRate) {
            for (x in 0 until bitmap.width step sampleRate) {
                val pixel = bitmap.getPixel(x, y)
                val luminance = Color.red(pixel)

                totalLuminance += luminance

                if (luminance < 128) {
                    darkPixels++
                } else {
                    lightPixels++
                }

                if (luminance < minLuminance) minLuminance = luminance
                if (luminance > maxLuminance) maxLuminance = luminance

                sampledPixels++
            }
        }

        val averageLuminance = if (sampledPixels > 0) totalLuminance / sampledPixels else 128
        val darkPercentage = if (sampledPixels > 0) (darkPixels * 100) / sampledPixels else 50

        return ImageStats(
            width = bitmap.width,
            height = bitmap.height,
            averageLuminance = averageLuminance,
            minLuminance = minLuminance,
            maxLuminance = maxLuminance,
            darkPixelPercentage = darkPercentage,
            isDarkHeavy = darkPixels > lightPixels,
            sampledPixels = sampledPixels,
        )
    }

    /**
     * Data class containing image statistics.
     */
    data class ImageStats(
        val width: Int,
        val height: Int,
        val averageLuminance: Int,
        val minLuminance: Int,
        val maxLuminance: Int,
        val darkPixelPercentage: Int,
        val isDarkHeavy: Boolean,
        val sampledPixels: Int,
    ) {
        override fun toString(): String =
            """
            ImageStats(
                size: ${width}x$height,
                avg luminance: $averageLuminance,
                range: $minLuminance-$maxLuminance,
                dark pixels: $darkPixelPercentage%,
                isDarkHeavy: $isDarkHeavy,
                samples: $sampledPixels
            )
            """.trimIndent()
    }
}
