package com.miguelprojects.myapplication.room.entity

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Workspace(
    @PrimaryKey
    var id: String = UUID.randomUUID()
        .toString(), // Gera um novo UUID como String se nenhum ID for fornecido
    var firebaseId: String? = null, // ID do Firebase (pode ser nulo para registros não sincronizados)
    var name: String = "",
    var description: String? = null,
    var cep: String = "",
    var state: String = "",
    var city: String = "",
    var neighborhood: String = "",
    var public: Boolean = false,
    var inviteCode: String = "",
    var creator: String? = "",
    var needsSync: Boolean = false,
    var needsUpdate: Boolean = false,
    @Ignore val userAccessIds: List<String> = emptyList() // Não salva no banco, apenas para uso em memória
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.createStringArrayList() ?: emptyList(), // Lê a lista de IDs de usuário
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(firebaseId)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(cep)
        parcel.writeString(state)
        parcel.writeString(city)
        parcel.writeString(neighborhood)
        parcel.writeByte(if (public == true) 1 else 0)
        parcel.writeString(inviteCode)
        parcel.writeString(creator)
        parcel.writeByte(if (needsSync) 1 else 0)
        parcel.writeByte(if (needsUpdate) 1 else 0)
        parcel.writeStringList(userAccessIds) // Escreve a lista de IDs de usuário
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Workspace> {
        override fun createFromParcel(parcel: Parcel): Workspace {
            return Workspace(parcel)
        }

        override fun newArray(size: Int): Array<Workspace?> {
            return arrayOfNulls(size)
        }
    }

    fun entityNotEmpty(): Boolean {
        return name.isNotEmpty() &&
                cep.isNotEmpty() &&
                state.isNotEmpty() &&
                city.isNotEmpty() &&
                neighborhood.isNotEmpty() &&
                inviteCode.isNotEmpty() &&
                creator!!.isNotEmpty()
    }
}

