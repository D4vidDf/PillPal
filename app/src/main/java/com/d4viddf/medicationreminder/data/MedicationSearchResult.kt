import android.os.Parcel
import android.os.Parcelable

data class MedicationSearchResult(
    val name: String,
    val atcCode: String?,
    val description: String?,
    val safetyNotes: String?,
    val documentUrls: List<String>,
    val administrationRoutes: List<String>,
    val dosage: String?,
    val nregistro: String?,
    val labtitular: String?,
    val comercializado: Boolean,
    val requiereReceta: Boolean,
    val generico: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList()!!,
        parcel.createStringArrayList()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(), // Corrected line
        parcel.readByte() != 0.toByte(), // Corrected line
        parcel.readByte() != 0.toByte()  // Corrected line
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(atcCode)
        parcel.writeString(description)
        parcel.writeString(safetyNotes)
        parcel.writeStringList(documentUrls)
        parcel.writeStringList(administrationRoutes)
        parcel.writeString(dosage)
        parcel.writeString(nregistro)
        parcel.writeString(labtitular)
        parcel.writeByte(if (comercializado) 1.toByte() else 0.toByte()) // Corrected line
        parcel.writeByte(if (requiereReceta) 1.toByte() else 0.toByte()) // Corrected line
        parcel.writeByte(if (generico) 1.toByte() else 0.toByte()) // Corrected line
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MedicationSearchResult> {
        override fun createFromParcel(parcel: Parcel): MedicationSearchResult {
            return MedicationSearchResult(parcel)
        }

        override fun newArray(size: Int): Array<MedicationSearchResult?> {
            return arrayOfNulls(size)
        }
    }
}