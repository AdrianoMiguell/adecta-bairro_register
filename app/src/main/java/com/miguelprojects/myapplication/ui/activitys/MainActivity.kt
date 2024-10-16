package com.miguelprojects.myapplication.ui.activitys

import WorkspaceRepository
import android.content.Context
import android.content.SharedPreferences
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
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.util.WorkManagerUtil
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class MainActivity : AppCompatActivity() {
    //    Estou testando a situação em que o usuario conseguiu ser logado no firebase, mas não no banco.
    private lateinit var binding: ActivityMainBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var synchronizationTopnavItem: MenuItem
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var database: MyAppDatabase
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var isInitializeDrawer = false
    private var workspaceList = mutableListOf<WorkspaceModel>()
    private var userModel = UserModel()
    private var userId: String = ""

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

        startTools()

        getUserDataSharedPreferences()

        loadUserIds()

        if (!isInitializeDrawer) {
            initializeDrawer()
        }

        loadFragment(
            HomeFragment.newInstance(userId),
            false
        )
    }


    override fun onResume() {
        super.onResume()

        // Garantir que a NavigationView esteja configurada corretamente
        navigationView = findViewById(binding.topNavMenuView.id)
        navigationView.setCheckedItem(R.id.home_topnav)

//        // Mover a lógica de carregamento de dados e fragmentos para onResume
//        userViewModel.userModel.removeObservers(this)
        loadUserAndWorkspaces()
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
            binding.drawerLayout.id,
            binding.topNavMenuView.id,
            mapOf("userId" to userId),
        ).configureDrawerAndNavigation()
    }

    private fun loadUserAndWorkspaces() {

        println("loadUserAndWorkspaces")
        if (networkChangeReceiver.isNetworkConnected(this)) {
            userViewModel.userModel.observe(this, Observer { user ->
                userModel = user
                println("userViewModel.userModel.observe")
                WorkManagerUtil.scheduleWorkspaceSync(this, userId)
                userViewModel.userModel.removeObservers(this)
            })

            userViewModel.loadUserModel(userId)
        } else {
            userViewModel.loadUserRoom(userId) { user ->
                if (user != null) {
                    userModel = User.toUserModel(user)
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
