package com.miguelprojects.myapplication.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.room.dao.UserDao
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.util.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resumeWithException

typealias UserEntityCallback = (User?) -> Unit

class UserRepository(private val userDao: UserDao) {

    suspend fun saveUserRoom(user: User): String {
        if (user.id.isEmpty()) {
            val id = UUID.randomUUID().toString()
            user.id = id
        } else {
            user.firebaseId = user.id
        }

        println(user)
        userDao.insert(user)
        return user.id
    }

    suspend fun verifyEmailInRoom(email: String, callback: UserEntityCallback) {
        val userEntity =
            userDao.getUserByEmail(email)
        callback(userEntity)
    }

    fun verifyEmailInFirebase(email: String, callback: (Boolean) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("users")
        reference.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val user = userSnapshot.getValue(UserModel::class.java)
                            if (user != null) {
                                println("Usuario existente: $user")
                                callback(true)
                            }
                            return
                        }
                    }
                    println("Processo para verificar email do usuario com problemas!")
                    callback(false) // userId não encontrado
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Processo para verificar email do usuario no firebase cancelado!")
                    callback(false)
                }
            })
    }

    suspend fun loginUserRoom(email: String, password: String, callback: UserEntityCallback) {
        // Lógica para buscar o usuário no banco de dados e verificar o login
        try {
            val userEntity =
                userDao.getUserByEmail(email) // Exemplo hipotético de busca de usuário por email

            Log.d(
                "No login User Room",
                "Dados do usuario senha: Password: $password ; Password Hash : ${userEntity?.password} ; salt: ${userEntity?.salt}"
            )
            // Verifique se o usuário existe e se a senha corresponde
            val loginSuccess =
                userEntity != null && UserSessionManager.verifyPassword(
                    password,
                    userEntity.password,
                    userEntity.salt
                )

            // Execute o callback com o resultado do login
            if (loginSuccess) {
                callback(userEntity)
            } else {
                callback(null)
            }
        } catch (e: Exception) {
            // Trate qualquer exceção que possa ocorrer, por exemplo, falha na consulta ao banco de dados
            Log.e("UserRepository", "Error logging in user: ${e.message}")
            callback(null) // Informe à camada superior que o login falhou
        }
    }

    fun loadUserFirebaseFromRoom(userId: String, callback: (String) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("users")
        reference.orderByChild("userId").equalTo(userId.toDouble())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val user = userSnapshot.getValue(UserModel::class.java)
                            if (user != null) {
                                callback(user.id)
                            }
                            return
                        }
                    }
                    println("Processo para pegar id do usuario com problemas!")
                    callback("") // userId não encontrado
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Processo para pegar id do usuario no firebase cancelado!")
                    callback("")
                }

            })
    }

    suspend fun loadUserRoom(userId: String, callback: UserEntityCallback) {
        val user = userDao.getUser(userId)
        callback(user)
    }

    fun saveFirebase(userModel: UserModel, callback: (Boolean) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("users")
        if (userModel.id.isEmpty()) {
            // Gerar um ID único automaticamente se não for fornecido
            userModel.id = reference.push().key ?: ""
        }

        reference.child(userModel.id).setValue(userModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("Dados salvos com sucesso!")
                callback(true)
            } else {
                task.exception?.let {
                    println("Erro ao salvar os dados: ${it.message}")
                    callback(false)
                }
            }
        }
    }
