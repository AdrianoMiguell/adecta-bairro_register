package com.miguelprojects.myapplication.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.miguelprojects.myapplication.model.CitizenModel
import java.util.UUID

@Entity(
    foreignKeys = [ForeignKey(
        entity = Workspace::class,
        parentColumns = ["id"],
        childColumns = ["workspaceId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Citizen(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(), // Gera um novo UUID como String se nenhum ID for fornecido
    var firebaseId: String? = null, // ID do Firebase (pode ser nulo para registros não sincronizados)
    val name: String,
    val telephone: String,
    val sex: String,
    val cpf: String,
    val sus: String?,
    var numberregister: String,
    val birthdate: Long,
    val fathername: String,
    val mothername: String,
    val birthplace: String,
    val cep: String,
    val state: String,
    val city: String,
    val neighborhood: String,
    val street: String,
    val numberhouse: Int,
    val addons: String?,
    val workspaceId: String,
    var needsSync: Boolean = false,
    var needsUpdate: Boolean = false,
    var active: Boolean = true,
    var isDelete: Boolean = false,
) {
    companion object {
        fun fromCitizenModel(citizenModel: CitizenModel, workspaceId: String): Citizen {
            return Citizen(
                id = citizenModel.id,
                firebaseId = citizenModel.id,
                name = citizenModel.name,
                telephone = citizenModel.telephone,
                sex = citizenModel.sex,
                cpf = citizenModel.cpf,
                sus = citizenModel.sus,
                numberregister = citizenModel.numberregister,
                birthdate = citizenModel.birthdate,
                fathername = citizenModel.fathername,
                mothername = citizenModel.mothername,
                birthplace = citizenModel.birthplace,
                cep = citizenModel.cep,
                state = citizenModel.state,
                city = citizenModel.city,
                neighborhood = citizenModel.neighborhood,
                street = citizenModel.street,
                numberhouse = citizenModel.numberhouse,
                addons = citizenModel.addons,
                active = citizenModel.active,
                workspaceId = workspaceId
            )
        }

        fun toListCitizen(list: List<CitizenModel>, workspaceId: String): MutableList<Citizen> {
            return list.mapTo(mutableListOf()) {
                fromCitizenModel(it, workspaceId)
            }
        }
    }
}