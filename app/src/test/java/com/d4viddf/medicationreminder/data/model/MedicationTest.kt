package com.d4viddf.medicationreminder.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MedicationTest {

    @Test
    fun `createMedication with all properties`() {
        val medication = Medication(
            id = 1,
            name = "Ibuprofen",
            typeId = 1,
            color = "#FFFFFF",
            packageSize = 20,
            remainingDoses = 15,
            saveRemainingFraction = true,
            startDate = "2024-01-01",
            endDate = "2024-01-10",
            reminderTime = "08:00",
            registrationDate = "2023-12-01",
            nregistro = "12345",
            lowStockThreshold = 5,
            lowStockReminderDays = 3,
            isArchived = false,
            isSuspended = false
        )

        assertEquals(1, medication.id)
        assertEquals("Ibuprofen", medication.name)
        assertEquals(1, medication.typeId)
        assertEquals("#FFFFFF", medication.color)
        assertEquals(20, medication.packageSize)
        assertEquals(15, medication.remainingDoses)
        assertEquals(true, medication.saveRemainingFraction)
        assertEquals("2024-01-01", medication.startDate)
        assertEquals("2024-01-10", medication.endDate)
        assertEquals("08:00", medication.reminderTime)
        assertEquals("2023-12-01", medication.registrationDate)
        assertEquals("12345", medication.nregistro)
        assertEquals(5, medication.lowStockThreshold)
        assertEquals(3, medication.lowStockReminderDays)
        assertEquals(false, medication.isArchived)
        assertEquals(false, medication.isSuspended)
    }

    @Test
    fun `createMedication with default values`() {
        val medication = Medication(
            id = 2,
            name = "Paracetamol",
            typeId = 2,
            color = "#000000",
            packageSize = 30,
            remainingDoses = 30
        )

        assertEquals(2, medication.id)
        assertEquals("Paracetamol", medication.name)
        assertEquals(2, medication.typeId)
        assertEquals("#000000", medication.color)
        assertEquals(30, medication.packageSize)
        assertEquals(30, medication.remainingDoses)
        assertEquals(false, medication.saveRemainingFraction)
        assertEquals(null, medication.startDate)
        assertEquals(null, medication.endDate)
        assertEquals(null, medication.reminderTime)
        assertEquals(null, medication.registrationDate)
        assertEquals(null, medication.nregistro)
        assertEquals(null, medication.lowStockThreshold)
        assertEquals(null, medication.lowStockReminderDays)
        assertEquals(false, medication.isArchived)
        assertEquals(false, medication.isSuspended)
    }
}