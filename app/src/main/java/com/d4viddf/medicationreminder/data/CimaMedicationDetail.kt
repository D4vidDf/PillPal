package com.d4viddf.medicationreminder.data

import android.os.Parcel
import android.os.Parcelable

// Main data class for CIMA medicamento endpoint response
data class CimaMedicationDetail(
    val nregistro: String?,
    val nombre: String?,
    val labtitular: String?,
    val pactivos: String?, // Principios activos, often a string
    val formaFarmaceutica: CimaFormaFarmaceutica?,
    val viasAdministracion: List<CimaViaAdministracion>?,
    val condPresc: String?, // Condiciones de prescripción
    val estado: CimaEstado?, // Estado de autorización
    val docs: List<CimaDocumento>?,
    val fotos: List<CimaFoto>?,
    val comerc: Boolean?, // Comercializado
    val conduc: Boolean?, // Afecta conducción
    val triangulo: Boolean?, // Triángulo negro
    val huerfano: Boolean?, // Medicamento huérfano
    val biosimilar: Boolean?
    // Add other fields as needed from CIMA documentation
)

data class CimaFormaFarmaceutica(
    val id: Int?, // Assuming there's an ID, though not specified, good practice
    val nombre: String?
)

data class CimaViaAdministracion(
    val id: Int?, // Assuming there's an ID
    val nombre: String?
)

data class CimaEstado(
    val aut: Long? // Fecha de autorización (timestamp)
    // Potentially a 'nombre' field for state description, but API usually returns 'aut' for date
)

data class CimaDocumento(
    val tipo: Int?, // 1: Ficha técnica, 2: Prospecto
    val urlHtml: String?,
    val url: String? // URL to PDF
    // seccTimestamp might also be useful
)
data class CimaFoto(
    val tipo: String?, // "materialAcondicionamientoPrimario", "materialAcondicionamientoSecundario"
    val url: String?, // URL to the image
    val fecha: Long? // Timestamp of the image
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Long::class.java.classLoader) as? Long
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(tipo)
        parcel.writeString(url)
        parcel.writeValue(fecha)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CimaFoto> {
        override fun createFromParcel(parcel: Parcel): CimaFoto {
            return CimaFoto(parcel)
        }

        override fun newArray(size: Int): Array<CimaFoto?> {
            return arrayOfNulls(size)
        }
    }
}
