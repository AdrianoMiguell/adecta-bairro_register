package com.miguelprojects.myapplication.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.ui.activitys.users.LoginActivity
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom

object UserSessionManager {
    fun checkIfLogged(activity: AppCompatActivity) {
        println("Está no checkiflogged")
        val sharedPreferences = activity.getSharedPreferences("login", Context.MODE_PRIVATE)
        val storedLogged = sharedPreferences.getBoolean("logged", false)
        if (storedLogged) {
            activity.startActivity(Intent(activity.baseContext, MainActivity::class.java))
            activity.finish()
        } else {
            return
        }
    }

    fun onUserNotFoundOrLogout(activity: AppCompatActivity, userViewModel: UserViewModel) {
        val sharedPreferences = activity.getSharedPreferences("login", Context.MODE_PRIVATE)

//        deleteAllOffData(sharedPreferences) { res ->
//            println(if (res) "Operação bem sucedida!" else "Operação mal sucedida!")
        println("No onUserNotFoundOrLogout")

        val editor = sharedPreferences.edit()
        editor.putString("user_id", "")
        editor.putBoolean("logged", false)
        editor.apply()

        Firebase.auth.signOut()
        userViewModel.cleanUserModel()

        activity.startActivity(
            Intent(activity, LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        activity.finish()
    }

    private fun deleteAllOffData(
        sharedPreferences: SharedPreferences,
        callback: (Boolean) -> Unit
    ) {
        val storedUserId = sharedPreferences.getString("user_id", "")!!

        val database = MyApplication().database
        val userDao = database.userDao()

        val userRepository = UserRepository(userDao)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userRepository.deleteUserAccountRoom(storedUserId)
                callback(true)
            } catch (e: Exception) {
                Log.d(
                    "deleteAllOffData",
                    "Erro na execução da função de deletação de todos os dados: ${e.message}"
                )
                callback(false)
            }
        }
    }

    fun checkAndLoadUser(
        context: Context,
        activity: AppCompatActivity,
        userViewModel: UserViewModel,
        callback: (Boolean, UserModel?, User?) -> Unit
    ) {
        val networkChangeReceiver = NetworkChangeReceiver()
        val sharedPreferences = activity.getSharedPreferences("login", Context.MODE_PRIVATE)
        val storedUserId = sharedPreferences.getString("user_id", null)

        if (storedUserId != null) {
            if (networkChangeReceiver.isNetworkConnected(context)) {
                userViewModel.loadUserModel(storedUserId)
                userViewModel.userModel.observe(activity, Observer { userModel ->
                    if (userModel != null) {
                        Log.d(
                            "UserSessionManager", "ID de usuário encontrado no SharedPreferences"
                        )
                        callback(true, userModel, null)
                    } else {
                        Log.d("UserSessionManager", "Falha ao carregar dados do usuário")
                        onUserNotFoundOrLogout(activity, userViewModel)
                    }
                })
            } else {
                userViewModel.loadUserRoom(storedUserId) { user ->
                    if (user != null) {
                        Log.d(
                            "UserSessionManager", "ID de usuário encontrado no SharedPreferences"
                        )
                        callback(true, null, user)
                    } else {
                        Log.d("UserSessionManager", "Falha ao carregar dados do usuário")
                        onUserNotFoundOrLogout(activity, userViewModel)
                    }
                }
            }
        } else {
            onUserNotFoundOrLogout(activity, userViewModel)
            Log.d(
                "UserSessionManager", "Nenhum ID de usuário encontrado no SharedPreferences"
            )
        }
    }

    //    Quando usar? Usar quando for fazer alterações significativas como de edição e delete.
    fun accessSecurityCheck(
        userId: String,
        workspaceId: String,
        workspaceViewModel: WorkspaceViewModel,
        callback: (Boolean) -> Unit
    ) {
        if (userId.isEmpty() || workspaceId.isEmpty()) {
            callback(false)
            Log.d(
                "Teste de Acesso - User Session Manager",
                "Deu ruim pois workspaceId e userId não existem."
            )
        } else {
            workspaceViewModel.accessSecurityCheck(userId, workspaceId) { res ->
                if (res) {
                    callback(true)
                    Log.d(
                        "Teste de Acesso - User Session Manager", "Deu bom no workspaceViewModel."
                    )
                } else {
                    callback(false)
                    Log.d(
                        "Teste de Acesso - User Session Manager", "Deu ruim no workspaceViewModel."
                    )
                }
            }
        }
    }