//
//    fun saveOffUserIdFirebase(userModel: UserModel) {
//        val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
//        if (userModel.offUserId.toString().isNotEmpty()) {
//            reference.child(userModel.id)
//                .updateChildren(mapOf("offUserId" to userModel.offUserId))
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        Log.d("Firebase", "Dados atualizados do off user id com sucesso!")
//                    } else {
//                        Log.d("Firebase", "Dados do off user id não atualizados!")
//                    }
//                }
//        }
//    }

    fun loadFirebase(id: String, callback: (UserModel) -> Unit) {
        val reference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("users").child(id)
        reference.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(UserModel::class.java)
                if (user != null) {
                    callback(user)
                } else {
                    callback(UserModel())
                }
            } else {
                callback(UserModel())
            }
        }.addOnFailureListener { exception ->
            println("Erro ao carregar os dados: ${exception.message}")
            callback(UserModel())
        }
    }

    fun updateUserFirebase(userModel: UserModel, callback: (Boolean) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("users").child(userModel.id)
        reference.updateChildren(userModel.toMap()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                callback(true)
                println("Atualização bem sucedida")
            } else {
                callback(false)
                println("Atualização mal sucedida")
            }
        }.addOnFailureListener { exception ->
            callback(false)
            println("Atualização mal sucedida: ${exception.message}")
        }
    }

    suspend fun updateUserRoom(user: User, callback: (Boolean) -> Unit) {
        val rowsUpdated = userDao.update(user)
        callback(rowsUpdated > 0)
    }

    suspend fun updateUserIdRoom(newUserId: String, oldUserId: String) {
        userDao.updateUserIdRoom(newUserId, oldUserId)
    }

    fun loadListUsersFirebase(listId: List<String>, callback: (List<UserModel>) -> Unit) {
        val reference = FirebaseDatabase.getInstance().getReference("users")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userList = mutableListOf<UserModel>()
                    for (users in dataSnapshot.children) {
                        val user = users.getValue(UserModel::class.java)
                        if (user != null) {
                            if (listId.contains(user.id)) {
                                userList.add(user)
                            }
                        }
                    }

                    callback(userList)
                } else {
                    callback(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }

        })
    }

    suspend fun verifyExistsUserByOffId(userId: String): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            val reference = FirebaseDatabase.getInstance().getReference("users")
            val dataSnapshot = suspendCancellableCoroutine<DataSnapshot> { continuation ->
                reference.orderByChild("userId")
                    .equalTo(userId.toDouble())
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            continuation.resume(snapshot) { }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            continuation.resumeWithException(error.toException())
                        }
                    })
            }

            // Verifica se algum dado foi encontrado
            if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                // Pegue o primeiro item e obtenha seu ID
                val firstChild = dataSnapshot.children.firstOrNull()
                val id = firstChild?.key
                println("Sem dados do usuário")
                Pair(true, id)
            } else {
                println("Com dados do usuário")
                Pair(false, null)
            }
        }
    }

    suspend fun getUsersNeedingSync(userId: String): User? {
        return userDao.getUsersNeedingSync(userId)
    }

    suspend fun getAllUsersNotNeedingSync(): List<User> {
        return userDao.getAllUsersNotNeedingSync()
    }

    suspend fun updateSyncStatus(userId: String, needsSync: Boolean) {
        userDao.updateSyncStatus(userId, needsSync)
    }

    private fun deleteWorkspacesForUserFirebase(userId: String, callback: (Boolean) -> Unit) {
        var control = true
        val workspaceReference = FirebaseDatabase.getInstance().getReference("workspaces")

        workspaceReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val workspacesToDelete = dataSnapshot.children.mapNotNull { workspaceSnapshot ->
                    workspaceSnapshot.getValue(WorkspaceModel::class.java)?.takeIf { workspace ->
                        workspace.userIds[userId] == true
                    }
                }

                if (workspacesToDelete.isEmpty()) {
                    callback(true)
                    return
                }

                workspacesToDelete.forEach { workspace ->
                    val userIds = workspace.userIds

                    if (userIds.size == 1 && workspace.creator == userId) {
                        workspaceReference.child(workspace.id).removeValue()
                            .addOnSuccessListener {
                                Log.d(
                                    "Delete All Workspaces Delete",
                                    "Workspace deletado com sucesso! ${workspace.id}"
                                )
                            }
                            .addOnFailureListener {
                                control = false
                                Log.d(
                                    "Delete All Workspaces Delete",
                                    "Erro ao deletar workspace ${workspace.id}"
                                )
                            }
                    } else {
                        if (workspace.creator == userId) {
                            val nextUserId = userIds.keys.elementAtOrNull(1) ?: ""

                            workspaceReference.child(workspace.id)
                                .updateChildren(mapOf("creator" to nextUserId))
                                .addOnSuccessListener {
                                    println("Criador atualizado para: $nextUserId.")
                                }.addOnFailureListener {
                                    control = false
                                    println("Falha ao atualizar criador para: $nextUserId.")
                                }
                        }

                        val pathToRemove = "userIds/$userId"
                        workspaceReference.child(workspace.id).child(pathToRemove)
                            .setValue(null)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    println("Membro $userId removido com sucesso.")
                                } else {
                                    control = false
                                    println("Falha ao remover membro $userId: ${task.exception?.message}")
                                }
                            }
                    }
                }

                // Chamando o callback após todas as operações
                callback(control)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("Get Workspaces Delete", "Nenhum valor de workspace para deletar")
                callback(false)
            }
        })
    }

    suspend fun deleteWorkspacesForUserRoom(userId: String) {
        val listWorkspaces = userDao.getUserWorkspaceAccess(userId)
        if (listWorkspaces.isEmpty()) return

        val listWorkspacesIds = listWorkspaces.map { it.id }.toSet().toList()
        userDao.deleteListUserWorkspaces(listWorkspacesIds)
    }

    suspend fun deleteWorkspacesAccessForUserRoom(userId: String) {
        userDao.deleteListUserWorkspacesAccess(userId)
    }

    fun deleteUserAccountFirebase(id: String, callback: (Boolean, String) -> Unit) {
        if (id.isNotEmpty()) {
            deleteWorkspacesForUserFirebase(id) { result ->
                if (result) {
                    val reference = FirebaseDatabase.getInstance().getReference("users").child(id)
                    reference.removeValue().addOnSuccessListener {
                        Log.d("delete account", "Conta deletada com sucesso no banco!")
                        // Redirecionar para a tela de login após a exclusão da conta
                        callback(true, "")
                    }.addOnFailureListener { exception ->
                        println("Erro ao deletar os dados: ${exception.message}")
                        callback(false, "Erro ao deletar a conta do usuário!")
                    }
                } else {
                    callback(false, "Tivemos um erro ao removê-lo os seus grupos, tente novamente!")
                }
            }
        } else {
            println("ID está nulo")
            callback(false, "Error, id nulo")
        }
    }

    suspend fun deleteUserAccountRoom(userId: String) {
        userDao.delete(userId)
    }

    suspend fun deleteListUsersRoom(listUser: List<User>) {
        try {
            userDao.deleteList(listUser)
        } catch (e: Exception) {
            Log.d("Delete list USer", "${e.message}")
        }
    }

}