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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityLoginBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userViewModel: UserViewModel
    private lateinit var sharedPreferences: SharedPreferences
    private val networkChangeReceiver = NetworkChangeReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        // Configuração da barra de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startTools()

        setEvents()
    }

//    verificar se tem internet ou não
//    se off ->
//    verificar se dados do login correspondem
//    se online ->
//    verificar se dados no firebase estão salvos
//      se sim ->
//         verificar se dados estão salvos no off
//            se salvos no off ->
//              continuar e seguir
//            se não ->
//               salvar os dados no off e seguir
//      se não ->
//        verificar se os dados de login estão salvos off
//          se estiver ->
//              pega-los e salva-los no online
//          se não estiver ->
//              dar erro, pois não encontrou os dados


    private fun startTools() {
        UserSessionManager.checkIfLogged(this)

        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)

        mAuth = FirebaseAuth.getInstance()
        // Obtenha a instância do banco de dados a partir da aplicação
        val database = (application as MyApplication).database
        val userDao = database.userDao()
        val repository = UserRepository(userDao)
        val factory = UserViewModelFactory(repository)

        userViewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]
    }

    private fun setEvents() {
        // Configuração do clique no botão de login
        binding.buttonLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()
            val checkboxSaveLogin = binding.buttonSaveLogin.isChecked

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (!isValidEmail(email)) {
                    messageToast("Email inválido.")
                    return@setOnClickListener
                }

                if (!isValidPassword(password)) {
                    messageToast("A senha deve ter pelo menos 6 caracteres.")
                    return@setOnClickListener
                }

                binding.buttonSaveLogin.isEnabled = false
                binding.includeProgressBar.layoutProgressBar.visibility = View.VISIBLE

                println("Email: ${email} Senha: ${password} ")

                if (!networkChangeReceiver.isNetworkConnected(this)) {
//                    criar função de login off
                    UserSessionManager.loginUserToRoom(
                        this,
                        userViewModel,
                        password,
                        UserModel(email = email),
                        sharedPreferences,
                        checkboxSaveLogin
                    ) { res, mes ->
                        messageToast(mes)
                        if (res) {
                            openLoginActivity()
                        } else {
                            binding.buttonSaveLogin.isEnabled = true
                            binding.includeProgressBar.layoutProgressBar.visibility = View.GONE
                        }
                    }
                } else {
                    UserSessionManager.loginUserAccount(
                        this,
                        email,
                        password,
                        userViewModel,
                        checkboxSaveLogin
                    ) { res, message ->
                        messageToast(message)

                        if (res) {
                            openLoginActivity()
                        } else {
                            binding.buttonSaveLogin.isEnabled = true
                            binding.includeProgressBar.layoutProgressBar.visibility = View.GONE
                        }
                    }
                }
            } else {
                messageToast("Por favor, preencha os campos vazios!")
            }

        }

        binding.textSaveLogin.setOnClickListener {
            changeChecked(binding.buttonSaveLogin)
        }

        binding.textViewPassword.setOnClickListener {
            changeChecked(binding.buttonViewPassword)
        }

        binding.buttonViewPassword.setOnCheckedChangeListener { _, isChecked ->
            // Mostrar a senha
            val passwordCursorPosition = binding.editPassword.selectionStart

            if (isChecked) {
                binding.editPassword.transformationMethod =
                    HideReturnsTransformationMethod.getInstance()
            } else {
                binding.editPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            }

            binding.editPassword.setSelection(passwordCursorPosition)
        }

        // Configuração dos cliques nos outros botões
        binding.buttonRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.buttonForgotPassword.setOnClickListener {
            if (networkChangeReceiver.isNetworkConnected(this)) {
                val intent = Intent(this, ResetPasswordActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Essa ação precisa de acesso a rede.", Toast.LENGTH_SHORT)
                    .show()
            }
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
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6 // Firebase requer uma senha com pelo menos 6 caracteres
    }

    private fun changeChecked(element: CheckBox) {
        element.isChecked = !element.isChecked
    }
}