    fun confirmUserPassword(
        context: Context,
        userViewModel: UserViewModel,
        email: String,
        password: String,
        callback: (Boolean, String, FirebaseUser?) -> Unit
    ) {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            val networkChangeReceiver = NetworkChangeReceiver()

            if (networkChangeReceiver.isNetworkConnected(context)) {
                val mAuth = FirebaseAuth.getInstance()
                val user = mAuth.currentUser

                if (user != null) {
                    Log.d("ConfirmUserPassword", "Tentando fazer login com $email: $password")
                    val credential = EmailAuthProvider.getCredential(email, password)

                    user.reauthenticate(credential).addOnCompleteListener {
                        Log.d("Teste re-autenticação", "User re-authenticated.")
                    }.addOnSuccessListener {
                        Log.d("ConfirmUserPassword", "Login bem-sucedido para $email")
                        callback(true, "", user)
                    }.addOnFailureListener { task ->
                        val error = when (task) {
                            is FirebaseAuthInvalidCredentialsException -> "Email ou senha inválidos!"
                            is FirebaseAuthInvalidUserException -> "Usuário não encontrado ou desativado!"
                            is FirebaseAuthUserCollisionException -> "Usuário já existe!"
                            is FirebaseAuthRecentLoginRequiredException -> "A reautenticação é necessária. Por favor, faça login novamente!"
                            is FirebaseNetworkException -> "Erro de rede. Por favor, verifique sua conexão!"
                            else -> "Erro ao efetuar o login"
                        }
                        Log.d("ConfirmUserPassword", "Falha ao efetuar o login: $error")
                        callback(false, error, null)
                    }
                } else {
                    Log.d("ConfirmUserPassword", "Usuário não está logado.")
                    callback(false, "Usuário não está logado.", null)
                }
            } else {
                Log.d("ConfirmUserPassword", "Está sem internet.")
                userViewModel.loginUserRoom(email, password) { user ->
                    if (user != null) {
                        Log.d("ConfirmUserPassword", "Login bem-sucedido para $email")
                        callback(true, "", null)
                    } else {
                        callback(false, "Campos de Email ou Senha vazios.", null)
                    }
                }
            }
        } else {
            Log.d("ConfirmUserPassword", "Campos de Email ou Senha vazios.")
            callback(false, "Campos de Email ou Senha vazios.", null)
        }
    }

    fun hashPassword(password: String, salt: String): String {
        println("No Hash password : $password, $salt")
        val md = MessageDigest.getInstance("SHA-256")
        val saltedPassword = password + salt
        val hashedBytes = md.digest(saltedPassword.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, hashedPassword: String, salt: String): Boolean {
        println("No verifyPassword")
        val testHashedPassword = hashPassword(password, salt)
        println(" hashPassword(password, salt) == hashedPassword -> $testHashedPassword = $hashedPassword")
        return testHashedPassword == hashedPassword
    }

    fun verifyExistsEmailInBases(
        context: Context,
        userViewModel: UserViewModel,
        email: String,
        callback: (Boolean) -> Unit
    ) {
        if (NetworkChangeReceiver().isNetworkConnected(context)) {
            userViewModel.verifyEmailInFirebase(email) { res ->
                if (res) {
                    println("Existe no firebase realtime")
                    callback(true)
                } else {
//                    val mAuth = FirebaseAuth.getInstance()
//                    mAuth.fetchSignInMethodsForEmail(email)
//                        .addOnCompleteListener { task ->
//                            if (task.isSuccessful) {
//                                println("Existe no firebase auth")
//                                callback(true)
//                            } else {
                    userViewModel.verifyEmailInRoom(email) { userEntity ->
                        if (userEntity != null) {
                            println("Existe no firebase room")
                        } else {
                            println("Não existe")
                        }
                        callback(userEntity != null)
                    }
//                            }
//                        }
                }
            }
        } else {
            userViewModel.verifyEmailInRoom(email) { userEntity ->
                callback(userEntity != null)
            }
        }
    }

    fun verifyExistsEmailInFirebase(
        userViewModel: UserViewModel,
        email: String,
        callback: (Boolean) -> Unit
    ) {
        userViewModel.verifyEmailInFirebase(email) { res ->
            callback(res)
        }
    }

    fun verifyExistsEmailInRoom(
        userViewModel: UserViewModel,
        email: String,
        callback: (Boolean) -> Unit
    ) {
        userViewModel.verifyEmailInRoom(email) { userEntity ->
            callback(userEntity != null)
        }
    }

    fun registerUserAccountFirebase(
        activity: AppCompatActivity,
        userViewModel: UserViewModel,
        userModel: UserModel,
        password: String,
        callback: (Boolean, String, String) -> Unit
    ) {
        val mAuth = FirebaseAuth.getInstance()

        mAuth.createUserWithEmailAndPassword(userModel.email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = mAuth.currentUser
                    if (firebaseUser != null) {
                        val userId = firebaseUser.uid
                        userModel.id = userId

                        userViewModel.saveUserModel(userModel) { res ->
                            if (res) {
                                callback(true, "Cadastro realizado com sucesso!", userId)
                            } else {
                                callback(false, "Erro ao salvar os dados", "")
                            }
                        }
                    }
                } else {
                    val error = getErrorMessage(task.exception ?: Exception("Unknown error"))
                    Log.d("registerUserAccountFirebase", error)
                    callback(false, error, "")
                }
            }
    }

    fun loginUserAccount(
        activity: AppCompatActivity,
        email: String,
        password: String,
        userViewModel: UserViewModel,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        val sharedPreferences = activity.getSharedPreferences("login", Context.MODE_PRIVATE)
        val mAuth = FirebaseAuth.getInstance()

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = mAuth.currentUser

                if (firebaseUser != null) {
                    println("usuario firebase encontrado: ${firebaseUser.uid}")
                    val userId = firebaseUser.uid ?: ""
                    saveUserIdToPreferences(sharedPreferences, userId, checkboxSaveLogin)
                    observeUserModel(
                        activity,
                        userViewModel,
                        userId,
                        email,
                        password,
                        sharedPreferences,
                        checkboxSaveLogin,
                        callback
                    )
                    userViewModel.loadUserModel(userId)
                } else {
                    callback(false, "Erro ao realizar login, verifique se os dados estão corretos!")
                    return@addOnCompleteListener
                }
            } else {
                handleLoginError(
                    task.exception,
                    userViewModel,
                    activity,
                    email,
                    password,
                    sharedPreferences,
                    checkboxSaveLogin,
                    callback
                )
            }
        }
    }

    private fun saveUserIdToPreferences(
        sharedPreferences: SharedPreferences, userId: String, checkboxSaveLogin: Boolean
    ) {
        sharedPreferences.edit().apply {
            putString("user_id", userId)
            putBoolean("logged", checkboxSaveLogin)
            apply()
        }
    }

    private fun observeUserModel(
        activity: AppCompatActivity,
        userViewModel: UserViewModel,
        userId: String,
        email: String,
        password: String,
        sharedPreferences: SharedPreferences,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        userViewModel.userModel.observe(activity) { userModel ->
            if (userModel != null && userModel.id.isNotEmpty()) {
                println("No observer: $userModel")
                loginUserToRoom(
                    activity,
                    userViewModel,
                    password,
                    userModel,
                    sharedPreferences,
                    checkboxSaveLogin,
                    callback
                )
            } else {
//                função para criar os dados do usuario no firebase realtime caso não existam nele, apenas os dados no authentic
                createUserModelAndSave(
                    activity,
                    userViewModel,
                    userId,
                    email,
                    password,
                    sharedPreferences,
                    checkboxSaveLogin,
                    callback
                )
            }

            userViewModel.userModel.removeObservers(activity)
        }
    }

    fun loginUserToRoom(
        context: Context,
        userViewModel: UserViewModel,
        password: String,
        userModel: UserModel,
        sharedPreferences: SharedPreferences,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        // Tenta fazer o login no Room
        userViewModel.loginUserRoom(userModel.email, password) { userLogged ->
            println("No user login")
            val isConnected = NetworkChangeReceiver().isNetworkConnected(context)
            println("userLogged -> $userLogged")
            println("userModel -> $userModel")

            if (userLogged != null) {
                println("Login realizado com sucesso!")

                // Verifica se o usuário precisa ser atualizado
                if (userLogged.id.isNotEmpty() && userLogged.firebaseId.isNullOrEmpty() && isConnected) {
                    println("Update do user")
                    userViewModel.updateUserIdRoom(userModel.id, userLogged.id)
                }

                // Atualiza o status de sincronização do usuário
                if (userLogged.needsSync && isConnected) {
                    userLogged.needsSync = false
                    userViewModel.updateUserRoom(userLogged) { _, _ -> }
                }

                // Salva o ID do usuário nas preferências
                saveUserIdToPreferences(
                    sharedPreferences,
                    userModel.id.ifEmpty { userLogged.id },
                    checkboxSaveLogin
                )

                callback(true, "Login realizado com sucesso!")
                return@loginUserRoom

            } else if (!isConnected) {
                callback(
                    false,
                    "Erro ao executar login. Certifique-se de que os dados informados estão corretos!"
                )
                return@loginUserRoom
            } else {
                // Se o login falhou, verifica se o e-mail está presente no Room
                userViewModel.verifyEmailInRoom(userModel.email) { user ->
                    // Se não há conexão ou o usuário não foi encontrado
                    if (user != null) {
                        println("Erro ao verificar se foi falha pois senha foi mudada online ou se errou mesmo")
                        handleUserNotRegisteredRoom(
                            userViewModel,
                            user,
                            password,
                            sharedPreferences,
                            checkboxSaveLogin,
                            callback
                        )
                    } else {
                        // Se o usuário existe mas não está cadastrado no off
                        createUserRoomAndSave(
                            context,
                            userViewModel,
                            userModel,
                            password,
                            sharedPreferences,
                            checkboxSaveLogin,
                            callback
                        )
                    }
                }
            }
        }
    }

    // Função para lidar com usuários não registrados
    private fun handleUserNotRegisteredRoom(
        userViewModel: UserViewModel,
        userEntity: User,
        password: String,
        sharedPreferences: SharedPreferences,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        // Atualiza o ID e a senha do usuário offline
        userEntity.password = password

        println("Nova senha : Password = $password")

        userViewModel.updateUserPasswordRoom(userEntity) { res, message ->
            // Salva o ID do usuário nas preferências
            saveUserIdToPreferences(sharedPreferences, userEntity.id, checkboxSaveLogin)
            println(message)
            callback(
                res,
                if (res) "Login realizado com sucesso!" else "Erro ao realizar esta ação."
            )
        }
    }

    private fun createUserModelAndSave(
        context: Context,
        userViewModel: UserViewModel,
        userId: String,
        email: String,
        password: String,
        sharedPreferences: SharedPreferences,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        val newUser = UserModel(id = userId, email = email)
        userViewModel.saveUserModel(newUser) { isSaved ->
            if (isSaved) {
                loginUserToRoom(
                    context,
                    userViewModel,
                    password,
                    newUser,
                    sharedPreferences,
                    checkboxSaveLogin,
                    callback
                )
            } else {
                callback(false, "Erro ao salvar dados do usuário no banco!")
            }
        }
    }

    private fun createUserRoomAndSave(
        context: Context,
        userViewModel: UserViewModel,
        newUser: UserModel,
        password: String,
        sharedPreferences: SharedPreferences,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        var needsSync = !NetworkChangeReceiver().isNetworkConnected(context)

        userViewModel.saveUserRoom(newUser, password, needsSync) { id ->
            saveUserIdToPreferences(sharedPreferences, id, checkboxSaveLogin)
            callback(true, "Login realizado com sucesso!")
        }
    }

    private fun handleLoginError(
        exception: Exception?,
        userViewModel: UserViewModel,
        activity: AppCompatActivity,
        email: String,
        password: String,
        sharedPreferences: SharedPreferences,
        checkboxSaveLogin: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        println("Chegou no handle")
        println("$email, $password")
        userViewModel.loginUserRoom(email, password) { userLogged ->
            println(userLogged)
            if (userLogged != null) {
                val userModel = User.toUserModel(userLogged)
                println(userModel)

                registerUserAccountFirebase(
                    activity,
                    userViewModel,
                    userModel,
                    password
                ) { resRegister, messageRegister, regUserId ->
                    if (resRegister) {
                        val mAuth = FirebaseAuth.getInstance()
                        mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                Log.d("task.isSuccessful", "${task.isSuccessful}")
                                if (task.isSuccessful) {
                                    val firebaseUser = mAuth.currentUser
                                    val userId = firebaseUser?.uid ?: ""
                                    val oldId = userLogged.id

                                    println("regUserId: $regUserId | userId: $userId | oldUserId: $oldId")

                                    saveUserIdToPreferences(
                                        sharedPreferences,
                                        userId,
                                        checkboxSaveLogin
                                    )

                                    println("Update do user id")
                                    userLogged.id = userId
                                    userLogged.firebaseId = userId

                                    userViewModel.updateUserIdRoom(userId, oldId)

                                    // Atualiza o status de sincronização do usuário
                                    if (userLogged.needsSync) {
                                        userLogged.needsSync = false
                                        userViewModel.updateUserRoom(userLogged) { _, _ -> }
                                    }

                                    callback(true, "Login realizado com sucesso!")
                                } else {
                                    callback(false, task.exception?.message!!)
                                }
                            }
                    } else {
                        Log.d(
                            "Login verify in registerUserAccountFirebase",
                            "Erro ao executar registro (Devido a existencia dos dados no off) : $messageRegister"
                        )
                        callback(
                            false,
                            "Erro ao executar esta ação. Certifique-se de que os dados estão corretos!"
                        )
                    }
                }

            } else {
                callback(false, getErrorMessage(exception))
            }
        }
    }

    fun getErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Email ou senha inválidos!"
            is FirebaseAuthUserCollisionException -> "Usuário já existe"
            is FirebaseAuthInvalidUserException -> "Usuário não encontrado ou desativado!"
            is FirebaseAuthRecentLoginRequiredException -> "A reautenticação é necessária. Por favor, faça login novamente!"
            is FirebaseNetworkException -> "Erro de rede. Por favor, verifique sua conexão!"
            is FirebaseAuthWeakPasswordException -> "A senha fornecida é fraca!"
            else -> "Erro ao executar esta ação. Certifique-se de que os dados estão corretos!"
        }
    }

    fun updateUserAccount(
        context: Context,
        newUserModel: UserModel,
        userViewModel: UserViewModel,
        userId: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (NetworkChangeReceiver().isNetworkConnected(context)) {
            userViewModel.updateUserModel(newUserModel) { res, updateMessage ->
                if (res) {
                    updateAccountUserRoom(
                        newUserModel,
                        userViewModel,
                        userId,
                        false,
                        callback
                    )
                } else {
                    Log.d(
                        "update account", "Falha ao atualizar a conta: $updateMessage"
                    )
                    callback(false, "Falha ao atualizar a conta: $updateMessage")
                }
            }
        } else {
//                pegae o id e salvar no room
            updateAccountUserRoom(
                newUserModel,
                userViewModel,
                userId,
                true,
                callback
            )
        }
    }

    fun updateAccountUserRoom(
        newUserModel: UserModel,
        userViewModel: UserViewModel,
        userId: String,
        needsSync: Boolean,
        callback: (Boolean, String) -> Unit
    ) {
        userViewModel.loadUserRoom(userId) { user ->
            if (user != null) {
                val newUser = newUserModel.toUserEntity(
                    user.password, user.salt
                )
                newUser.needsSync = needsSync
                userViewModel.updateUserRoom(newUser) { res, message ->
                    if (res) {
                        callback(true, "Conta atualizada com sucesso!")
                    } else {
                        callback(
                            false, "Problema ao atualizar os dados off da conta!"
                        )
                    }
                }
            } else {
                callback(
                    false, "Problema ao verificar os dados off da conta!"
                )
            }
        }
        Log.d("update account", "Conta atualizada com sucesso no firebase!")
    }

    fun deleteUserAccount(
        activity: AppCompatActivity,
        userViewModel: UserViewModel,
        userModel: UserModel,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        if (!NetworkChangeReceiver().isNetworkConnected(activity)) {
            callback(false, "Erro! falha na conexão!")
            return
        }

        val mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        if (currentUser != null) {
            // Reautenticar o usuário com as credenciais fornecidas
            val credential = EmailAuthProvider.getCredential(userModel.email, password)
            currentUser.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    userViewModel.deleteUserAccount(userModel.id) { result, message ->
                        if (!result) {
                            callback(false, message)
                            return@deleteUserAccount
                        }

                        // Após reautenticação bem-sucedida, deletar a conta
                        currentUser.delete().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                // Conta deletada com sucesso
                                val sharedPreferences = activity.getSharedPreferences(
                                    "login", Context.MODE_PRIVATE
                                )
                                sharedPreferences.edit().apply {
                                    putString("user_id", "")
                                    putBoolean("logged", false)
                                    apply()
                                }
                                callback(true, "Conta deletada com sucesso!")
                            } else {
                                // Falha ao deletar a conta
                                val error =
                                    deleteTask.exception?.message ?: "Falha ao deletar a conta"
                                Log.d("delete account", error)
                                callback(false, error)
                            }
                        }
                    }
                } else {
                    // Falha na reautenticação
                    val error = reauthTask.exception?.message ?: "Falha na reautenticação"
                    Log.d("delete account", error)
                    callback(false, error)
                }
            }
        } else {
            // Usuário não está logado
            callback(false, "Usuário não está logado.")
        }
    }
}

