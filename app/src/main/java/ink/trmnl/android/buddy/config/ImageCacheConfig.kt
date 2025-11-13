package ink.trmnl.android.buddy.config

/**
 * Configuration constants for Coil image loading and caching.
 *
 * These values control the disk and memory cache sizes for image loading with Coil.
 * Using conservative values to balance performance with storage/memory usage.
 *
 * @see coil3.ImageLoader
 */
object ImageCacheConfig {
    /**
     * Disk cache size as a percentage of available disk space.
     *
     * Set to 1% of disk space to provide adequate caching for device preview images,
     * blog post featured images, and recipe screenshots while not consuming
     * excessive storage.
     *
     * Default: 0.01 (1% of disk space)
     */
    const val DISK_CACHE_SIZE_PERCENT = 0.01

    /**
     * Memory cache size as a percentage of available app memory.
     *
     * Set to 10% of app memory for efficient in-memory caching of recently viewed images.
     * This reduces network requests and provides instant image display on repeated views.
     *
     * Default: 0.10 (10% of app memory)
     */
    const val MEMORY_CACHE_SIZE_PERCENT = 0.10
}
