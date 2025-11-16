package ink.trmnl.android.buddy.fakes

import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of [DeviceTokenRepository] for testing.
 *
 * This fake provides a working in-memory implementation suitable for unit tests,
 * following the project's testing guidelines of using fakes instead of mocks.
 *
 * Tracks whether clearAll() was called to verify behavior.
 *
 * @param hasToken If true, initializes with a test token for "ABC-123" device.
 * @param initialTokens Optional map of device IDs to tokens to pre-populate the repository.
 * @param shouldThrowOnSave If true, saveDeviceToken will throw an exception. Useful for testing error handling.
 * @param shouldThrowOnClear If true, clearDeviceToken will throw an exception. Useful for testing error handling.
 */
class FakeDeviceTokenRepository(
    hasToken: Boolean = false,
    initialTokens: Map<String, String> = emptyMap(),
    private val shouldThrowOnSave: Boolean = false,
    private val shouldThrowOnClear: Boolean = false,
) : DeviceTokenRepository {
    private val deviceTokens =
        mutableMapOf<String, String>().apply {
            if (hasToken) {
                put("ABC-123", "test-token")
            }
            putAll(initialTokens)
        }

    /**
     * Test-visible property to verify that clearAll() was called.
     */
    var wasCleared = false
        private set

    override suspend fun saveDeviceToken(
        deviceFriendlyId: String,
        token: String,
    ) {
        if (shouldThrowOnSave) {
            throw Exception("Test exception")
        }
        deviceTokens[deviceFriendlyId] = token
    }

    override suspend fun getDeviceToken(deviceFriendlyId: String): String? = deviceTokens[deviceFriendlyId]

    override fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?> = flowOf(deviceTokens[deviceFriendlyId])

    override suspend fun clearDeviceToken(deviceFriendlyId: String) {
        if (shouldThrowOnClear) {
            throw Exception("Test exception")
        }
        deviceTokens.remove(deviceFriendlyId)
    }

    override suspend fun hasDeviceToken(deviceFriendlyId: String): Boolean = deviceTokens.containsKey(deviceFriendlyId)

    override suspend fun clearAll() {
        wasCleared = true
        deviceTokens.clear()
    }

    /**
     * Test helper method to get all stored tokens.
     */
    fun getAllTokens(): Map<String, String> = deviceTokens.toMap()
}
