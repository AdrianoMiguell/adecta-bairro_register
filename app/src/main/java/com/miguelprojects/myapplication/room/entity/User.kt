package com.miguelprojects.myapplication.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miguelprojects.myapplication.model.UserModel
import java.util.UUID

@Entity
data class User(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(), // Gera um novo UUID como String se nenhum ID for fornecido
    var firebaseId: String? = null, // ID do Firebase (pode ser nulo para registros não sincronizados)
    val username: String,
    val fullname: String,
    val email: String,
    var password: String,
    var salt: String,
    var needsSync: Boolean = true // Campo para indicar que precisa ser sincronizado
) {
    companion object {
        fun toUserModel(user: User): UserModel {
            return UserModel(
                id = user.id,
                username = user.username,
                fullname = user.fullname,
                email = user.email
            )
        }
    }
}
