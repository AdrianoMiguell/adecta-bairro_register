package com.miguelprojects.myapplication.model

import android.os.Parcel
import android.os.Parcelable
import com.miguelprojects.myapplication.room.entity.User

data class UserModel(
    var id: String = "",
    var username: String = "",
    var fullname: String = "",
    var email: String = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        // Leia outros campos conforme necessário
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(username)
        parcel.writeString(fullname)
        parcel.writeString(email)
        // Escreva outros campos conforme necessário
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WorkspaceModel> {
        override fun createFromParcel(parcel: Parcel): WorkspaceModel {
            return WorkspaceModel(parcel)
        }

        override fun newArray(size: Int): Array<WorkspaceModel?> {
            return arrayOfNulls(size)
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "username" to username,
            "fullname" to fullname,
            "email" to email,
        )
    }

    fun toUserEntity(password: String, salt: String): User {
        return User(
            id = id,
            firebaseId = id,
            username = username,
            fullname = fullname,
            email = email,
            password = password,
            salt = salt,
        )
    }
}
