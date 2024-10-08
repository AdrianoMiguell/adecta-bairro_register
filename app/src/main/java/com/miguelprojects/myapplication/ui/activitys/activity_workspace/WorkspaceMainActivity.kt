package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityWorkspaceMainBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.ui.fragments.workspace.WorkspaceMainFragment
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.NetworkSynchronizeUser
import com.miguelprojects.myapplication.util.NetworkSynchronizeWorkspace
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.util.WorkManagerUtil
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class WorkspaceMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkspaceMainBinding
    private lateinit var sharedPreferences: SharedPreferences
//    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var database: MyAppDatabase
    private lateinit var networkSynchronizeWorkspace: NetworkSynchronizeWorkspace
    private lateinit var networkSynchronizeUser: NetworkSynchronizeUser
    private var userModel = UserModel()
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var workspaceId: String = ""
    private var userId: String = ""
    private var isReceiverRegistered = false

    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "DATA_SYNCHRONIZED" -> println("No data sync")
                "DATA_SYNCHRONIZED_USER" -> {
                    if(intent.getStringExtra("userId").isNullOrEmpty()){
                        Toast.makeText(this@WorkspaceMainActivity, "Sessão Encerrada! Por favor, realize login novamente!", Toast.LENGTH_SHORT).show()
                        UserSessionManager.onUserNotFoundOrLogout(this@WorkspaceMainActivity, userViewModel)
                    }
                }
                else -> println("Broadcast inesperado!")
            }
        }
    }

    private val userViewModel: UserViewModel by lazy {
        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)
        ViewModelProvider(this, userFactory)[UserViewModel::class.java]
    }

    private val workspaceViewModel: WorkspaceViewModel by lazy {
        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)
        ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkspaceMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        setupInsets()
        initializeDatabase()
        initializeSharedPreferences()

        getUserAndWorkspaceData()
        initializeApp()

        registerNetworkReceivers()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeDatabase() {
        database = (application as MyApplication).database
    }

    private fun initializeSharedPreferences() {
        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)
    }

    private fun registerNetworkReceivers() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

            networkSynchronizeWorkspace =
                NetworkSynchronizeWorkspace(workspaceViewModel, workspaceId)
            networkSynchronizeUser =
                NetworkSynchronizeUser(userViewModel, sharedPreferences, userId)

            registerReceiver(networkSynchronizeWorkspace, intentFilter)
            registerReceiver(networkSynchronizeUser, intentFilter)

            LocalBroadcastManager.getInstance(this).registerReceiver(
                uiUpdateReceiver,
                IntentFilter().apply {
                    addAction("DATA_SYNCHRONIZED")
                    addAction("DATA_SYNCHRONIZED_USER")
                    addAction("DATA_SYNCHRONIZED_WORKSPACE")
                }
            )
            isReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceivers()
    }

    private fun unregisterReceivers() {
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(networkSynchronizeWorkspace)
                unregisterReceiver(networkSynchronizeUser)
                LocalBroadcastManager.getInstance(this).unregisterReceiver(uiUpdateReceiver)
            } catch (e: Exception) {
                // Tratar exceções, se necessário
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    private fun initializeApp() {
        verifyStatusUserAndWorkspaceData()
        initializeFragment()
    }

    private fun initializeFragment() {
        if (!isFinishing && !isDestroyed) {
            loadFragment(WorkspaceMainFragment.newInstance(workspaceId, userId, userModel), false)
        }
    }

    private fun getUserAndWorkspaceData() {
        userId = intent.getStringExtra("userId") ?: ""
        workspaceId = intent.getStringExtra("workspaceId") ?: ""
    }

    private fun verifyStatusUserAndWorkspaceData() {
        if (userId.isEmpty() || workspaceId.isEmpty()) {
            showErrorAndFinish()
        } else {
            loadUserData()
        }
    }

    private fun loadUserData() {
        if (networkChangeReceiver.isNetworkConnected(this)) {
            userViewModel.loadUserModel(userId)
            userViewModel.userModel.observe(this, Observer { data ->
                userModel = data
                configureDrawerLayoutAndNavigationView()
            })
            WorkManagerUtil.scheduleCitizenSync(this, userId, workspaceId)
        } else {
            userViewModel.loadUserRoom(userId) { data ->
                if (data != null) {
                    userModel = User.toUserModel(data)
                    configureDrawerLayoutAndNavigationView()
                } else {
                    showErrorAndFinish()
                }
            }
        }
    }

    private fun showErrorAndFinish() {
        Toast.makeText(
            this,
            "Erro ao acessar o grupo de trabalho. Reporte esse problema!",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun configureDrawerLayoutAndNavigationView() {
        DrawerConfigurator(
            this,
            userModel,
            binding.drawerLayout.id,
            binding.topNavMenuView.id,
            mapOf("userId" to userId, "workspaceId" to workspaceId),
        ).configureDrawerAndNavigation()
    }

    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
        Handler(Looper.getMainLooper()).post {
            if (!isFinishing && !isDestroyed) {
                val transition = supportFragmentManager.beginTransaction()
                transition.replace(R.id.fragment_container_citizens, fragment)
                if (addToBack) {
                    transition.addToBackStack(null)
                }
                transition.commit()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (drawerToggle.onOptionsItemSelected(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}