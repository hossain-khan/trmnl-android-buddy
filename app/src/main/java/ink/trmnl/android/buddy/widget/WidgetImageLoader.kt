package ink.trmnl.android.buddy.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Utility for loading images for Glance widgets.
 *
 * Glance doesn't support Coil directly, so we need to download images
 * and save them to internal storage for later use.
 */
object WidgetImageLoader {
    private const val WIDGET_IMAGE_DIR = "widget_images"

    /**
     * Download an image from URL and save it to internal storage.
     *
     * @param context Android context
     * @param imageUrl URL of the image to load
     * @param deviceId Device ID to use for filename
     * @return File path of the saved image, or null if failed
     */
    suspend fun downloadAndSaveImage(
        context: Context,
        imageUrl: String,
        deviceId: Int,
    ): String? {
        return try {
            // Download image using Coil
            val imageLoader = ImageLoader(context)
            val request =
                ImageRequest
                    .Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Disable hardware bitmaps for widgets
                    .build()

            val result = imageLoader.execute(request)
            val bitmap = result.image?.toBitmap() ?: return null

            // Save bitmap to internal storage
            val imageDir = File(context.filesDir, WIDGET_IMAGE_DIR)
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            val imageFile = File(imageDir, "device_$deviceId.png")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            Timber.d("Saved widget image to: ${imageFile.absolutePath}")
            imageFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to download and save image from URL: $imageUrl")
            null
        }
    }

    /**
     * Load a saved image from internal storage.
     *
     * @param imagePath Absolute path to the saved image
     * @return Bitmap if successful, null otherwise
     */
    fun loadSavedImage(imagePath: String): Bitmap? =
        try {
            val file = File(imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(imagePath)
            } else {
                Timber.w("Image file not found: $imagePath")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load saved image: $imagePath")
            null
        }

    /**
     * Delete the saved image for a device.
     *
     * @param context Android context
     * @param deviceId Device ID
     */
    fun deleteSavedImage(
        context: Context,
        deviceId: Int,
    ) {
        try {
            val imageDir = File(context.filesDir, WIDGET_IMAGE_DIR)
            val imageFile = File(imageDir, "device_$deviceId.png")
            if (imageFile.exists()) {
                imageFile.delete()
                Timber.d("Deleted widget image: ${imageFile.absolutePath}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete saved image for device $deviceId")
        }
    }
}
