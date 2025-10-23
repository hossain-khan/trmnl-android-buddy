package ink.trmnl.android.buddy.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import coil3.ImageLoader
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for downloading and saving images to the device's Pictures directory.
 * Uses MediaStore API for Android 10+ (no runtime permissions required).
 */
object ImageDownloadUtils {
    private const val TAG = "ImageDownloadUtils"

    /**
     * Downloads an image from a URL and saves it to the Pictures directory.
     *
     * @param context Android context
     * @param imageUrl URL of the image to download
     * @param fileName Base name for the file (timestamp will be appended)
     * @return Result indicating success or failure with error message
     */
    suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        fileName: String = "TRMNL_Display",
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Load the image using Coil (leverages existing cache)
                val imageLoader = ImageLoader(context)
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(imageUrl)
                        .build()

                val result = imageLoader.execute(request)
                if (result !is SuccessResult) {
                    Log.e(TAG, "Failed to load image: $result")
                    return@withContext Result.failure(Exception("Failed to load image"))
                }

                // Convert Coil 3's Image to Android Bitmap
                val bitmap =
                    try {
                        result.image.toBitmap()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to convert image to bitmap", e)
                        return@withContext Result.failure(Exception("Could not convert image to bitmap: ${e.message}"))
                    }

                // Generate filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val displayName = "${fileName}_$timestamp.png"

                // Save to MediaStore (Pictures directory)
                val savedUri = saveBitmapToMediaStore(context, bitmap, displayName)

                if (savedUri != null) {
                    Log.d(TAG, "Image saved successfully: $savedUri")
                    Result.success(displayName)
                } else {
                    Result.failure(Exception("Failed to save image to media store"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image", e)
                Result.failure(e)
            }
        }

    /**
     * Saves a bitmap to the MediaStore Pictures directory.
     *
     * @param context Android context
     * @param bitmap The bitmap to save
     * @param displayName The name to display for the file
     * @return The URI of the saved image, or null if failed
     */
    private fun saveBitmapToMediaStore(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
    ): android.net.Uri? {
        val contentResolver = context.contentResolver
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

        val imageUri =
            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues,
            ) ?: return null

        var outputStream: OutputStream? = null
        try {
            outputStream = contentResolver.openOutputStream(imageUri)
            outputStream?.let {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            // Mark as not pending anymore (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            }

            return imageUri
        } catch (e: Exception) {
            Log.e(TAG, "Error saving bitmap to MediaStore", e)
            // Clean up on failure
            contentResolver.delete(imageUri, null, null)
            return null
        } finally {
            outputStream?.close()
        }
    }
}
