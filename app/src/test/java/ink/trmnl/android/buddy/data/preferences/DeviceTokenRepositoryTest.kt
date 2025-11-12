package ink.trmnl.android.buddy.data.preferences

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for [DeviceTokenRepository] interface using a fake implementation.
 *
 * Tests device token storage operations without requiring actual DataStore.
 */
class DeviceTokenRepositoryTest {
    @Test
    fun `saveDeviceToken stores token for device`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            // When
            repository.saveDeviceToken("ABC-123", "test-token-123")

            // Then
            val token = repository.getDeviceToken("ABC-123")
            assertThat(token).isEqualTo("test-token-123")
        }

    @Test
    fun `saveDeviceToken overwrites existing token`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "old-token")

            // When
            repository.saveDeviceToken("ABC-123", "new-token")

            // Then
            val token = repository.getDeviceToken("ABC-123")
            assertThat(token).isEqualTo("new-token")
        }

    @Test
    fun `getDeviceToken returns null when token not set`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            // When
            val token = repository.getDeviceToken("NONEXISTENT-ID")

            // Then
            assertThat(token).isNull()
        }

    @Test
    fun `getDeviceToken returns correct token for specific device`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "token-abc")
            repository.saveDeviceToken("DEF-456", "token-def")

            // When
            val tokenAbc = repository.getDeviceToken("ABC-123")
            val tokenDef = repository.getDeviceToken("DEF-456")

            // Then
            assertThat(tokenAbc).isEqualTo("token-abc")
            assertThat(tokenDef).isEqualTo("token-def")
        }

    @Test
    fun `getDeviceTokenFlow emits null initially`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            // When/Then
            repository.getDeviceTokenFlow("ABC-123").test {
                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    fun `getDeviceTokenFlow emits token after save`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            repository.getDeviceTokenFlow("ABC-123").test {
                // Initially null
                assertThat(awaitItem()).isNull()

                // When
                repository.saveDeviceToken("ABC-123", "flow-token")

                // Then
                assertThat(awaitItem()).isEqualTo("flow-token")
            }
        }

    @Test
    fun `getDeviceTokenFlow emits updated token on change`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "initial-token")

            repository.getDeviceTokenFlow("ABC-123").test {
                // Initial value
                assertThat(awaitItem()).isEqualTo("initial-token")

                // When
                repository.saveDeviceToken("ABC-123", "updated-token")

                // Then
                assertThat(awaitItem()).isEqualTo("updated-token")
            }
        }

    @Test
    fun `clearDeviceToken removes token for specific device`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "token-123")
            repository.saveDeviceToken("DEF-456", "token-456")

            // When
            repository.clearDeviceToken("ABC-123")

            // Then
            assertThat(repository.getDeviceToken("ABC-123")).isNull()
            assertThat(repository.getDeviceToken("DEF-456")).isEqualTo("token-456")
        }

    @Test
    fun `clearDeviceToken is safe when token not set`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            // When/Then - should not throw
            repository.clearDeviceToken("NONEXISTENT-ID")
            assertThat(repository.getDeviceToken("NONEXISTENT-ID")).isNull()
        }

    @Test
    fun `hasDeviceToken returns false when token not set`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            // When
            val hasToken = repository.hasDeviceToken("ABC-123")

            // Then
            assertThat(hasToken).isFalse()
        }

    @Test
    fun `hasDeviceToken returns true when token is set`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "token-123")

            // When
            val hasToken = repository.hasDeviceToken("ABC-123")

            // Then
            assertThat(hasToken).isTrue()
        }

    @Test
    fun `hasDeviceToken returns false after token cleared`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "token-123")
            repository.clearDeviceToken("ABC-123")

            // When
            val hasToken = repository.hasDeviceToken("ABC-123")

            // Then
            assertThat(hasToken).isFalse()
        }

    @Test
    fun `clearAll removes all device tokens`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("ABC-123", "token-abc")
            repository.saveDeviceToken("DEF-456", "token-def")
            repository.saveDeviceToken("GHI-789", "token-ghi")

            // When
            repository.clearAll()

            // Then
            assertThat(repository.getDeviceToken("ABC-123")).isNull()
            assertThat(repository.getDeviceToken("DEF-456")).isNull()
            assertThat(repository.getDeviceToken("GHI-789")).isNull()
        }

    @Test
    fun `multiple operations work correctly in sequence`() =
        runTest {
            // Given
            val repository = FakeDeviceTokenRepository()

            // Save
            repository.saveDeviceToken("ABC-123", "token-1")
            assertThat(repository.hasDeviceToken("ABC-123")).isTrue()

            // Update
            repository.saveDeviceToken("ABC-123", "token-2")
            assertThat(repository.getDeviceToken("ABC-123")).isEqualTo("token-2")

            // Add another
            repository.saveDeviceToken("DEF-456", "token-3")
            assertThat(repository.hasDeviceToken("DEF-456")).isTrue()

            // Clear one
            repository.clearDeviceToken("ABC-123")
            assertThat(repository.hasDeviceToken("ABC-123")).isFalse()
            assertThat(repository.hasDeviceToken("DEF-456")).isTrue()

            // Clear all
            repository.clearAll()
            assertThat(repository.hasDeviceToken("DEF-456")).isFalse()
        }

    /**
     * Fake in-memory implementation of DeviceTokenRepository for testing.
     */
    private class FakeDeviceTokenRepository : DeviceTokenRepository {
        private val tokens = mutableMapOf<String, String>()
        private val flows = mutableMapOf<String, MutableStateFlow<String?>>()

        override suspend fun saveDeviceToken(
            deviceFriendlyId: String,
            token: String,
        ) {
            tokens[deviceFriendlyId] = token
            getOrCreateFlow(deviceFriendlyId).value = token
        }

        override suspend fun getDeviceToken(deviceFriendlyId: String): String? = tokens[deviceFriendlyId]

        override fun getDeviceTokenFlow(deviceFriendlyId: String): Flow<String?> = getOrCreateFlow(deviceFriendlyId)

        override suspend fun clearDeviceToken(deviceFriendlyId: String) {
            tokens.remove(deviceFriendlyId)
            getOrCreateFlow(deviceFriendlyId).value = null
        }

        override suspend fun hasDeviceToken(deviceFriendlyId: String): Boolean = tokens.containsKey(deviceFriendlyId)

        override suspend fun clearAll() {
            val keys = tokens.keys.toList()
            tokens.clear()
            keys.forEach { key ->
                flows[key]?.value = null
            }
        }

        private fun getOrCreateFlow(deviceFriendlyId: String): MutableStateFlow<String?> =
            flows.getOrPut(deviceFriendlyId) { MutableStateFlow(tokens[deviceFriendlyId]) }
    }
}
