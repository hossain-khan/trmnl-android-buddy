package ink.trmnl.android.buddy.ui.utils

import android.graphics.Bitmap
import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ColorUtils image brightness analysis functions.
 */
@RunWith(RobolectricTestRunner::class)
class ColorUtilsTest {
    @Test
    fun `calculateDarkPixelPercentage returns 1 for completely black image`() {
        // Create a completely black 10x10 bitmap
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)

        val result = calculateDarkPixelPercentage(bitmap)

        assertThat(result).isCloseTo(1.0f, 0.01f)
    }

    @Test
    fun `calculateDarkPixelPercentage returns 0 for completely white image`() {
        // Create a completely white 10x10 bitmap
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        val result = calculateDarkPixelPercentage(bitmap)

        assertThat(result).isCloseTo(0.0f, 0.01f)
    }

    @Test
    fun `calculateDarkPixelPercentage returns approximately 0_5 for half dark image`() {
        // Create a 10x10 bitmap with half black, half white
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        // Fill top half with black, bottom half with white
        for (y in 0 until 10) {
            for (x in 0 until 10) {
                val color = if (y < 5) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                bitmap.setPixel(x, y, color)
            }
        }

        val result = calculateDarkPixelPercentage(bitmap)

        // Should be close to 0.5, allowing some margin for sampling
        assertThat(result).isCloseTo(0.5f, 0.15f)
    }

    @Test
    fun `calculateDarkPixelPercentage ignores fully transparent pixels`() {
        // Create a 10x10 bitmap with transparent black
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.TRANSPARENT)

        val result = calculateDarkPixelPercentage(bitmap)

        // Should be 0 since all pixels are transparent
        assertThat(result).isEqualTo(0.0f)
    }

    @Test
    fun `isBitmapPredominantlyDark returns true for mostly dark image`() {
        // Create a 10x10 bitmap that's 90% black
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        for (y in 0 until 10) {
            for (x in 0 until 10) {
                // 90 out of 100 pixels are black
                val color =
                    if ((y * 10 + x) < 90) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                bitmap.setPixel(x, y, color)
            }
        }

        val result = isBitmapPredominantlyDark(bitmap, darkPercentageThreshold = 0.8f)

        assertThat(result).isTrue()
    }

    @Test
    fun `isBitmapPredominantlyDark returns false for mostly light image`() {
        // Create a 10x10 bitmap that's 70% white
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        for (y in 0 until 10) {
            for (x in 0 until 10) {
                // 30 out of 100 pixels are black
                val color =
                    if ((y * 10 + x) < 30) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                bitmap.setPixel(x, y, color)
            }
        }

        val result = isBitmapPredominantlyDark(bitmap, darkPercentageThreshold = 0.8f)

        assertThat(result).isFalse()
    }

    @Test
    fun `isBitmapPredominantlyDark respects custom threshold`() {
        // Create a 10x10 bitmap that's 50% black
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        for (y in 0 until 10) {
            for (x in 0 until 10) {
                val color =
                    if ((y * 10 + x) < 50) {
                        android.graphics.Color.BLACK
                    } else {
                        android.graphics.Color.WHITE
                    }
                bitmap.setPixel(x, y, color)
            }
        }

        // With 80% threshold, should be false
        assertThat(isBitmapPredominantlyDark(bitmap, darkPercentageThreshold = 0.8f)).isFalse()

        // With 40% threshold, should be true
        assertThat(isBitmapPredominantlyDark(bitmap, darkPercentageThreshold = 0.4f)).isTrue()
    }

    @Test
    fun `isBitmapPredominantlyDark handles dark gray as dark`() {
        // Create a bitmap with dark gray (RGB: 50, 50, 50)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val darkGray = android.graphics.Color.rgb(50, 50, 50)
        bitmap.eraseColor(darkGray)

        val result = isBitmapPredominantlyDark(bitmap, darkPercentageThreshold = 0.8f)

        // Dark gray should be considered dark (brightness < 127)
        assertThat(result).isTrue()
    }

    @Test
    fun `isBitmapPredominantlyDark handles light gray as light`() {
        // Create a bitmap with light gray (RGB: 200, 200, 200)
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val lightGray = android.graphics.Color.rgb(200, 200, 200)
        bitmap.eraseColor(lightGray)

        val result = isBitmapPredominantlyDark(bitmap, darkPercentageThreshold = 0.8f)

        // Light gray should be considered light (brightness > 127)
        assertThat(result).isFalse()
    }
}
