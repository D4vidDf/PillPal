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

    fun getPluralResId(): Int {
        return when (this) {
            TABLET -> R.plurals.medication_form_tablet_plural
            PILL -> R.plurals.medication_form_pill_plural
            LIQUID -> R.plurals.medication_form_liquid_plural
            INJECTION -> R.plurals.medication_form_injection_plural
            INHALER -> R.plurals.medication_form_inhaler_plural
            DROPS -> R.plurals.medication_form_drops_plural
            SUPPOSITORY -> R.plurals.medication_form_suppository_plural
            POWDER -> R.plurals.medication_form_powder_plural
            OINTMENT -> R.plurals.medication_form_ointment_plural
            OTHER -> R.plurals.medication_form_other_plural
        }
    }

    fun getUnitPluralResId(): Int {
        return when (this) {
            TABLET -> R.plurals.unit_medication_form_tablet_plural
            PILL -> R.plurals.unit_medication_form_pill_plural
            LIQUID -> R.plurals.unit_medication_form_liquid_plural
            INJECTION -> R.plurals.unit_medication_form_injection_plural
            INHALER -> R.plurals.unit_medication_form_inhaler_plural
            DROPS -> R.plurals.unit_medication_form_drops_plural
            SUPPOSITORY -> R.plurals.unit_medication_form_suppository_plural
            POWDER -> R.plurals.unit_medication_form_powder_plural
            OINTMENT -> R.plurals.unit_medication_form_ointment_plural
            OTHER -> R.plurals.unit_medication_form_other_plural
        }
    }
}
