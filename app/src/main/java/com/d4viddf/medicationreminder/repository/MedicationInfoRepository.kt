package com.d4viddf.medicationreminder.data

import MedicationSearchResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationInfoRepository @Inject constructor(
    private val medicationInfoDao: MedicationInfoDao
) {
    private val client = OkHttpClient()

    suspend fun insertMedicationInfo(medicationInfo: MedicationInfo) {
        medicationInfoDao.insertMedicationInfo(medicationInfo)
    }

    suspend fun getMedicationInfoById(medicationId: Int): MedicationInfo? {
        return medicationInfoDao.getMedicationInfoById(medicationId)
    }

    // Enhanced Function to Search Medication from CIMA API with Pagination
    suspend fun searchMedication(query: String): List<MedicationSearchResult> {
        val searchResults = mutableListOf<MedicationSearchResult>()
        var page = 1
        var moreResults = true

        while (moreResults) {
            val apiUrl = "https://cima.aemps.es/cima/rest/medicamentos?nombre=$query&pagina=$page"
            val request = Request.Builder()
                .url(apiUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                break
            }

            val responseBody = response.body?.string() ?: break
            val jsonResponse = JSONObject(responseBody)
            val resultsArray = jsonResponse.optJSONArray("resultados") ?: break

            if (resultsArray.length() == 0) {
                moreResults = false
                break
            }

            for (i in 0 until resultsArray.length()) {
                val resultJson = resultsArray.getJSONObject(i)

                // Extracting medication information
                val name = resultJson.optString("nombre", "Unknown")
                val atcCode = resultJson.optJSONObject("vtm")?.optString("nombre", "Unknown") ?: "Unknown"
                val description = resultJson.optJSONObject("formaFarmaceuticaSimplificada")?.optString("nombre", "Unknown") ?: "Unknown"
                val safetyNotes = resultJson.optString("cpresc", "No safety notes available")
                val nregistro = resultJson.optString("nregistro", "Unknown")
                val labtitular = resultJson.optString("labtitular", "Unknown")
                val comercializado = resultJson.optBoolean("comerc", false)
                val requiereReceta = resultJson.optBoolean("receta", false)
                val generico = resultJson.optBoolean("generico", false)

                // Construct imageUrl using the existing 'nregistro' variable
                val imageUrl = if (nregistro.isNotEmpty() && nregistro != "Unknown") {
                    "https://cima.aemps.es/cima/rest/medicamento/$nregistro/foto/materialAcondicionamientoPrimario"
                } else {
                    null
                }

                // Extracting document URLs
                val documentsArray = resultJson.optJSONArray("docs")
                val documentUrls = mutableListOf<String>()
                documentsArray?.let {
                    for (j in 0 until documentsArray.length()) {
                        val doc = documentsArray.getJSONObject(j)
                        val url = doc.optString("urlHtml", null)
                        if (url != null) {
                            documentUrls.add(url)
                        }
                    }
                }

                // Extracting administration routes
                val administrationRoutesArray = resultJson.optJSONArray("viasAdministracion")
                val administrationRoutes = mutableListOf<String>()
                administrationRoutesArray?.let {
                    for (j in 0 until administrationRoutesArray.length()) {
                        val route = administrationRoutesArray.getJSONObject(j).optString("nombre", "Unknown")
                        administrationRoutes.add(route)
                    }
                }

                // Extracting dosage information
                val dosage = resultJson.optString("dosis", "Not specified")

                val searchResult = MedicationSearchResult(
                    name = name,
                    atcCode = atcCode,
                    description = description,
                    safetyNotes = safetyNotes,
                    documentUrls = documentUrls,
                    administrationRoutes = administrationRoutes,
                    dosage = dosage,
                    nregistro = nregistro,
                    labtitular = labtitular,
                    comercializado = comercializado,
                    requiereReceta = requiereReceta,
                    generico = generico,
                    imageUrl = imageUrl // Added imageUrl parameter
                )
                searchResults.add(searchResult)
            }

            page++
        }

        return searchResults
    }
}
