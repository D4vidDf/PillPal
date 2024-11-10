data class MedicationSearchResult(
    val name: String,
    val atcCode: String?,
    val description: String?,
    val safetyNotes: String?,
    val documentUrls: List<String>,
    val administrationRoutes: List<String>, // List of administration routes
    val dosage: String?,                    // Dosage information
    val nregistro: String?,                 // Medication registration number
    val labtitular: String?,                // Laboratory titular name
    val comercializado: Boolean,            // Whether the medication is marketed
    val requiereReceta: Boolean,            // Whether the medication requires a prescription
    val generico: Boolean                   // Whether the medication is generic
)