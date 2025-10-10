package com.d4viddf.medicationreminder.utils

import com.d4viddf.medicationreminder.R

fun getMedicationTypeStringResource(typeId: Int): Int {
    return when (typeId) {
        1 -> R.string.medication_type_tablet
        2 -> R.string.medication_type_pill
        3 -> R.string.medication_type_powder
        4 -> R.string.medication_type_syringe
        5 -> R.string.medication_type_creme
        6 -> R.string.medication_type_spray
        7 -> R.string.medication_type_liquid
        8 -> R.string.medication_type_suppositorium
        9 -> R.string.medication_type_patch
        else -> R.string.medication_type_other_image_description
    }
}