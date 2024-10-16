package com.miguelprojects.myapplication.ui.activitys.users

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityRegisterBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userModel: UserModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var upPassword = ""
    private var upSalt = ""
    private var avatarImageResult = 0
    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val imageResult = result.data?.getIntExtra("imageResult", 0) ?: 0
            avatarImageResult = imageResult
            println("Result = $imageResult")

            if (result.resultCode == IMAGE_CODE) {
                binding.imageProfileAvatar.setImageResource(
                    UserSessionManager.changeImageProfileAvatar(imageResult, false)
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        startTools()

        setEvents()
    }

    private fun startTools() {
        mAuth = FirebaseAuth.getInstance()
        // Obtenha a instância do banco de dados a partir da aplicação
        val database = (application as MyApplication).database
        val userDao = database.userDao()
        val repository = UserRepository(userDao)
        val factory = UserViewModelFactory(repository)

        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]
    }

    private fun setEvents() {
        binding.imageProfileAvatar.setOnClickListener {
            val intent = Intent(this, ChooseImageProfileActivity::class.java)
            result.launch(intent)
        }

        binding.buttonRegister.setOnClickListener {
            val name = binding.editName.text.toString()
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()
            val confirmPassword = binding.editConfirmPassword.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                val username = name.split(" ")[0]

                userModel = UserModel("", username, name, email, avatarImageResult)

                if (password != confirmPassword) {
                    Toast.makeText(
                        this,
                        getString(R.string.as_senhas_informadas_nao_sao_iguais), Toast.LENGTH_SHORT
                    )
                        .show()
                    return@setOnClickListener
                } else {
                    binding.buttonRegister.isEnabled = false
                    binding.includeProgressBar.layoutProgressBar.visibility =
                        View.VISIBLE

                    UserSessionManager.verifyExistsEmailInBases(
                        this,
                        userViewModel,
                        email
                    ) { existsEmail ->
                        println("$email, $existsEmail")
                        if (existsEmail) {
                            messageToast("Usuário já existe!")
                            binding.buttonRegister.isEnabled = true
                            binding.includeProgressBar.layoutProgressBar.visibility =
                                View.GONE
                            return@verifyExistsEmailInBases
                        } else {
                            println("Dados do usuario não encontrados")
                            if (networkChangeReceiver.isNetworkConnected(this)) {
                                saveUserFirebase(password)
                            } else {
                                saveUserLocal(userModel, password)
                            }
                        }
                    }
                }
            } else {
                messageToast("Por favor, preencha os campos vazios!")
            }
        }

        binding.buttonLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.textViewPassword.setOnClickListener {
            changeChecked(binding.buttonViewPassword)
        }

        binding.buttonViewPassword.setOnCheckedChangeListener { _, isChecked ->
            // Mostrar a senha
            val passwordCursorPosition = binding.editPassword.selectionStart
            val passwordConfirmCursorPosition = binding.editConfirmPassword.selectionStart

            if (isChecked) {
                binding.editPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
                binding.editConfirmPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.editPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
                binding.editConfirmPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            }

            binding.editPassword.setSelection(passwordCursorPosition)
            binding.editConfirmPassword.setSelection(passwordConfirmCursorPosition)
        }
    }

    private fun saveUserFirebase(password: String) {
        UserSessionManager.registerUserAccountFirebase(
            this,
            userViewModel,
            userModel,
            password,
        ) { res, message, userId ->
            if (res && userId.isNotEmpty()) {
                userModel.id = userId
                saveUserLocal(userModel, password)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                binding.buttonRegister.isEnabled = true
                println(message)
                messageToast(message)
                binding.includeProgressBar.layoutProgressBar.visibility =
                    View.GONE
            }
        }
    }


    private fun saveUserLocal(userModel: UserModel, password: String) {
        try {
            val needsSync = !networkChangeReceiver.isNetworkConnected(this)

            userViewModel.saveUserRoom(userModel, password, needsSync) { userId ->
                if (userId.isNotEmpty()) {
                    openLoginActivity()
                }
            }
        } catch (e: Exception) {
            binding.buttonRegister.isEnabled = true
            println(e.message)
            messageToast("Erro ao salvar os dados do usuário. Reporte esse problema!")
            binding.includeProgressBar.layoutProgressBar.visibility = View.GONE
        }
    }

    private fun messageToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun openLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun changeChecked(element: CheckBox) {
        element.isChecked = !element.isChecked
    }

    private companion object {
        private const val IMAGE_CODE = 99
    }
}