package com.miguelprojects.myapplication.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.util.UserSessionManager
import kotlinx.coroutines.launch

typealias UserEntityCallback = (User?) -> Unit

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _userModel = MutableLiveData<UserModel>()
    val userModel: LiveData<UserModel> get() = _userModel

    private val _listUserModel = MutableLiveData<List<UserModel>>()
    val listUserModel: LiveData<List<UserModel>> get() = _listUserModel

    fun loadUserDataFirebaseFromRoom(offUserId: String, callback: (String) -> Unit) {
        repository.loadUserFirebaseFromRoom(offUserId) { userId ->
            callback(userId)
        }
    }

    fun saveUserRoom(
        userModel: UserModel,
        password: String,
        needsSync: Boolean,
        callback: (String) -> Unit
    ) {
        val salt = UserSessionManager.generateSalt()
        val hashedPassword = UserSessionManager.hashPassword(password, salt)
        val userEntity = userModel.toUserEntity(hashedPassword, salt)
        userEntity.needsSync = needsSync

        viewModelScope.launch {
            val userId = repository.saveUserRoom(userEntity)
            callback(userId)
        }
    }

    fun updateUserPasswordRoom(user: User, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val salt = UserSessionManager.generateSalt()
            val hashedPassword = UserSessionManager.hashPassword(user.password, salt)
            user.password = hashedPassword
            user.salt = salt

            repository.updateUserRoom(user) { res ->
                callback(
                    res,
                    if (res) "Dados atualizados no banco de dados com sucesso!" else "Erro ao atualizar os valores do banco de dados!"
                )
            }
        }
    }

    fun updateUserRoom(user: User, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.updateUserRoom(user) { res ->
                var message = ""
                if (res) {
                    message = "Dados atualizados no banco de dados com sucesso!"
                } else {
                    message = "Erro ao atualizar os valores do banco de dados!"
                }
                callback(res, message)
            }
        }
    }

    fun updateUserIdRoom(newUserId: String, oldUserId: String) {
        viewModelScope.launch {
            repository.updateUserIdRoom(newUserId, oldUserId)
        }
    }

    fun verifyEmailInFirebase(email: String, callback: (Boolean) -> Unit) {
        repository.verifyEmailInFirebase(email) {
            callback(it)
        }
    }

    fun verifyEmailInRoom(email: String, callback: UserEntityCallback) {
        viewModelScope.launch {
            repository.verifyEmailInRoom(email) { user ->
                callback(user)
            }
        }
    }

    fun loginUserRoom(email: String, password: String, callback: UserEntityCallback) {
        viewModelScope.launch {
            repository.loginUserRoom(email, password) { userLogged ->
                callback(userLogged)
            }
        }
    }

    fun loadUserRoom(offUserId: String, callback: UserEntityCallback) {
        viewModelScope.launch {
            repository.loadUserRoom(offUserId) { user ->
                callback(user)
            }
        }
    }

    fun loadUserModel(id: String) {
        repository.loadFirebase(id) { user ->
            _userModel.value = user
        }
    }

    fun saveUserModel(user: UserModel, callback: (Boolean) -> Unit) {
        repository.saveFirebase(user) { success ->
            if (success) {
                _userModel.value = user
                callback(true)
            } else {
                // Lidar com falha na gravação
                callback(false)
            }
        }
    }

    fun cleanUserModel() {
        _userModel.value = UserModel()
    }

    fun updateUserModel(userModel: UserModel, callback: (Boolean, String) -> Unit) {
        repository.updateUserFirebase(userModel) { res ->
            if (res) {
                callback(true, "Atualização bem-sucedida!")
            } else {
                callback(false, "Algo deu errado ao atualizar o usuario.")
                println("Algo deu errado ao atualizar o usuario.")
            }
        }
    }

    fun deleteUserAccount(id: String, callback: (Boolean, String) -> Unit) {
        repository.deleteUserAccountFirebase(id) { res, message ->
            if (res) {
                viewModelScope.launch {
                    repository.deleteWorkspacesForUserRoom(id)
                    repository.deleteWorkspacesAccessForUserRoom(id)

                    repository.deleteUserAccountRoom(id)
                    _userModel.value = UserModel()
                    callback(true, message)
                }
            } else {
                callback(false, message)
            }
        }
    }

    fun loadListUserModel(
        listUserId: List<String>,
        callback: (List<UserModel>) -> Unit
    ) {
        repository.loadListUsersFirebase(listUserId) { listUsers ->
            callback(listUsers)
        }
    }

    fun verifyExistsUserByOffId(offUserId: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val (exists, userId) = try {
                repository.verifyExistsUserByOffId(offUserId)
            } catch (e: Exception) {
                callback(false, null)
                return@launch
            }

            callback(exists, userId)
        }
    }

    fun synchronizeUser(
        sharedPreferences: SharedPreferences,
        offUserId: String,
        callback: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = repository.getUsersNeedingSync(offUserId)
                if (user != null) {
                    val (exists, userId) = try {
                        repository.verifyExistsUserByOffId(offUserId)
                    } catch (e: Exception) {
                        callback(false, "Erro ao verificar a existência do usuário.")
                        return@launch
                    }

                    repository.updateSyncStatus(offUserId, false)

                    if (exists) {
                        // Usuário existe
                        val userModel = User.toUserModel(user)
                        if (userId != null) {
                            userModel.id = userId

                            val existsUserIdInPreferences =
                                sharedPreferences.getString("user_id", null)
                            if (existsUserIdInPreferences != null) {
                                sharedPreferences.edit().apply {
                                    putString("user_id", userId)
                                    apply()
                                }
                            }

                            println("Dados salvos do userId")
                            repository.updateUserFirebase(userModel) { res ->
                                if (res) {
                                    callback(false, "Sincronização bem sucedida!")
                                } else {
                                    callback(false, "Erro ao sincronizar os dados do usuário.")
                                }
                            }
                        } else {
                            callback(false, "Erro ao captar id do usuário.")
                        }
                    } else {
                        // Usuário não existe
                        callback(false, "Usuário não existe.")
                    }
                } else {
                    callback(false, "Nenhum dado do usuário para sincronizar.")
                    println("Nenhum dado do usuário para sincronizar.")
                }
            } catch (e: Exception) {
                callback(false, "Erro ao sincronizar os dados do usuário.")
                println("Erro ao sincronizar os dados do usuário.")
            }
        }
    }


}
