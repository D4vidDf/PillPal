package com.d4viddf.medicationreminder.data.source.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.d4viddf.medicationreminder.data.model.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MedicationDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: MedicationDatabase
    private lateinit var medicationDao: MedicationDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MedicationDatabase::class.java
        ).allowMainThreadQueries().build()
        medicationDao = database.medicationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert and retrieve medication`() = runBlocking {
        val medication = Medication(id = 1, name = "Aspirin", typeId = 1, color = "#FF0000", packageSize = 10, remainingDoses = 10, startDate = null, endDate = null, reminderTime = null)
        medicationDao.insertMedication(medication)

        val retrievedMedication = medicationDao.getMedicationById(1)
        assertEquals(medication, retrievedMedication)
    }

    @Test
    fun `update medication`() = runBlocking {
        val medication = Medication(id = 1, name = "Aspirin", typeId = 1, color = "#FF0000", packageSize = 10, remainingDoses = 10, startDate = null, endDate = null, reminderTime = null)
        medicationDao.insertMedication(medication)

        val updatedMedication = medication.copy(name = "Ibuprofen")
        medicationDao.updateMedication(updatedMedication)

        val retrievedMedication = medicationDao.getMedicationById(1)
        assertEquals(updatedMedication, retrievedMedication)
    }

    @Test
    fun `delete medication`() = runBlocking {
        val medication = Medication(id = 1, name = "Aspirin", typeId = 1, color = "#FF0000", packageSize = 10, remainingDoses = 10, startDate = null, endDate = null, reminderTime = null)
        medicationDao.insertMedication(medication)
        medicationDao.deleteMedication(medication)

        val retrievedMedication = medicationDao.getMedicationById(1)
        assertNull(retrievedMedication)
    }

    @Test
    fun `get all medications`() = runBlocking {
        val medication1 = Medication(id = 1, name = "Aspirin", typeId = 1, color = "#FF0000", packageSize = 10, remainingDoses = 10, startDate = null, endDate = null, reminderTime = null)
        val medication2 = Medication(id = 2, name = "Ibuprofen", typeId = 2, color = "#00FF00", packageSize = 20, remainingDoses = 20, startDate = null, endDate = null, reminderTime = null)
        medicationDao.insertMedication(medication1)
        medicationDao.insertMedication(medication2)

        val allMedications = medicationDao.getAllMedications().first()
        assertEquals(2, allMedications.size)
    }

    @Test
    fun `get medication by id flow`() = runBlocking {
        val medication = Medication(id = 1, name = "Aspirin", typeId = 1, color = "#FF0000", packageSize = 10, remainingDoses = 10, startDate = null, endDate = null, reminderTime = null)
        medicationDao.insertMedication(medication)

        val retrievedMedication = medicationDao.getMedicationByIdFlow(1).first()
        assertEquals(medication, retrievedMedication)
    }
}