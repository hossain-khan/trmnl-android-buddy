package ink.trmnl.android.buddy.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.view.WindowManager
import java.io.File
import kotlin.math.max

/**
 * Helpers for estimating the system `RemoteViews` bitmap memory cap and decoding
 * downsampled widget bitmaps within Android 17+ (`targetSdk = 37`) memory limits.
 */
object WidgetBitmapUtils {
    /**
     * Estimates the per-update `RemoteViews` bitmap memory ceiling (`6 * width * height` bytes).
     *
     * Uses `WindowManager.currentWindowMetrics.bounds` on API 30+ and falls back to
     * `resources.displayMetrics` on API 26–29 or when `WindowManager` is unavailable.
     */
    fun calculateWidgetMemoryCapBytes(context: Context): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds =
                context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                return 6L * bounds.width().toLong() * bounds.height().toLong()
            }
        }
        val dm = context.resources.displayMetrics
        return 6L * dm.widthPixels.toLong() * dm.heightPixels.toLong()
    }

    /**
     * Decodes [filePath] into a [Bitmap] downsampled in two stages:
     * 1. Fits the widget's target [maxWidthDp] x [maxHeightDp] bounds.
     * 2. Enforces `calculateWidgetMemoryCapBytes(context) / distinctImageCount`.
     *
     * Returns `null` if [filePath] does not exist or cannot be decoded.
     */
    fun decodeDownsampledWidgetBitmap(
        context: Context,
        filePath: String,
        maxWidthDp: Int,
        maxHeightDp: Int,
        distinctImageCount: Int = 1,
        config: Bitmap.Config = Bitmap.Config.RGB_565,
    ): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val bounds =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
                inPreferredConfig = config
            }
        BitmapFactory.decodeFile(filePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val density = context.resources.displayMetrics.density
        val targetWidthPx = max(1, (maxWidthDp * density).toInt())
        val targetHeightPx = max(1, (maxHeightDp * density).toInt())

        var sampleSize = 1
        while (
            (
                bounds.outWidth / (sampleSize * 2) >= targetWidthPx &&
                    bounds.outHeight / (sampleSize * 2) >= targetHeightPx
            ) ||
            (bounds.outWidth / sampleSize > targetWidthPx * 2) ||
            (bounds.outHeight / sampleSize > targetHeightPx * 2)
        ) {
            sampleSize *= 2
        }

        val perImageCapBytes = calculateWidgetMemoryCapBytes(context) / max(1, distinctImageCount)
        val outConfig =
            try {
                bounds.outConfig
            } catch (_: NoSuchFieldError) {
                null
            }
        val bytesPerPixel =
            when {
                outConfig != null && outConfig != config -> 4L
                config == Bitmap.Config.RGB_565 -> 2L
                config == Bitmap.Config.ALPHA_8 -> 1L
                else -> 4L
            }

        while (
            (
                max(1, bounds.outWidth / sampleSize).toLong() *
                    max(1, bounds.outHeight / sampleSize).toLong() *
                    bytesPerPixel
            ) > perImageCapBytes
        ) {
            sampleSize *= 2
        }

        val decodeOptions =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = config
            }
        return BitmapFactory.decodeFile(filePath, decodeOptions)
    }
}
