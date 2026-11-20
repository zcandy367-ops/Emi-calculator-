package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EmiPulseDatabase
import com.example.data.local.SavedEmiSessionEntity
import com.example.data.local.SavedSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SavedSessionRepositoryTest {

    private lateinit var database: EmiPulseDatabase
    private lateinit var repository: SavedSessionRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EmiPulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SavedSessionRepository(database.savedEmiSessionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndListSessions_ordersNewestFirstAndSupportsDelete() = runTest {
        val older = SavedEmiSessionEntity(
            label = "Older Car Loan",
            presetName = "CAR",
            currencyCode = "USD",
            principal = 30000.0,
            annualRatePercent = 8.5,
            tenureValue = 5,
            tenureUnitName = "YEARS",
            extraPayment = 0.0,
            prepaymentFrequencyName = "NONE",
            monthlyEmi = 615.5,
            totalInterest = 6930.0,
            totalPayable = 36930.0,
            interestSaved = 0.0,
            monthsSaved = 0,
            createdAtMillis = 1000L
        )
        val newer = SavedEmiSessionEntity(
            label = "Newer Home Loan",
            presetName = "HOME",
            currencyCode = "INR",
            principal = 5000000.0,
            annualRatePercent = 8.25,
            tenureValue = 20,
            tenureUnitName = "YEARS",
            extraPayment = 5000.0,
            prepaymentFrequencyName = "MONTHLY",
            monthlyEmi = 42603.0,
            totalInterest = 4100000.0,
            totalPayable = 9100000.0,
            interestSaved = 1120000.0,
            monthsSaved = 38,
            createdAtMillis = 2000L
        )

        val id1 = repository.saveSession(older)
        val id2 = repository.saveSession(newer)

        val list = repository.allSessions.first()
        assertEquals(2, list.size)
        assertEquals("Newer Home Loan", list[0].label)
        assertEquals("Older Car Loan", list[1].label)

        repository.deleteSession(id2)
        val afterDelete = repository.allSessions.first()
        assertEquals(1, afterDelete.size)
        assertEquals(id1, afterDelete[0].id)

        repository.clearAll()
        assertTrue(repository.allSessions.first().isEmpty())
    }
}
