package ink.trmnl.android.buddy.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.ImageLoader
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility functions for downloading and saving images to the device's Pictures directory.
 * Uses MediaStore API for Android 10+ (no runtime permissions required).
 */
object ImageDownloadUtils {
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
                    Timber.e("Failed to load image: %s", result)
                    return@withContext Result.failure(Exception("Failed to load image"))
                }

                // Convert Coil 3's Image to Android Bitmap
                val bitmap =
                    try {
                        result.image.toBitmap()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to convert image to bitmap")
                        return@withContext Result.failure(Exception("Could not convert image to bitmap: ${e.message}"))
                    }

                // Sanitize filename and generate with timestamp
                val sanitizedFileName = sanitizeFileName(fileName)
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val displayName = "${sanitizedFileName}_$timestamp.png"

                // Save to MediaStore (Pictures directory)
                val savedUri = saveBitmapToMediaStore(context, bitmap, displayName)

                if (savedUri != null) {
                    Timber.d("Image saved successfully: %s", savedUri)
                    Result.success(displayName)
                } else {
                    Result.failure(Exception("Failed to save image to media store"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading image")
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
            Timber.e(e, "Error saving bitmap to MediaStore")
            // Clean up on failure
            contentResolver.delete(imageUri, null, null)
            return null
        } finally {
            outputStream?.close()
        }
    }

    /**
     * Sanitizes a filename by removing or replacing special characters that could cause issues
     * with Android file naming.
     *
     * Removes: / \ : * ? " ' < > | ( ) [ ] (reserved on Windows/Android and shell-problematic)
     * Replaces: whitespace with underscores
     * Limits: length to 50 characters to avoid excessively long filenames
     *
     * @param fileName The filename to sanitize
     * @return A sanitized filename safe for Android file system
     */
    internal fun sanitizeFileName(fileName: String): String {
        // Replace whitespace with underscores
        var sanitized = fileName.replace(Regex("\\s+"), "_")

        // Remove special characters that are not allowed in filenames
        // Android/Linux: / null
        // Windows (also restricted on Android): \ / : * ? " < > |
        // Also remove quotes, brackets, and parentheses to avoid shell/parsing issues
        sanitized = sanitized.replace(Regex("[/\\\\:*?\"'<>|()\\[\\]]"), "")

        // Remove leading/trailing dots and underscores
        sanitized = sanitized.trim('.', '_')

        // Ensure the filename is not empty after sanitization
        if (sanitized.isEmpty()) {
            sanitized = "TRMNL_Display"
        }

        // Limit length to avoid excessively long filenames
        if (sanitized.length > 50) {
            sanitized = sanitized.take(50)
        }

        return sanitized
    }
}
