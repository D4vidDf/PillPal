package com.d4viddf.medicationreminder.data.model

import androidx.annotation.StringRes
import com.d4viddf.medicationreminder.R

enum class MedicationForm(
    @StringRes val nameResId: Int,
    val imageUrl: Int
) {
    TABLET(R.string.medication_form_tablet, R.drawable.ic_med_pill),
    PILL(R.string.medication_form_pill, R.drawable.ic_med_capsule),
    LIQUID(R.string.medication_form_liquid, R.drawable.ic_med_syrup),
    INJECTION(R.string.medication_form_injection, R.drawable.ic_med_injection),
    INHALER(R.string.medication_form_inhaler, R.drawable.ic_med_inhaler),
    DROPS(R.string.medication_form_drops, R.drawable.ic_med_drops),
    SUPPOSITORY(R.string.medication_form_suppository, R.drawable.ic_pill_placeholder),
    POWDER(R.string.medication_form_powder, R.drawable.ic_pill_placeholder),
    OINTMENT(R.string.medication_form_ointment, R.drawable.ic_pill_placeholder),
    OTHER(R.string.medication_form_other, R.drawable.ic_pill_placeholder);
}
