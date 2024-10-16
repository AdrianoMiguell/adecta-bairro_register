package com.miguelprojects.myapplication.model

import android.os.Parcel
import android.os.Parcelable
import com.miguelprojects.myapplication.room.entity.Citizen

data class CitizenModel(
    var id: String = "",
    var name: String = "",
    var telephone: String = "",
    var sex: String = "",
    var cpf: String = "",
    var sus: String? = null,
    var numberregister: String = "",
    var birthdate: Long = 0L, //timestamp
    var fathername: String = "",
    var mothername: String = "",
    var birthplace: String = "",
    var cep: String = "",
    var state: String = "",
    var city: String = "",
    var neighborhood: String = "",
    var street: String = "",
    var numberhouse: Int = 0,
    var addons: String? = null,
    var active: Boolean = true
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong() ?: 0L,
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt() ?: 0,
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(telephone)
        parcel.writeString(sex)
        parcel.writeString(cpf)
        parcel.writeString(sus)
        parcel.writeString(numberregister)
        parcel.writeLong(birthdate)
        parcel.writeString(fathername)
        parcel.writeString(mothername)
        parcel.writeString(birthplace)
        parcel.writeString(cep)
        parcel.writeString(state)
        parcel.writeString(city)
        parcel.writeString(neighborhood)
        parcel.writeString(street)
        parcel.writeInt(numberhouse)
        parcel.writeString(addons)
        parcel.writeByte(if (active) 1 else 0)
        // Escreva outros campos conforme necessário
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CitizenModel) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CitizenModel> {
        override fun createFromParcel(parcel: Parcel): CitizenModel {
            return CitizenModel(parcel)
        }

        override fun newArray(size: Int): Array<CitizenModel?> {
            return arrayOfNulls(size)
        }

        fun fromEntity(citizen: Citizen): CitizenModel {
            return CitizenModel(
                id = citizen.id,
                name = citizen.name,
                telephone = citizen.telephone,
                sex = citizen.sex,
                cpf = citizen.cpf,
                sus = citizen.sus,
                numberregister = citizen.numberregister,
                birthdate = citizen.birthdate,
                fathername = citizen.fathername,
                mothername = citizen.mothername,
                birthplace = citizen.birthplace,
                cep = citizen.cep,
                state = citizen.state,
                city = citizen.city,
                neighborhood = citizen.neighborhood,
                street = citizen.street,
                numberhouse = citizen.numberhouse,
                addons = citizen.addons,
                active = citizen.active,
            )
        }

        fun fromEntityList(listCitizenEntity: List<Citizen>): MutableList<CitizenModel> {
            return listCitizenEntity.mapTo(mutableListOf()) { fromEntity(it) }
        }
    }

    fun modelNotEmpty(): Boolean {
        return name.isNotEmpty() &&
                telephone.isNotEmpty() &&
                sex.isNotEmpty() &&
                cpf.isNotEmpty() &&
                numberregister.isNotEmpty() &&
                birthdate != 0L &&
                fathername.isNotEmpty() &&
                mothername.isNotEmpty() &&
                birthplace.isNotEmpty() &&
                cep.isNotEmpty() &&
                state.isNotEmpty() &&
                city.isNotEmpty() &&
                neighborhood.isNotEmpty() &&
                street.isNotEmpty()
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "telephone" to telephone,
            "sex" to sex,
            "cpf" to cpf,
            "sus" to sus,
            "numberregister" to numberregister,
            "birthdate" to birthdate,
            "fathername" to fathername,
            "mothername" to mothername,
            "birthplace" to birthplace,
            "cep" to cep,
            "state" to state,
            "city" to city,
            "neighborhood" to neighborhood,
            "street" to street,
            "numberhouse" to numberhouse,
            "addons" to addons,
            "active" to active
        )
    }
}
