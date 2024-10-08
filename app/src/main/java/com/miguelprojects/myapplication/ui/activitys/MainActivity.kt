package com.miguelprojects.myapplication.ui.activitys

import WorkspaceRepository
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityMainBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.ui.fragments.HomeEmptyFragment
import com.miguelprojects.myapplication.ui.fragments.HomeFragment
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.NetworkSynchronizeUser
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.util.WorkManagerUtil
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class MainActivity : AppCompatActivity() {

    //    TODO("Colocar criptografia nos dados salvos offline")
//    TODO("Fazer a página de suporte")
//    TODO("Colocar novo campo "email do citizen" e lógica para enviar mensagem para o email ou um sms para o numero com as diretrizes de segurança")
//    TODO("Finalizar a atualização online dos dados do usuário")

//    TODO("Fazer activity de Suporte")

    //    Estou testando a situação em que o usuario conseguiu ser logado no firebase, mas não no banco.
    private lateinit var binding: ActivityMainBinding
    private lateinit var networkSynchronizeUser: NetworkSynchronizeUser
    private lateinit var userViewModel: UserViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var synchronizationTopnavItem: MenuItem
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: MyAppDatabase
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private var isInitializeDrawer = false
    private var workspaceList = mutableListOf<WorkspaceModel>()
    private var userModel = UserModel()
    private var userId: String = ""
    private var isReceiverRegistered = false
    private var dataSynchronized = false
    private var initWorkspaceAccessObserver = false
    private var initWorkspaceModelObserver = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Código específico para Android 10 (API 29) ou mais recente
            println("Entrou aqui no Main | Código específico para Android 10 (API 29) ou mais recente")
        } else {
            println("Entrou aqui no Main | Alternativa para versões anteriores do Android")
            // Alternativa para versões anteriores do Android
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)
//        progressBarLayoutManager(false)

        startTools()

        getUserDataSharedPreferences()

        loadUserIds()

        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

            networkChangeReceiver = NetworkChangeReceiver()
            networkSynchronizeUser =
                NetworkSynchronizeUser(userViewModel, sharedPreferences, userId)

            registerReceiver(networkSynchronizeUser, intentFilter)

            isReceiverRegistered = true
        }

        loadFragment(
            HomeFragment.newInstance(userId),
            false
        )

