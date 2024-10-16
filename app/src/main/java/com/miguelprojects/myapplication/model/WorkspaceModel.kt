package com.miguelprojects.myapplication.model

import android.os.Parcel
import android.os.Parcelable
import com.miguelprojects.myapplication.room.entity.Workspace
import com.miguelprojects.myapplication.room.entity.WorkspaceWithAccess

data class WorkspaceModel(
    var id: String = "",
    var name: String = "",
    var description: String = "",
    var cep: String = "",
    var state: String = "",
    var city: String = "",
    var neighborhood: String = "",
    var public: Boolean = false,
    var inviteCode: String = "",
    var creator: String = "",
    var userIds: MutableMap<String, Boolean> = mutableMapOf(), // Alteração aqui para MutableMap
) : Parcelable {
    constructor(parcel: Parcel) : this(
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
        parcel.readHashMap(Boolean::class.java.classLoader) as MutableMap<String, Boolean> ?: mutableMapOf()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(cep)
        parcel.writeString(state)
        parcel.writeString(city)
        parcel.writeString(neighborhood)
        parcel.writeByte(if (public) 1 else 0)
        parcel.writeString(inviteCode)
        parcel.writeString(creator)
        parcel.writeMap(userIds)
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

        fun fromEntity(workspaceEntity: Workspace): WorkspaceModel {
            return WorkspaceModel(
                id = workspaceEntity.id,
                name = workspaceEntity.name,
                description = workspaceEntity.description!!,
                cep = workspaceEntity.cep,
                state = workspaceEntity.state,
                city = workspaceEntity.city,
                neighborhood = workspaceEntity.neighborhood,
                public = workspaceEntity.public,
                inviteCode = workspaceEntity.inviteCode,
                creator = workspaceEntity.creator!!,
            )
        }

        fun fromEntityList(listWorkspaceEntity: List<Workspace>): MutableList<WorkspaceModel> {
            return listWorkspaceEntity.mapTo(mutableListOf()) { fromEntity(it) }
        }

        fun fromWorkspaceWithAccessList(workspacesWithAccess: List<WorkspaceWithAccess>): List<WorkspaceModel> {
            return workspacesWithAccess.map { workspaceWithAccess ->
                fromEntity(workspaceWithAccess.workspace)
            }
        }
    }

    fun toWorkspaceEntity(needsSync: Boolean): Workspace {
        return Workspace(
            id = this.id,
            firebaseId = this.id,
            name = this.name,
            description = this.description,
            cep = this.cep,
            state = this.state,
            city = this.city,
            neighborhood = this.neighborhood,
            public = this.public,
            inviteCode = this.inviteCode,
            creator = this.creator,
            needsSync = needsSync
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "cep" to cep,
            "state" to state,
            "city" to city,
            "neighborhood" to neighborhood,
            "public" to public,
            "inviteCode" to inviteCode,
            "creator" to creator,
        )
    }

    fun modelIsEmpty(): Boolean {
        return name.isEmpty() &&
                description.isEmpty() &&
                cep.isEmpty() &&
                state.isEmpty() &&
                city.isEmpty() &&
                neighborhood.isEmpty() &&
                inviteCode.isEmpty() &&
                creator.isEmpty()
//                userIds.isEmpty()
    }
}
