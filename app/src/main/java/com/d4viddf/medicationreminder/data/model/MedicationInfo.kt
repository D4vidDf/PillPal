package com.d4viddf.medicationreminder.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_info",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedicationInfo(
    @PrimaryKey val medicationId: Int,   // Use medication ID as the primary key to ensure 1:1 relation
    val description: String?,            // Description of the medication
    val atcCode: String?,                // ATC code of the medication
    val safetyNotes: String?,            // Safety notes retrieved from CIMA
    val documentUrls: String?,           // URLs to access full information (comma-separated string for multiple URLs)
    val administrationRoutes: String?,   // Administration routes (comma-separated string for multiple routes)
    val dosage: String?,                 // Dosage information
    val nregistro: String?,              // Medication registration number
    val labtitular: String?,             // Laboratory titular name
    val comercializado: Boolean,         // Whether the medication is marketed
    val requiereReceta: Boolean,         // Whether the medication requires a prescription
    val generico: Boolean                // Whether the medication is generic
)