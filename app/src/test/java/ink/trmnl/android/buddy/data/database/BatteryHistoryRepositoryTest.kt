package ink.trmnl.android.buddy.data.database

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BatteryHistoryRepositoryImpl.
 */
class BatteryHistoryRepositoryTest {
    private lateinit var repository: BatteryHistoryRepository
    private lateinit var fakeDao: FakeBatteryHistoryDao

    @Before
    fun setup() {
        fakeDao = FakeBatteryHistoryDao()
        repository = BatteryHistoryRepositoryImpl(fakeDao)
    }

    @Test
    fun `recordBatteryReading should insert battery data`() =
        runTest {
            // When
            repository.recordBatteryReading(
                deviceId = "ABC-123",
                percentCharged = 85.0,
                batteryVoltage = 3.7,
                timestamp = 1000L,
            )

            // Then
            val history = fakeDao.getBatteryHistoryForDevice("ABC-123").first()
            assertThat(history).hasSize(1)
            assertThat(history[0].deviceId).isEqualTo("ABC-123")
            assertThat(history[0].percentCharged).isEqualTo(85.0)
            assertThat(history[0].batteryVoltage).isEqualTo(3.7)
            assertThat(history[0].timestamp).isEqualTo(1000L)
        }

    @Test
    fun `getBatteryHistoryForDevice should return device history`() =
        runTest {
            // Given
            repository.recordBatteryReading("ABC-123", 85.0, 3.7, 1000L)
            repository.recordBatteryReading("ABC-123", 80.0, 3.6, 2000L)
            repository.recordBatteryReading("DEF-456", 90.0, 3.8, 1500L)

            // When
            val history = repository.getBatteryHistoryForDevice("ABC-123").first()

            // Then
            assertThat(history).hasSize(2)
            assertThat(history[0].deviceId).isEqualTo("ABC-123")
            assertThat(history[1].deviceId).isEqualTo("ABC-123")
        }

    @Test
    fun `getLatestBatteryReading should return most recent reading`() =
        runTest {
            // Given
            repository.recordBatteryReading("ABC-123", 85.0, 3.7, 1000L)
            repository.recordBatteryReading("ABC-123", 80.0, 3.6, 2000L)
            repository.recordBatteryReading("ABC-123", 75.0, 3.5, 3000L)

            // When
            val latest = repository.getLatestBatteryReading("ABC-123")

            // Then
            assertThat(latest).isNotNull()
            assertThat(latest!!.percentCharged).isEqualTo(75.0)
            assertThat(latest.timestamp).isEqualTo(3000L)
        }

    @Test
    fun `getLatestBatteryReading should return null when no data`() =
        runTest {
            // When
            val latest = repository.getLatestBatteryReading("ABC-123")

            // Then
            assertThat(latest).isNull()
        }

    @Test
    fun `getBatteryHistoryInRange should return data within time range`() =
        runTest {
            // Given
            repository.recordBatteryReading("ABC-123", 85.0, 3.7, 1000L)
            repository.recordBatteryReading("ABC-123", 80.0, 3.6, 2000L)
            repository.recordBatteryReading("ABC-123", 75.0, 3.5, 3000L)
            repository.recordBatteryReading("ABC-123", 70.0, 3.4, 4000L)

            // When
            val history = repository.getBatteryHistoryInRange("ABC-123", 1500L, 3500L).first()

            // Then
            assertThat(history).hasSize(2)
            assertThat(history[0].timestamp).isEqualTo(2000L)
            assertThat(history[1].timestamp).isEqualTo(3000L)
        }

    @Test
    fun `deleteHistoryForDevice should remove all device history`() =
        runTest {
            // Given
            repository.recordBatteryReading("ABC-123", 85.0, 3.7, 1000L)
            repository.recordBatteryReading("ABC-123", 80.0, 3.6, 2000L)
            repository.recordBatteryReading("DEF-456", 90.0, 3.8, 1500L)

            // When
            repository.deleteHistoryForDevice("ABC-123")

            // Then
            val abc123History = repository.getBatteryHistoryForDevice("ABC-123").first()
            val def456History = repository.getBatteryHistoryForDevice("DEF-456").first()
            assertThat(abc123History).hasSize(0)
            assertThat(def456History).hasSize(1)
        }
}