//        loadUserAndWorkspaces()
    }

    override fun onDestroy() {
        super.onDestroy()
//        workspaceViewModel.workspaceListModel.removeObservers(this)
//        workspaceViewModel.workspaceListRoom.removeObservers(this)

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(networkSynchronizeUser)
            } catch (e: IllegalArgumentException) {
                // O receptor não estava registrado, não faça nada
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Garantir que a NavigationView esteja configurada corretamente
        navigationView = findViewById(binding.topNavMenuView.id)
        navigationView.setCheckedItem(R.id.home_topnav)

//        // Mover a lógica de carregamento de dados e fragmentos para onResume
//        userViewModel.userModel.removeObservers(this)
        loadUserAndWorkspaces()

        // Inicializar o Drawer se ainda não foi feito
        if (!isInitializeDrawer) {
            initializeDrawer()
        }
    }

    private fun startTools() {
        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)

        // Obtenha a instância do banco de dados a partir da aplicação
        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)

        userViewModel = ViewModelProvider(this, userFactory)[UserViewModel::class.java]

        // Obtenha a instância do banco de dados a partir da aplicação
        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)

        workspaceViewModel =
            ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]
    }

    private fun getUserDataSharedPreferences() {
        userId = sharedPreferences.getString("user_id", null).toString()
    }

    private fun loadUserIds() {
        println("userId = $userId")

        if (NetworkChangeReceiver().isNetworkConnected(this)) {
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                // Usuário não está autenticado
                needsAuthAgain()
                UserSessionManager.onUserNotFoundOrLogout(this, userViewModel)
                return
            }

            userViewModel.userModel.observe(this, Observer { user ->
                println("observer user")
                if (user == null || user.id.isEmpty()) {
                    needsAuthAgain()
                    UserSessionManager.onUserNotFoundOrLogout(this, userViewModel)
                    userViewModel.userModel.removeObservers(this)
                } else {
                    println("verifyUserDataRoom")
                    verifyUserDataRoom(true)
                }
            })

            userViewModel.loadUserModel(userId)
        } else {
            verifyUserDataRoom(false)
        }
    }

    private fun verifyUserDataRoom(statusConnect: Boolean) {
        userViewModel.loadUserRoom(userId) { offUser ->
            println("userId - $userId")

            if (offUser == null) {
                println("Load user room com problemas")
                UserSessionManager.onUserNotFoundOrLogout(this, userViewModel)
            } else {
                if (statusConnect) {
                    getAllNotifications()
                }

                if (offUser.needsSync) {
                    userViewModel.updateUserModel(User.toUserModel(offUser)) { _, message ->
                        print(message)
                    }
                }
            }
        }
    }

    private fun getAllNotifications() {
        workspaceViewModel.getSyncNotifications(userId) { success, list ->
            if (success) {
                val firstNotify = list.first()

                if (firstNotify.notification.isNotEmpty()) {
                    Toast.makeText(this, firstNotify.notification, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun needsAuthAgain() {
        Toast.makeText(
            this,
            "Sessão Encerrada! Por favor, realize login novamente!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun initializeDrawer() {
        DrawerConfigurator(
            this,
            userModel,
            binding.drawerLayout.id,
            binding.topNavMenuView.id,
            mapOf("userId" to userId!!),
        ).configureDrawerAndNavigation()
    }

    private fun loadUserAndWorkspaces() {
//        progressBarLayoutManager(false)

        println("loadUserAndWorkspaces")
        if (networkChangeReceiver.isNetworkConnected(this)) {
            userViewModel.userModel.observe(this, Observer { user ->
                userModel = user
                println("userViewModel.userModel.observe")
//                loadWorkspace()
                if (!isInitializeDrawer) {
                    initializeDrawer()
                }
                WorkManagerUtil.scheduleWorkspaceSync(this, userId)
                userViewModel.userModel.removeObservers(this)
            })

            userViewModel.loadUserModel(userId)
        } else {
            userViewModel.loadUserRoom(userId) { user ->
                if (user != null) {
                    userModel = User.toUserModel(user)
//                    loadWorkspace()
                    if (!isInitializeDrawer) {
                        initializeDrawer()
                    }
                } else {
                    Log.d(
                        "UserSessionManager",
                        "Dados de usuário não encontrados no Banco de dados sqlite"
                    )
                    Toast.makeText(
                        this,
                        "Falha ao carregar os Dados do usuário. Reporte esse erro!",
                        Toast.LENGTH_SHORT
                    ).show()
                    UserSessionManager.onUserNotFoundOrLogout(this, userViewModel)
                }
            }
        }
    }

    private fun updateUserData() {
        usernameTextView =
            binding.topNavMenuView.getHeaderView(0).findViewById(R.id.username_navbar)
        emailTextView = binding.topNavMenuView.getHeaderView(0).findViewById(R.id.email_navbar)

        usernameTextView.text = userModel.username
        emailTextView.text = userModel.email
    }
//
//    private fun progressBarLayoutManager(status: Boolean) {
//        binding.fragmentContainer.visibility = if (status) View.VISIBLE else View.GONE
//        binding.progressBar.visibility = if (status) View.GONE else View.VISIBLE
//    }

    private fun loadFragmentHomeEmpty() {
        loadFragment(HomeEmptyFragment.newInstance(userId), false)
    }

    private fun loadFragment(fragment: Fragment, addOnBack: Boolean?) {
        if (!isFinishing && !isDestroyed) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            if (addOnBack!!) {
                transaction.addToBackStack(null)
            }
            transaction.commit()
        }
    }
}
