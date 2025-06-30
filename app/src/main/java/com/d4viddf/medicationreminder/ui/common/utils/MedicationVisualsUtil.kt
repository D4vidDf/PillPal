package com.d4viddf.medicationreminder.ui.common.utils

import androidx.annotation.DrawableRes
import com.d4viddf.medicationreminder.R

object MedicationVisualsUtil {

    @DrawableRes
    fun getMedicationTypeIcon(medicationTypeName: String?): Int {
        return when (medicationTypeName?.trim()?.uppercase()) {
            "PILL", "TABLET" -> R.drawable.ic_medication_pill_solid // Placeholder, ensure this exists
            "CAPSULE" -> R.drawable.ic_medication_capsule_solid   // Placeholder
            "SYRUP", "SOLUTION", "LIQUID" -> R.drawable.ic_medication_syrup_solid // Placeholder
            "INJECTION" -> R.drawable.ic_medication_injection_solid // Placeholder
            "DROPS" -> R.drawable.ic_medication_drops_solid       // Placeholder
            "INHALER" -> R.drawable.ic_medication_inhaler_solid   // Placeholder
            "POWDER" -> R.drawable.ic_medication_powder_solid   // Placeholder (new)
            "OINTMENT", "CREAM", "GEL" -> R.drawable.ic_medication_cream_solid // Placeholder (new)
            "PATCH" -> R.drawable.ic_medication_patch_solid     // Placeholder (new)
            "SUPPOSITORY" -> R.drawable.ic_medication_suppository_solid // Placeholder (new)
            // Add other specific types as needed
            else -> R.drawable.ic_medication_default_solid // A default icon, ensure this exists
        }
    }
}
