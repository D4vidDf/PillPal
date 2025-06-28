package com.d4viddf.medicationreminder.repository

import com.d4viddf.medicationreminder.data.MedicationSearchResult
import com.d4viddf.medicationreminder.data.MedicationInfo
import com.d4viddf.medicationreminder.data.MedicationInfoDao
import okhttp3.OkHttpClient
import android.util.Log
import com.d4viddf.medicationreminder.data.CimaDocumento
import com.d4viddf.medicationreminder.data.CimaEstado
import com.d4viddf.medicationreminder.data.CimaFormaFarmaceutica
import com.d4viddf.medicationreminder.data.CimaFoto
import com.d4viddf.medicationreminder.data.CimaMedicationDetail
import com.d4viddf.medicationreminder.data.CimaViaAdministracion
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class MedicationInfoRepository @Inject constructor(
    private val medicationInfoDao: MedicationInfoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val client = OkHttpClient()

    // Private helper function moved out from getMedicationDetailsByNRegistro and made non-inline
    private fun <T> parseJsonArrayToList(jsonArray: JSONArray?, parser: (JSONObject) -> T): List<T>? {
        if (jsonArray == null) return null
        val list = mutableListOf<T>()
        for (i in 0 until jsonArray.length()) {
            jsonArray.optJSONObject(i)?.let { // Ensure JSONObject is not null before parsing
                list.add(parser(it))
            }
        }
        return list.ifEmpty { null } // Keep original behavior of returning null for empty
    }

    suspend fun insertMedicationInfo(medicationInfo: MedicationInfo) {
        medicationInfoDao.insertMedicationInfo(medicationInfo)
    }

    suspend fun getMedicationInfoById(medicationId: Int): MedicationInfo? {
        return medicationInfoDao.getMedicationInfoById(medicationId)
    }

    // Enhanced Function to Search Medication from CIMA API with Pagination
    suspend fun searchMedication(query: String): List<MedicationSearchResult> = withContext(ioDispatcher) {
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

        searchResults // Return searchResults from withContext block
    }

    suspend fun getMedicationDetailsByNRegistro(nregistro: String): CimaMedicationDetail? = withContext(ioDispatcher) {
        val apiUrl = "https://cima.aemps.es/cima/rest/medicamento?nregistro=$nregistro"
        val request = Request.Builder().url(apiUrl).build()
        val TAG = "MedicationInfoRepo" // For logging

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Unsuccessful response for nregistro $nregistro: ${response.code}")
                return@withContext null
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Empty response body for nregistro $nregistro")
                return@withContext null
            }

            val jsonResponse = JSONObject(responseBody)

            val formaFarmaceuticaJson = jsonResponse.optJSONObject("formaFarmaceutica")
            val viasAdministracionJsonArray = jsonResponse.optJSONArray("viasAdministracion")
            val estadoJson = jsonResponse.optJSONObject("estado")
            val docsJsonArray = jsonResponse.optJSONArray("docs")
            val fotosJsonArray = jsonResponse.optJSONArray("fotos")

            CimaMedicationDetail(
                nregistro = jsonResponse.optString("nregistro", null),
                nombre = jsonResponse.optString("nombre", null),
                labtitular = jsonResponse.optString("labtitular", null),
                pactivos = jsonResponse.optString("pactivos", null), // Principios activos as a single string
                formaFarmaceutica = formaFarmaceuticaJson?.let {
                    CimaFormaFarmaceutica(id = it.optInt("id"), nombre = it.optString("nombre", null))
                },
                viasAdministracion = parseJsonArrayToList(viasAdministracionJsonArray) { viaJson ->
                    CimaViaAdministracion(id = viaJson.optInt("id"), nombre = viaJson.optString("nombre", null))
                },
                condPresc = jsonResponse.optString("condPresc", null),
                estado = estadoJson?.let { CimaEstado(aut = it.optLong("aut", -1L).takeIf { aut -> aut != -1L }) },
                docs = parseJsonArrayToList(docsJsonArray) { docJson ->
                    CimaDocumento(
                        tipo = docJson.optInt("tipo", -1).takeIf { tipo -> tipo != -1 },
                        urlHtml = docJson.optString("urlHtml", null),
                        url = docJson.optString("url", null)
                    )
                },
                fotos = parseJsonArrayToList(fotosJsonArray) { fotoJson ->
                    CimaFoto(
                        tipo = fotoJson.optString("tipo", null),
                        url = fotoJson.optString("url", null),
                        fecha = fotoJson.optLong("fecha", -1L).takeIf { fecha -> fecha != -1L }
                    )
                },
                comerc = jsonResponse.optBoolean("comerc"),
                conduc = jsonResponse.optBoolean("conduc"),
                triangulo = jsonResponse.optBoolean("triangulo"),
                huerfano = jsonResponse.optBoolean("huerfano"),
                biosimilar = jsonResponse.optBoolean("biosimilar")
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching or parsing medication details for nregistro $nregistro", e)
            null // Return null from withContext block
        }
    }
}
