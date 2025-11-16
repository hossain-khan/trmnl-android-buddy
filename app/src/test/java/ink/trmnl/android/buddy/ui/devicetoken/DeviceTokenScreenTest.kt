package ink.trmnl.android.buddy.ui.devicetoken

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import ink.trmnl.android.buddy.data.preferences.DeviceTokenRepository
import ink.trmnl.android.buddy.fakes.FakeDeviceTokenRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for DeviceTokenPresenter.
 *
 * Tests cover:
 * - Initial state with device info
 * - Load existing token
 * - Token input and validation
 * - Save device token
 * - Clear token functionality
 * - Navigation flows
 * - Error handling
 * - Edge cases (long tokens, special characters, unicode, whitespace)
 */
class DeviceTokenScreenTest {
    @Test
    fun `presenter returns initial state with device info`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                assertThat(state.deviceFriendlyId).isEqualTo("TRMNL-ABC-123")
                assertThat(state.deviceName).isEqualTo("Living Room Display")
                assertThat(state.currentToken).isEqualTo("")
                assertThat(state.tokenInput).isEqualTo("")
                assertThat(state.isSaving).isFalse()
                assertThat(state.errorMessage).isNull()
            }
        }

    @Test
    fun `presenter loads existing token on startup`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("TRMNL-ABC-123", "existing_token_123")

            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                // Skip initial empty state
                skipItems(1)

                val state = awaitItem()
                assertThat(state.currentToken).isEqualTo("existing_token_123")
                assertThat(state.tokenInput).isEqualTo("existing_token_123")
            }
        }

    @Test
    fun `token changed event updates input and clears error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                assertThat(state.tokenInput).isEqualTo("")

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("new_token_input"))

                val updatedState = awaitItem()
                assertThat(updatedState.tokenInput).isEqualTo("new_token_input")
                assertThat(updatedState.errorMessage).isNull()
            }
        }

    @Test
    fun `save token with empty token shows error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token cannot be empty")
                assertThat(errorState.isSaving).isFalse()
            }
        }

    @Test
    fun `save token with blank token shows error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("   "))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token cannot be empty")
            }
        }

    @Test
    fun `save token with short token shows error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("short"))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token must be at least 20 characters long")
            }
        }

    @Test
    fun `save token with exactly 20 characters succeeds`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                var state = awaitItem()

                // Exactly 20 characters (boundary case)
                state.eventSink(DeviceTokenScreen.Event.TokenChanged("12345678901234567890"))
                state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)
                awaitItem() // saving

                // Verify token was saved
                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isEqualTo("12345678901234567890")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `save token with 19 characters shows error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                // 19 characters (boundary case - should fail)
                state.eventSink(DeviceTokenScreen.Event.TokenChanged("1234567890123456789"))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)

                val errorState = awaitItem()
                assertThat(errorState.errorMessage).isEqualTo("Token must be at least 20 characters long")
            }
        }

    @Test
    fun `save valid token succeeds and updates repository`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                var state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("valid_device_token_12345"))
                state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)

                state = awaitItem()
                assertThat(state.isSaving).isTrue()
                assertThat(state.errorMessage).isNull()

                // Verify token was saved
                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isEqualTo("valid_device_token_12345")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `save token trims whitespace before saving`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("  token_with_spaces_12345  "))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)
                awaitItem() // saving

                // Verify token was trimmed
                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isEqualTo("token_with_spaces_12345")
            }
        }

    @Test
    fun `save token handles very long tokens`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                val longToken = "device_" + "x".repeat(1000)
                state.eventSink(DeviceTokenScreen.Event.TokenChanged(longToken))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)
                awaitItem() // saving

                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isEqualTo(longToken)
            }
        }

    @Test
    fun `save token handles special characters`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                val specialToken = "token_!@#\$%^&*()_+-=[]{}|;':,.<>?/"
                state.eventSink(DeviceTokenScreen.Event.TokenChanged(specialToken))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)
                awaitItem() // saving

                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isEqualTo(specialToken)
            }
        }

    @Test
    fun `save token handles exception and shows error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository =
                FakeDeviceTokenRepository(
                    shouldThrowOnSave = true,
                )
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("valid_device_token_12345"))
                awaitItem()

                state.eventSink(DeviceTokenScreen.Event.SaveToken)
                awaitItem() // saving

                val errorState = awaitItem()
                assertThat(errorState.isSaving).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("Failed to save token: Test exception")
            }
        }

    @Test
    fun `clear token removes current token`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("TRMNL-ABC-123", "existing_token_123")

            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                // Skip initial empty state and wait for loaded state
                skipItems(1)
                val loadedState = awaitItem()
                assertThat(loadedState.currentToken).isEqualTo("existing_token_123")

                // Clear token
                loadedState.eventSink(DeviceTokenScreen.Event.ClearToken)

                val savingState = awaitItem()
                assertThat(savingState.isSaving).isTrue()

                val clearedState = awaitItem()
                assertThat(clearedState.currentToken).isEqualTo("")
                assertThat(clearedState.tokenInput).isEqualTo("")
                assertThat(clearedState.isSaving).isFalse()

                // Verify token was removed from repository
                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isNull()
            }
        }

    @Test
    fun `clear token handles exception and shows error`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository =
                FakeDeviceTokenRepository(
                    shouldThrowOnClear = true,
                )
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.ClearToken)
                awaitItem() // saving

                val errorState = awaitItem()
                assertThat(errorState.isSaving).isFalse()
                assertThat(errorState.errorMessage).isEqualTo("Failed to clear token: Test exception")
            }
        }

    @Test
    fun `multiple token changes update input correctly`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                val state = awaitItem()

                state.eventSink(DeviceTokenScreen.Event.TokenChanged("token1"))
                val state1 = awaitItem()
                assertThat(state1.tokenInput).isEqualTo("token1")

                state1.eventSink(DeviceTokenScreen.Event.TokenChanged("token2"))
                val state2 = awaitItem()
                assertThat(state2.tokenInput).isEqualTo("token2")

                state2.eventSink(DeviceTokenScreen.Event.TokenChanged("token3"))
                val state3 = awaitItem()
                assertThat(state3.tokenInput).isEqualTo("token3")
            }
        }

    @Test
    fun `save token updates existing token`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("TRMNL-ABC-123", "old_token_12345678901234567890")

            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                // Skip initial and loaded states
                skipItems(1)
                val loadedState = awaitItem()
                assertThat(loadedState.currentToken).isEqualTo("old_token_12345678901234567890")

                // Update with new token
                loadedState.eventSink(DeviceTokenScreen.Event.TokenChanged("new_token_12345678901234567890"))
                awaitItem()

                loadedState.eventSink(DeviceTokenScreen.Event.SaveToken)
                awaitItem() // saving

                // Verify new token was saved
                assertThat(repository.getDeviceToken("TRMNL-ABC-123")).isEqualTo("new_token_12345678901234567890")
            }
        }

    @Test
    fun `loading state set during clear operation`() =
        runTest {
            val screen = DeviceTokenScreen(deviceFriendlyId = "TRMNL-ABC-123", deviceName = "Living Room Display")
            val navigator = FakeNavigator(screen)
            val repository = FakeDeviceTokenRepository()
            repository.saveDeviceToken("TRMNL-ABC-123", "existing_token_123")

            val presenter = DeviceTokenPresenter(screen, navigator, repository)

            presenter.test {
                skipItems(1)
                var loadedState = awaitItem()

                loadedState.eventSink(DeviceTokenScreen.Event.ClearToken)

                loadedState = awaitItem()
                assertThat(loadedState.isSaving).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `presenter handles different device IDs correctly`() =
        runTest {
            val screen1 = DeviceTokenScreen(deviceFriendlyId = "TRMNL-AAA-111", deviceName = "Device 1")
            val navigator1 = FakeNavigator(screen1)
            val repository = FakeDeviceTokenRepository()

            // Save token for device 1
            repository.saveDeviceToken("TRMNL-AAA-111", "token_for_device_1_12345")

            val presenter1 = DeviceTokenPresenter(screen1, navigator1, repository)

            presenter1.test {
                skipItems(1)
                val state1 = awaitItem()
                assertThat(state1.deviceFriendlyId).isEqualTo("TRMNL-AAA-111")
                assertThat(state1.currentToken).isEqualTo("token_for_device_1_12345")
            }

            // Test with different device
            val screen2 = DeviceTokenScreen(deviceFriendlyId = "TRMNL-BBB-222", deviceName = "Device 2")
            val navigator2 = FakeNavigator(screen2)
            val presenter2 = DeviceTokenPresenter(screen2, navigator2, repository)

            presenter2.test {
                val state2 = awaitItem()
                assertThat(state2.deviceFriendlyId).isEqualTo("TRMNL-BBB-222")
                assertThat(state2.currentToken).isEqualTo("") // Different device, no token
            }
        }
}
