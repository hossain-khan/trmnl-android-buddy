package ink.trmnl.android.buddy.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThanOrEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WidgetBitmapUtilsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `calculateWidgetMemoryCapBytes returns positive value`() {
        val capBytes = WidgetBitmapUtils.calculateWidgetMemoryCapBytes(context)
        assertThat(capBytes).isGreaterThan(0L)
    }

    @Test
    fun `decodeDownsampledWidgetBitmap returns null when file does not exist`() {
        val missingFile = File(context.cacheDir, "does_not_exist.png")
        val result =
            WidgetBitmapUtils.decodeDownsampledWidgetBitmap(
                context = context,
                filePath = missingFile.absolutePath,
                maxWidthDp = 400,
                maxHeightDp = 300,
            )
        assertThat(result).isNull()
    }

    @Test
    fun `decodeDownsampledWidgetBitmap downsamples oversized image`() {
        val imageFile = File(context.cacheDir, "oversized_test.png")
        val largeBitmap = Bitmap.createBitmap(2400, 1800, Bitmap.Config.ARGB_8888)
        imageFile.outputStream().use { out ->
            largeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        largeBitmap.recycle()

        try {
            val decoded =
                WidgetBitmapUtils.decodeDownsampledWidgetBitmap(
                    context = context,
                    filePath = imageFile.absolutePath,
                    maxWidthDp = 400,
                    maxHeightDp = 300,
                    distinctImageCount = 1,
                    config = Bitmap.Config.RGB_565,
                )
            assertThat(decoded).isNotNull()
            assertThat(decoded!!.width).isLessThanOrEqualTo(1200)
            assertThat(decoded.height).isLessThanOrEqualTo(900)
        } finally {
            imageFile.delete()
        }
    }
}
