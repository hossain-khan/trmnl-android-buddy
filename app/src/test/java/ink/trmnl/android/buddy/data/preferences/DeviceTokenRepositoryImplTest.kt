package ink.trmnl.android.buddy.data.preferences

import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [DeviceTokenRepositoryImpl] using real DataStore preferences under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class DeviceTokenRepositoryImplTest {
    private lateinit var repository: DeviceTokenRepositoryImpl

    @Before
    fun setUp() =
        runTest {
            repository = DeviceTokenRepositoryImpl(ApplicationProvider.getApplicationContext())
            repository.clearAll()
        }

    @Test
    fun `getDeviceToken returns null when token not set`() =
        runTest {
            val token = repository.getDeviceToken("DEVICE-1")
            assertThat(token).isNull()
        }

    @Test
    fun `hasDeviceToken returns false initially and true when token saved`() =
        runTest {
            assertThat(repository.hasDeviceToken("DEVICE-1")).isFalse()

            repository.saveDeviceToken("DEVICE-1", "token-abc-123")

            assertThat(repository.hasDeviceToken("DEVICE-1")).isTrue()
            assertThat(repository.getDeviceToken("DEVICE-1")).isEqualTo("token-abc-123")
        }

    @Test
    fun `saveDeviceToken overwrites existing token`() =
        runTest {
            repository.saveDeviceToken("DEVICE-1", "old-token")
            assertThat(repository.getDeviceToken("DEVICE-1")).isEqualTo("old-token")

            repository.saveDeviceToken("DEVICE-1", "new-token")
            assertThat(repository.getDeviceToken("DEVICE-1")).isEqualTo("new-token")
        }

    @Test
    fun `getDeviceTokenFlow emits token updates reactively`() =
        runTest {
            repository.getDeviceTokenFlow("DEVICE-1").test {
                assertThat(awaitItem()).isNull()

                repository.saveDeviceToken("DEVICE-1", "flow-token")
                assertThat(awaitItem()).isEqualTo("flow-token")

                repository.clearDeviceToken("DEVICE-1")
                assertThat(awaitItem()).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearDeviceToken removes token for specific device without affecting other devices`() =
        runTest {
            repository.saveDeviceToken("DEVICE-1", "token-1")
            repository.saveDeviceToken("DEVICE-2", "token-2")

            repository.clearDeviceToken("DEVICE-1")

            assertThat(repository.getDeviceToken("DEVICE-1")).isNull()
            assertThat(repository.getDeviceToken("DEVICE-2")).isEqualTo("token-2")
        }

    @Test
    fun `clearAll removes all stored device tokens`() =
        runTest {
            repository.saveDeviceToken("DEVICE-1", "token-1")
            repository.saveDeviceToken("DEVICE-2", "token-2")

            repository.clearAll()

            assertThat(repository.getDeviceToken("DEVICE-1")).isNull()
            assertThat(repository.getDeviceToken("DEVICE-2")).isNull()
            assertThat(repository.hasDeviceToken("DEVICE-1")).isFalse()
            assertThat(repository.hasDeviceToken("DEVICE-2")).isFalse()
        }
}
