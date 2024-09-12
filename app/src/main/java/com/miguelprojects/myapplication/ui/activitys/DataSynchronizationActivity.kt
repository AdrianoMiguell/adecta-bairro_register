package com.miguelprojects.myapplication.ui.activitys

import WorkspaceRepository
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityDataSynchronizationBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class DataSynchronizationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDataSynchronizationBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var database: MyAppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDataSynchronizationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        database = (application as MyApplication).database

        startTools()

        binding.layoutNavbarTop.buttonOpenMenu.setImageResource(R.drawable.baseline_arrow_back_24)
        binding.layoutNavbarTop.buttonOpenMenu.setOnClickListener {
            finish()
        }
        setOnClickListeners()
    }


    private fun startTools() {
        // Obtenha a instância do banco de dados a partir da aplicação
        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)

        userViewModel = ViewModelProvider(this, userFactory)[UserViewModel::class.java]

        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)

        workspaceViewModel =
            ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]

//        val citizenDao = database.citizenDao()
//        val citizenRepository = CitizenRepository(citizenDao)
//        val citizenFactory = CitizenViewModelFactory(citizenRepository, )
//
//        citizenViewModel =
//            ViewModelProvider(this, citizenFactory)[CitizenViewModel::class.java]
    }

    private fun setOnClickListeners() {
        binding.buttonSynchronization.setOnClickListener {
//            sincronizar dados
        }

//        binding.layoutNavbarTop.buttonOpenMenu.setOnClickListener {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//        }

    }

}