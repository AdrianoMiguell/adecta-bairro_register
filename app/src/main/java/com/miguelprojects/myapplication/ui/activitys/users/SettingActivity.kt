package com.miguelprojects.myapplication.ui.activitys.users

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.navigation.NavigationView
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivitySettingBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.NetworkSynchronizeUser
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var userModel: UserModel
    private lateinit var database: MyAppDatabase
    private lateinit var navigationView: NavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var networkSynchronizeUser: NetworkSynchronizeUser
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var userSessionManager = UserSessionManager
    private var isReceiverRegistered = false
    private var userId: String = ""
    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "DATA_SYNCHRONIZED_USER" -> {
                    if(intent.getStringExtra("userId").isNullOrEmpty()){
                        Toast.makeText(this@SettingActivity, "Sessão Encerrada! Por favor, realize login novamente!", Toast.LENGTH_SHORT).show()
                        UserSessionManager.onUserNotFoundOrLogout(this@SettingActivity, userViewModel)
                    }
                }
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        viewLayoutSetting(true)
        viewProgressBar(true)

        startTools()

        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

            networkSynchronizeUser = NetworkSynchronizeUser(userViewModel, sharedPreferences, userId)
            registerReceiver(networkSynchronizeUser, intentFilter)

            LocalBroadcastManager.getInstance(this).registerReceiver(
                uiUpdateReceiver,
                IntentFilter().apply {
                    addAction("DATA_SYNCHRONIZED")
                    addAction("DATA_SYNCHRONIZED_USER")
                }
            )
        }
    }

    private fun viewLayoutSetting(status: Boolean) {
        binding.layoutSettings.visibility = if (status) View.VISIBLE else View.GONE
        binding.layoutConfirmDeleteAccount.visibility = if (status) View.GONE else View.VISIBLE
    }

    private fun viewProgressBar(status: Boolean) {
        binding.layoutProgressBar.visibility = if (status) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()

        loadUserData()
        navigationView = findViewById(binding.topNavMenuView.id)
        navigationView.setCheckedItem(R.id.home_topnav)
    }

    override fun onDestroy() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(networkSynchronizeUser)
            } catch (e: IllegalArgumentException) {
                // O receptor não estava registrado, não faça nada
            } finally {
                isReceiverRegistered = false
            }
        }
        super.onDestroy()
    }

    private fun loadUserData() {
        verifyExtrasId()

        if (networkChangeReceiver.isNetworkConnected(this)) {
            userViewModel.userModel.observe(this, Observer { data ->
                if (data != null) {
                    userModel = data
                    updateUserData()
                    DrawerConfigurator(
                        this,
                        userModel,
                        binding.drawerLayout.id,
                        binding.topNavMenuView.id,
                        mapOf("userId" to userId),
                    ).configureDrawerAndNavigation()

                    viewProgressBar(false)
                    setClickListeners()
                } else {
                    Log.d("loadUserData", "Id do workspace ou user está nulo!")
                    showErrorAndFinish()
                }
            })
            userViewModel.loadUserModel(userId)
        } else {
            userViewModel.loadUserRoom(userId) { data ->
                if (data != null) {
                    userModel = User.toUserModel(data)
                    updateUserData()
                    DrawerConfigurator(
                        this,
                        userModel,
                        binding.drawerLayout.id,
                        binding.topNavMenuView.id,
                        mapOf("userId" to userId),
                    ).configureDrawerAndNavigation()

                    viewProgressBar(false)
                    setClickListeners()
                } else {
                    Log.d("loadUserData", "Id do workspace ou user está nulo!")
                    showErrorAndFinish()
                }
            }
        }
    }

    private fun verifyExtrasId() {
        if (intent.hasExtra("userId") && intent.hasExtra("offUserId")) {
            userId = intent.getStringExtra("userId") ?: ""

            if (userId.isEmpty()) {
                getUserDataSharedPreferences()
            }
        } else {
            getUserDataSharedPreferences()
        }

        if (userId.isEmpty()) {
            showErrorAndFinish();
        }
    }

    private fun getUserDataSharedPreferences() {
        userId = sharedPreferences.getString("user_id", null).toString()
    }

    private fun showErrorAndFinish() {
        Toast.makeText(
            this,
            "Erro ao acessar as configurações. Reporte esse problema!",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun startTools() {
        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)

        // Obtenha a instância do banco de dados a partir da aplicação
        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)

        userViewModel = ViewModelProvider(this, userFactory)[UserViewModel::class.java]
    }

    private fun updateUserData() {
        binding.editUsername.setText(userModel.username)
        binding.editFullname.setText(userModel.fullname)
        binding.textEmail.setText(userModel.email)
    }

    private fun setClickListeners() {
        binding.buttonLogout.setOnClickListener {
            // Exibir um AlertDialog para confirmar a ação de logout
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Logout")
            builder.setMessage("Você tem certeza que deseja sair? Dados importantes que não foram sincronizados serão perdidos.")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                binding.layoutSettings.animate()
                    .alpha(0.0f)
                    .setDuration(250)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            // Ação de logout
                            userSessionManager.onUserNotFoundOrLogout(
                                this@SettingActivity,
                                userViewModel
                            )
                            // Mostrar um Toast confirmando a ação
                            Toast.makeText(
                                this@SettingActivity,
                                "Você saiu de sua conta!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    })

            }
            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }

            // Mostrar o AlertDialog
            builder.show()
        }

        binding.buttonDeleteAccount.setOnClickListener {
            if (networkChangeReceiver.isNetworkConnected(this)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Deletar conta")
                builder.setMessage("Você tem certeza que deseja deletar a conta atual? Todos os dados relacionados a essa conta serão excluidos.")

                // Adicionar botões ao AlertDialog
                builder.setPositiveButton("Sim") { dialog, which ->
                    // Ação de logout
                    viewLayoutSetting(false)
                    // Mostrar um Toast confirmando a ação
                }
                builder.setNegativeButton("Não") { dialog, which ->
                    // Fechar o dialog sem fazer nada
                    dialog.dismiss()
                }

                // Mostrar o AlertDialog
                builder.show()
            } else {
                Toast.makeText(
                    this,
                    "É necessário acesso á rede para concluir essa ação!",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.buttonConfirm.setOnClickListener {
            binding.buttonConfirm.isEnabled = false
            val password = binding.editConfirm.text.toString()

            if (password.isNotEmpty() && password.length >= 6) {
                viewProgressBar(true)

                UserSessionManager.confirmUserPassword(
                    this,
                    userViewModel,
                    userModel.email,
                    password
                ) { result, message, _ ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    if (!result) {
                        viewProgressBar(false)
                        binding.buttonConfirm.isEnabled = true
                        return@confirmUserPassword
                    }

                    UserSessionManager.deleteUserAccount(
                        this,
                        userViewModel,
                        userModel,
                        password
                    ) { res, message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                        if (!res) {
                            viewProgressBar(false)
                            binding.buttonConfirm.isEnabled = true
                            return@deleteUserAccount
                        }

                        animateScreenExit(message, binding.layoutSettings)
                    }

                }
            }
        }

        binding.buttonUpdateUser.setOnClickListener {
            val newUserModel = UserModel(
                userId ?: userModel.id,
                binding.editUsername.text.toString(),
                binding.editFullname.text.toString(),
                userModel.email,
            )

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Atualizar dados da conta")
            builder.setMessage("Você tem certeza que deseja atualizar a conta atual?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                updateUserAccount(newUserModel)
            }
            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }

            // Mostrar o AlertDialog
            builder.show()
        }

        binding.buttonBack.setOnClickListener {
            viewLayoutSetting(binding.layoutSettings.visibility != View.VISIBLE)
        }
    }

    private fun updateUserAccount(newUserModel: UserModel) {
        userSessionManager.updateUserAccount(
            this@SettingActivity,
            newUserModel,
            userViewModel,
            userId,
        ) { res, message ->
            if (res) {
                println("Processo concluido com sucesso!")
                animateScreenExit(message, binding.layoutSettings)
            } else {
                println("Erro no processo!")
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateScreenExit(message: String, view: View) {
        view.animate()
            .alpha(0.0f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

                    Toast.makeText(this@SettingActivity, message, Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(this@SettingActivity, MainActivity::class.java)
                    startActivity(intent)
                }
            })
    }

    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
        // Carregar o fragmento fornecido no container
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        if (addToBack) {
            transaction.addToBackStack(null) // Adicionar a transação ao back stack (opcional)
        }
        transaction.commit()
        // Ocultar o ProgressBar após a transação de fragmento
    }

//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        // Inflate the layout for this fragment
//        binding = FragmentSettingsBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        startTools()
//
//        if (networkChangeReceiver.isNetworkConnected(requireContext())) {
//            if (userId.isNullOrEmpty() || offUserId == 0L) {
//                Log.d(
//                    "User id, settings",
//                    "Error. Dados do usuário não carregados do banco!"
//                )
//                requireActivity().supportFragmentManager.popBackStack()
//            }
//
//            println(userId)
//            userViewModel.loadUserModel(userId!!)
//            userViewModel.userModel.observe(viewLifecycleOwner, Observer { user ->
//                println(user)
//                if (user != null) {
//                    userModel = user
//                    println(userModel)
//                    updateUserData()
//                } else {
//                    Log.d(
//                        "User id, settings",
//                        "Error. Dados do usuário não carregados do banco!"
//                    )
//                    requireActivity().supportFragmentManager.popBackStack()
//                }
//            })
//        } else {
//            userViewModel.loadUserRoom(offUserId!!) { user ->
//                if (user != null) {
//                    userModel = User.toUserModel(user)
//                    println(userModel)
//                    updateUserData()
//                } else {
//                    Log.d(
//                        "User id, settings",
//                        "Error. Dados do usuário não carregados do banco!"
//                    )
//                    requireActivity().supportFragmentManager.popBackStack()
//                }
//            }
//        }
//
//        setClickListeners()
//    }
//
//    private fun startTools() {
//        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
//    }
//
//    private fun updateUserData() {
//        binding.editUsername.setText(userModel.username)
//        binding.editFullname.setText(userModel.fullname)
//        binding.textEmail.setText(userModel.email)
//    }
//
//    private fun setClickListeners() {
//        binding.buttonLogout.setOnClickListener {
//            // Exibir um AlertDialog para confirmar a ação de logout
//            val builder = AlertDialog.Builder(requireContext())
//            builder.setTitle("Logout")
//            builder.setMessage("Você tem certeza que deseja sair?")
//
//            // Adicionar botões ao AlertDialog
//            builder.setPositiveButton("Sim") { dialog, which ->
//                binding.layoutSettings.animate()
//                    .alpha(0.0f)
//                    .setDuration(250)
//                    .setListener(object : AnimatorListenerAdapter() {
//                        override fun onAnimationEnd(animation: Animator) {
//                            super.onAnimationEnd(animation)
//                            // Ação de logout
//                            userSessionManager.onUserNotFoundOrLogout(
//                                requireActivity() as AppCompatActivity,
//                                userViewModel
//                            )
//                            // Mostrar um Toast confirmando a ação
//                            Toast.makeText(
//                                requireContext(),
//                                "Você saiu de sua conta!",
//                                Toast.LENGTH_SHORT
//                            )
//                                .show()
//                        }
//                    })
//
//            }
//            builder.setNegativeButton("Não") { dialog, which ->
//                // Fechar o dialog sem fazer nada
//                dialog.dismiss()
//            }
//
//            // Mostrar o AlertDialog
//            builder.show()
//        }
//
//        binding.buttonDeleteAccount.setOnClickListener {
//            val builder = AlertDialog.Builder(requireContext())
//            builder.setTitle("Deletar conta")
//            builder.setMessage("Você tem certeza que deseja deletar a conta atual?")
//
//            // Adicionar botões ao AlertDialog
//            builder.setPositiveButton("Sim") { dialog, which ->
//                // Ação de logout
//                loadFragment(
//                    UpdateAndDeleteUserFragment.newInstance(
//                        userId!!,
//                        offUserId!!,
//                        userModel,
//                        UserModel(),
//                        "delete"
//                    ), true
//                )
//                // Mostrar um Toast confirmando a ação
//                Toast.makeText(
//                    requireContext(),
//                    "Confirme a realização dessa ação!",
//                    Toast.LENGTH_SHORT
//                )
//                    .show()
//            }
//            builder.setNegativeButton("Não") { dialog, which ->
//                // Fechar o dialog sem fazer nada
//                dialog.dismiss()
//            }
//
//            // Mostrar o AlertDialog
//            builder.show()
//        }
//
//        binding.buttonUpdateUser.setOnClickListener {
//            val newUserModel = UserModel(
//                userId ?: userModel.id,
//                binding.editUsername.text.toString(),
//                binding.editFullname.text.toString(),
//                userModel.email,
//                offUserId ?: userModel.offUserId
//            )
//
//            val builder = AlertDialog.Builder(requireContext())
//            builder.setTitle("Atualizar dados da conta")
//            builder.setMessage("Você tem certeza que deseja atualizar a conta atual?")
//
//            // Adicionar botões ao AlertDialog
//            builder.setPositiveButton("Sim") { dialog, which ->
//                updateUserAccount(newUserModel)
//            }
//            builder.setNegativeButton("Não") { dialog, which ->
//                // Fechar o dialog sem fazer nada
//                dialog.dismiss()
//            }
//
//            // Mostrar o AlertDialog
//            builder.show()
//        }
//    }
//
//    fun updateUserAccount(newUserModel: UserModel) {
//        userSessionManager.updateUserAccount(
//            requireActivity() as AppCompatActivity,
//            newUserModel!!,
//            userViewModel,
//            offUserId!!,
//            userId!!,
//        ) { res, message ->
//            if (res) {
//                println("Processo concluido com sucesso!")
//                binding.layoutSettings.animate()
//                    .alpha(0.0f)
//                    .setDuration(250)
//                    .setListener(object : AnimatorListenerAdapter() {
//                        override fun onAnimationEnd(animation: Animator) {
//                            super.onAnimationEnd(animation)
//
//                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//                            val intent = requireActivity().intent
//                            startActivity(intent)
//                        }
//                    })
//            } else {
//                println("Erro no processo!")
//                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
//        // Carregar o fragmento fornecido no container
//        val transaction = requireActivity().supportFragmentManager.beginTransaction()
//        transaction.replace(R.id.fragment_container, fragment)
//        if (addToBack) {
//            transaction.addToBackStack(null) // Adicionar a transação ao back stack (opcional)
//        }
//        transaction.commit()
//        // Ocultar o ProgressBar após a transação de fragmento
//    }

}