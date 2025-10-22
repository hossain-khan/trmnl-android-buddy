package ink.trmnl.android.buddy.ui.utils

import android.graphics.Bitmap
import android.graphics.Color
import assertk.assertThat
import assertk.assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ImageAnalyzer.
 *
 * Tests image analysis functionality for determining if images are dark-heavy or light-heavy.
 */
@RunWith(RobolectricTestRunner::class)
class ImageAnalyzerTest {
    @Test
    fun `isDarkHeavy returns true for predominantly dark image`() {
        // Create a bitmap with 75% dark pixels
        val bitmap = createTestBitmap(width = 100, height = 100, darkRatio = 0.75f)

        val result = ImageAnalyzer.isDarkHeavy(bitmap)

        assertThat(result).isTrue()
    }

    @Test
    fun `isDarkHeavy returns false for predominantly light image`() {
        // Create a bitmap with 25% dark pixels (75% light)
        val bitmap = createTestBitmap(width = 100, height = 100, darkRatio = 0.25f)

        val result = ImageAnalyzer.isDarkHeavy(bitmap)

        assertThat(result).isFalse()
    }

    @Test
    fun `getAverageLuminance returns correct value for black image`() {
        val bitmap = createSolidColorBitmap(width = 100, height = 100, color = Color.BLACK)

        val avgLuminance = ImageAnalyzer.getAverageLuminance(bitmap)

        assertThat(avgLuminance).isEqualTo(0)
    }

    @Test
    fun `getAverageLuminance returns correct value for white image`() {
        val bitmap = createSolidColorBitmap(width = 100, height = 100, color = Color.WHITE)

        val avgLuminance = ImageAnalyzer.getAverageLuminance(bitmap)

        assertThat(avgLuminance).isEqualTo(255)
    }

    @Test
    fun `analyzeImage returns correct stats for mixed image`() {
        // Create a bitmap with 60% dark pixels
        val bitmap = createTestBitmap(width = 200, height = 100, darkRatio = 0.6f)

        val stats = ImageAnalyzer.analyzeImage(bitmap, sampleRate = 1)

        assertThat(stats.width).isEqualTo(200)
        assertThat(stats.height).isEqualTo(100)
        assertThat(stats.isDarkHeavy).isTrue()
        assertThat(stats.darkPixelPercentage).isBetween(55, 65)
        assertThat(stats.minLuminance).isEqualTo(0)
        assertThat(stats.maxLuminance).isEqualTo(255)
    }

    @Test
    fun `analyzeImage with 1-bit grayscale detects black and white only`() {
        // Simulate 1-bit grayscale: only pure black (0) and pure white (255)
        val bitmap = create1BitGrayscaleBitmap(width = 800, height = 480, darkRatio = 0.3f)

        val stats = ImageAnalyzer.analyzeImage(bitmap, sampleRate = 10)

        assertThat(stats.minLuminance).isEqualTo(0)
        assertThat(stats.maxLuminance).isEqualTo(255)
        assertThat(stats.isDarkHeavy).isFalse() // 30% dark, 70% light
    }

    @Test
    fun `analyzeImage with 2-bit grayscale detects four shades`() {
        // Simulate 2-bit grayscale: 0 (black), 85 (dark gray), 170 (light gray), 255 (white)
        val bitmap = create2BitGrayscaleBitmap(width = 800, height = 480)

        val stats = ImageAnalyzer.analyzeImage(bitmap, sampleRate = 10)

        assertThat(stats.minLuminance).isLessThanOrEqualTo(85)
        assertThat(stats.maxLuminance).isGreaterThanOrEqualTo(170)
    }

    // Helper functions

    private fun createTestBitmap(
        width: Int,
        height: Int,
        darkRatio: Float,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val darkPixels = (width * height * darkRatio).toInt()

        var pixelCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = if (pixelCount < darkPixels) Color.BLACK else Color.WHITE
                bitmap.setPixel(x, y, color)
                pixelCount++
            }
        }

        return bitmap
    }

    private fun createSolidColorBitmap(
        width: Int,
        height: Int,
        color: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun create1BitGrayscaleBitmap(
        width: Int,
        height: Int,
        darkRatio: Float,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val darkPixels = (width * height * darkRatio).toInt()

        var pixelCount = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                // 1-bit: only black (0) or white (255)
                val luminance = if (pixelCount < darkPixels) 0 else 255
                val color = Color.rgb(luminance, luminance, luminance)
                bitmap.setPixel(x, y, color)
                pixelCount++
            }
        }

        return bitmap
    }

    private fun create2BitGrayscaleBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 2-bit grayscale: 4 shades (0, 85, 170, 255)
        val shades = listOf(0, 85, 170, 255)
        var shadeIndex = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val luminance = shades[shadeIndex % shades.size]
                val color = Color.rgb(luminance, luminance, luminance)
                bitmap.setPixel(x, y, color)
                shadeIndex++
            }
        }

        return bitmap
    }
}
