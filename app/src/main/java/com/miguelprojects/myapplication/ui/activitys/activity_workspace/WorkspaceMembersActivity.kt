package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.UserListAdapter
import com.miguelprojects.myapplication.adapter.listener.UserOnClickListener
import com.miguelprojects.myapplication.databinding.ActivityWorkspaceMembersBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceRequestViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class WorkspaceMembersActivity : AppCompatActivity() {
    private lateinit var adapter: UserListAdapter
    private lateinit var binding: ActivityWorkspaceMembersBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var workspaceRequestViewModel: WorkspaceRequestViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var userList: List<UserModel>
    private lateinit var database: MyAppDatabase
    private var listMembers = emptyList<UserModel>()
    private var listUserId = mutableListOf<String>()
    private var listSelectedUsers = mutableListOf<String>()
    private var workspaceModel = WorkspaceModel()
    private var workspaceId: String = ""
    private var userId: String = ""
    private var workspaceCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWorkspaceMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        getExtraValues()

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)
        DrawerConfigurator(
            this,
            UserModel(),
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        startTools()

        getWorkspaceData()

        setOnClickListeners()
    }

    fun startTools() {
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

        workspaceRequestViewModel = ViewModelProvider(this)[WorkspaceRequestViewModel::class.java]
    }

    private fun getExtraValues() {
        workspaceId = intent.getStringExtra("workspaceId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""
        workspaceCode = intent.getStringExtra("workspaceCode") ?: ""

        verifyIfEmptyValues()
    }

    private fun verifyIfEmptyValues() {
        verifyIfConnection()

        if (userId.isEmpty() || workspaceId.isEmpty()) {
            Toast.makeText(
                this,
                "Erro ao acessar a página. Por favor, Reporte esse problema!",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("verifyIfEmptyValues", "Erro, dados necessários estão vazios")
            finish()
        }
    }

    private fun verifyIfConnection() {
        if (!NetworkChangeReceiver().isNetworkConnected(this)) {
            Toast.makeText(
                this,
                "Falha na conexão com a rede!",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("verifyIfEmptyValues", "Erro, falha na conexão")
            finish()
        }
    }

    private fun getWorkspaceData() {
        workspaceViewModel.workspaceModel.observe(this, Observer {
            workspaceModel = it
            checkButtonVisible(it.creator == userId)

            val mapUserWorkspace = it.userIds
            mapUserWorkspace.map { users ->
                if (users.value) {
                    listUserId.add(users.key)
                }
            }

            if (it.creator == userId) {
                binding.buttonRemoveUsers.visibility = View.VISIBLE
            } else {
                binding.buttonRemoveUsers.visibility = View.GONE
            }

            loadUsersWorkspace()
        })
        workspaceViewModel.loadData(workspaceId)
    }

    private fun reorderUsersList(
        usersList: List<UserModel>,
        creatorWorkspaceId: String?
    ): List<UserModel> {
        val (creator, others) = usersList.partition { it.id == creatorWorkspaceId }
        return creator + others
    }

    private fun loadUsersWorkspace() {
        userViewModel.loadListUserModel(listUserId) { listUserModel ->
            listMembers = reorderUsersList(listUserModel , workspaceModel.creator)

            if (listMembers.isNotEmpty()) {
                configureRecycleViewAdapter()
            }
        }
    }

    private fun configureRecycleViewAdapter() {
        binding.recycleviewNotificationIncomingOrders.layoutManager = LinearLayoutManager(this)
        adapter = UserListAdapter(
            listMembers,
            (workspaceModel.creator == userId),
            workspaceModel.creator,
            UserOnClickListener { user ->
                if (listSelectedUsers.contains(user.id)) {
                    listSelectedUsers.remove(user.id)
                } else {
                    listSelectedUsers.add(user.id)
                }
                checkButtonActivation()
            })
        binding.recycleviewNotificationIncomingOrders.adapter = adapter
    }

    private fun setOnClickListeners() {
        removeWorkspaceMembers()
    }

    private fun removeWorkspaceMembers() {
        binding.buttonRemoveUsers.setOnClickListener {
            verifyIfConnection()

            val builder = AlertDialog.Builder(this)

            builder.setTitle("Remover Membros")

            builder.setMessage("Deseja mesmo remover os membros selecionados deste grupo de trabalho?")

            builder.setPositiveButton("Sim") { _, _ ->
                workspaceViewModel.removeWorkspaceMembers(workspaceId, listSelectedUsers)

                listMembers = listMembers.filterNot { it.id in listSelectedUsers }

                if (::adapter.isInitialized) {
                    adapter.submitList(listMembers)
                } else {
                    configureRecycleViewAdapter()
                }
            }

            builder.setNegativeButton("Não") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun checkButtonVisible(isVisible: Boolean) {
        binding.buttonRemoveUsers.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    private fun checkButtonActivation() {
        if (listSelectedUsers.size > 0) {
            changeStyleButton(
                binding.buttonRemoveUsers,
                R.color.red,
                R.color.light_gray,
                true
            )
        } else {
            changeStyleButton(
                binding.buttonRemoveUsers,
                R.color.red,
                R.color.light_gray,
                false
            )
        }
    }

    private fun changeStyleButton(
        button: Button,
        colorEnabled: Int,
        colorDisabled: Int,
        isEnable: Boolean,
    ) {
        button.backgroundTintList =
            ContextCompat.getColorStateList(
                this,
                if (isEnable) colorEnabled else colorDisabled
            )
        button.isEnabled = isEnable
    }
}