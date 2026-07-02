package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.BiosecurityCheck
import com.example.data.FarmDao
import com.example.data.FarmDatabase
import com.example.data.VaccinationSchedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: FarmDatabase
    private lateinit var dao: FarmDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FarmDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.farmDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ternak Layer", appName)
    }

    @Test
    fun `insert and retrieve vaccination schedule`() = runBlocking {
        val vaccine = VaccinationSchedule(
            kandangName = "Kandang A",
            vaccineName = "ND-IB Live",
            plannedDate = "2026-07-01",
            method = "Tetes Mata",
            notes = "Test Note",
            status = "Pending"
        )
        dao.insertVaccination(vaccine)

        val allVaccines = dao.getAllVaccinations().first()
        assertEquals(1, allVaccines.size)
        assertEquals("ND-IB Live", allVaccines[0].vaccineName)
        assertEquals("Kandang A", allVaccines[0].kandangName)
        assertEquals("Pending", allVaccines[0].status)
    }

    @Test
    fun `insert and retrieve biosecurity check`() = runBlocking {
        val check = BiosecurityCheck(
            date = "2026-07-01",
            inspectorName = "Inspector Test",
            footBathActive = true,
            vehicleSpray = true,
            feedWarehouseClean = true,
            cageWalkwayClean = true,
            safeMortalityDisposal = true,
            eggTrayDisinfected = true,
            waterSanitization = true,
            wildBirdControl = true,
            score = 100,
            notes = "Perfect day"
        )
        dao.insertBiosecurityCheck(check)

        val allChecks = dao.getAllBiosecurityChecks().first()
        assertEquals(1, allChecks.size)
        assertEquals("Inspector Test", allChecks[0].inspectorName)
        assertEquals(100, allChecks[0].score)
        assertEquals(true, allChecks[0].footBathActive)
    }
}
