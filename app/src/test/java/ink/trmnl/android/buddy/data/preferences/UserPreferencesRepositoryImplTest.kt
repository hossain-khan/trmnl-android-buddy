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
 * Unit tests for [UserPreferencesRepositoryImpl] using real DataStore preferences under Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class UserPreferencesRepositoryImplTest {
    private lateinit var repository: UserPreferencesRepositoryImpl

    @Before
    fun setUp() =
        runTest {
            repository = UserPreferencesRepositoryImpl(ApplicationProvider.getApplicationContext())
            repository.clearAll()
        }

    @Test
    fun `userPreferencesFlow emits default values on fresh DataStore`() =
        runTest {
            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isNull()
                assertThat(prefs.isOnboardingCompleted).isFalse()
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
                assertThat(prefs.isLowBatteryNotificationEnabled).isFalse()
                assertThat(prefs.lowBatteryThresholdPercent).isEqualTo(UserPreferences.DEFAULT_LOW_BATTERY_THRESHOLD)
                assertThat(prefs.isRssFeedContentEnabled).isTrue()
                assertThat(prefs.isRssFeedContentNotificationEnabled).isFalse()
                assertThat(prefs.isAnnouncementAuthBannerDismissed).isFalse()
                assertThat(prefs.isSecurityEnabled).isFalse()
                assertThat(prefs.isShowRecipeHealthCardEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saveApiToken stores token and clearApiToken removes it`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.saveApiToken("test-api-token-xyz")
                val updated = awaitItem()
                assertThat(updated.apiToken).isEqualTo("test-api-token-xyz")

                repository.clearApiToken()
                val cleared = awaitItem()
                assertThat(cleared.apiToken).isNull()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setOnboardingCompleted marks onboarding as completed`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setOnboardingCompleted()
                val updated = awaitItem()
                assertThat(updated.isOnboardingCompleted).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setBatteryTrackingEnabled updates preference correctly`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setBatteryTrackingEnabled(false)
                assertThat(awaitItem().isBatteryTrackingEnabled).isFalse()

                repository.setBatteryTrackingEnabled(true)
                assertThat(awaitItem().isBatteryTrackingEnabled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setLowBatteryNotificationEnabled and setLowBatteryThreshold update preferences`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setLowBatteryNotificationEnabled(true)
                assertThat(awaitItem().isLowBatteryNotificationEnabled).isTrue()

                repository.setLowBatteryThreshold(20)
                assertThat(awaitItem().lowBatteryThresholdPercent).isEqualTo(20)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setRssFeedContentEnabled and setRssFeedContentNotificationEnabled update preferences`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setRssFeedContentEnabled(false)
                assertThat(awaitItem().isRssFeedContentEnabled).isFalse()

                repository.setRssFeedContentNotificationEnabled(true)
                assertThat(awaitItem().isRssFeedContentNotificationEnabled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setAnnouncementAuthBannerDismissed updates preference`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setAnnouncementAuthBannerDismissed(true)
                assertThat(awaitItem().isAnnouncementAuthBannerDismissed).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setSecurityEnabled updates preference`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setSecurityEnabled(true)
                assertThat(awaitItem().isSecurityEnabled).isTrue()

                repository.setSecurityEnabled(false)
                assertThat(awaitItem().isSecurityEnabled).isFalse()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setShowRecipeHealthCard updates preference`() =
        runTest {
            repository.userPreferencesFlow.test {
                awaitItem() // initial

                repository.setShowRecipeHealthCard(false)
                assertThat(awaitItem().isShowRecipeHealthCardEnabled).isFalse()

                repository.setShowRecipeHealthCard(true)
                assertThat(awaitItem().isShowRecipeHealthCardEnabled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `clearAll resets all stored preferences back to default values`() =
        runTest {
            repository.saveApiToken("test_token")
            repository.setOnboardingCompleted()
            repository.setBatteryTrackingEnabled(false)
            repository.setSecurityEnabled(true)
            repository.setShowRecipeHealthCard(false)

            repository.clearAll()

            repository.userPreferencesFlow.test {
                val prefs = awaitItem()
                assertThat(prefs.apiToken).isNull()
                assertThat(prefs.isOnboardingCompleted).isFalse()
                assertThat(prefs.isBatteryTrackingEnabled).isTrue()
                assertThat(prefs.isSecurityEnabled).isFalse()
                assertThat(prefs.isShowRecipeHealthCardEnabled).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
