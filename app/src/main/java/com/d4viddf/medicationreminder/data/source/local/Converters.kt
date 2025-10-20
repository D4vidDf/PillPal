package com.d4viddf.medicationreminder.data.source.local

import androidx.room.TypeConverter
import com.d4viddf.medicationreminder.data.model.MedicationForm

class Converters {
    @TypeConverter
    fun toMedicationForm(value: String) = enumValueOf<MedicationForm>(value)

    @TypeConverter
    fun fromMedicationForm(value: MedicationForm) = value.name
}
