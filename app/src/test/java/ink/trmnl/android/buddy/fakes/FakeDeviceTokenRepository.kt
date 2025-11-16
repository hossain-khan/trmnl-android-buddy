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
 */
class FakeDeviceTokenRepository(
    hasToken: Boolean = false,
) : DeviceTokenRepository {
    private val deviceTokens = mutableMapOf<String, String>()

    init {
        if (hasToken) {
            deviceTokens["ABC-123"] = "test-token"
        }
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
        deviceTokens[deviceFriendlyId] = token
    }

    override suspend fun getDeviceToken(deviceFriendlyId: String): String? = deviceTokens[deviceFriendlyId]

    override fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?> = flowOf(deviceTokens[deviceFriendlyId])

    override suspend fun clearDeviceToken(deviceFriendlyId: String) {
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
