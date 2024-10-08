package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.miguelprojects.myapplication.databinding.ActivityNotificationIncomingOrdersBinding
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

class NotificationIncomingOrdersActivity : AppCompatActivity() {
    private lateinit var adapter: UserListAdapter
    private lateinit var binding: ActivityNotificationIncomingOrdersBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var workspaceRequestViewModel: WorkspaceRequestViewModel
    private lateinit var userList: List<UserModel>
    private lateinit var database: MyAppDatabase
    private var listSelectedUsers = mutableListOf<String>()
    private var workspaceId: String = ""
    private var workspaceCreator: String = ""
    private var workspaceIsPulic: Boolean = true
    private var workspaceModel = WorkspaceModel()
    private var userId: String = ""
    private var offUserId: Long = 0L
    private var workspaceCode: String = ""

    private val workspaceViewModel: WorkspaceViewModel by lazy {
        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)
        ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityNotificationIncomingOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        DrawerConfigurator(
            this,
            UserModel(),
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        startTools()
        getExtraValues()
        initializeApp()

        Handler(Looper.getMainLooper()).postDelayed({
            managerProgressBar(false)
        }, 1000)
    }

    fun startTools() {
        // Obtenha a instância do banco de dados a partir da aplicação
        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)

        userViewModel = ViewModelProvider(this, userFactory)[UserViewModel::class.java]
        workspaceRequestViewModel = ViewModelProvider(this)[WorkspaceRequestViewModel::class.java]
    }

    private fun getExtraValues() {
        workspaceId = intent.getStringExtra("workspaceId") ?: ""
        userId = intent.getStringExtra("userId") ?: ""
        offUserId = intent.getLongExtra("offUserId", 0L)
        workspaceCreator = intent.getStringExtra("workspaceCreator") ?: ""
        workspaceCode = intent.getStringExtra("workspaceCode") ?: ""

        verifyIfEmptyValues()
    }

    private fun verifyIfEmptyValues() {
        if (userId.isEmpty() || workspaceId.isEmpty() || workspaceCode.isEmpty()) {
            errorGeneral()
        }
    }

    private fun errorGeneral() {
        Toast.makeText(
            this,
            "Erro ao acessar a página. Por favor, Reporte esse problema!",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("verifyIfEmptyValues", "Erro, dados necessários estão vazios")
        finish()
    }

    private fun initializeApp() {
        managerProgressBar(true)
        binding.textInviteCode.text = workspaceCode

        if (NetworkChangeReceiver().isNetworkConnected(this)) {
            loadWorkspaceFirebase()
        } else {
            loadWorkspaceRoom()
        }
    }

    private fun loadWorkspaceFirebase() {
        workspaceViewModel.workspaceModel.observe(this, Observer {
            if (!it.public) {
                Toast.makeText(
                    this,
                    "Configure o seu grupo como público para acessar essa função!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@Observer
            }

            binding.textTitleWorkspace.text = it.name

            loadUserList()

            setOnClickListeners()

            workspaceViewModel.workspaceModel.removeObservers(this)
        })

        workspaceViewModel.loadData(workspaceId)
    }

    private fun loadWorkspaceRoom() {
        workspaceViewModel.loadDataRoom(workspaceId) {
            if (it == null) {
                errorGeneral()
                return@loadDataRoom
            }

            binding.textTitleWorkspace.text = it.name

            if (!it.public) {
                Toast.makeText(
                    this,
                    "Configure o seu grupo como público para acessar essa função!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@loadDataRoom
            }

            showMessageOffline()

            setOnClickListeners()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showMessageOffline() {
        binding.textEmptyList.text = "Sem conexão com a internet!"

        managerViewLayoutRequired(
            false,
            false
        )
    }

    private fun reorderUsersList(
        usersList: List<UserModel>,
        creatorWorkspaceId: String?
    ): List<UserModel> {
        val (creator, others) = usersList.partition { it.id == creatorWorkspaceId }
        return creator + others
    }

    private fun configureRecycleViewAdapter() {
        binding.recycleviewNotificationIncomingOrders.layoutManager = LinearLayoutManager(this)
        adapter = UserListAdapter(userList, true, workspaceCreator, UserOnClickListener { user ->
            if (listSelectedUsers.contains(user.id)) {
                listSelectedUsers.remove(user.id)
            } else {
                listSelectedUsers.add(user.id)
            }

            changeStyleButton(
                binding.buttonPermission,
                R.color.quat_caribbean_green,
                R.color.light_gray,
                listSelectedUsers.isNotEmpty()
            )
            changeStyleButton(
                binding.buttonDeleteRequest,
                R.color.red,
                R.color.light_gray,
                listSelectedUsers.isNotEmpty()
            )
        })

        binding.recycleviewNotificationIncomingOrders.adapter = adapter
    }

    private fun setOnClickListeners() {
        binding.textInviteCode.setOnClickListener {
            val codeCopy = binding.textInviteCode.text.toString()

            val clipboardManager =
                this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Texto Copiado", codeCopy)
            clipboardManager.setPrimaryClip(clipData)

            Toast.makeText(this, "Texto copiado: $codeCopy", Toast.LENGTH_SHORT).show()
        }

        binding.buttonPermission.setOnClickListener {
            if (NetworkChangeReceiver().isNetworkConnected(this)) {

                if (listSelectedUsers.isNotEmpty()) {
                    workspaceRequestViewModel.allowUserRequest(
                        workspaceId,
                        listSelectedUsers
                    ) { res, message ->
                        if (res) {
                            println("Processo bem sucedido")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Houve um problema ao realizar essa ação",
                                Toast.LENGTH_LONG
                            ).show()
                            println("Processo com problemas. ${message}")
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "É necessário conexão com a internet para realizar essa ação",
                    Toast.LENGTH_LONG
                ).show()
            }
            println("Button permission clicado")
        }

        binding.buttonDeleteRequest.setOnClickListener {
//            fazer função para deletar as requisições selecionadas
            println("Button delete clicado")
            if (NetworkChangeReceiver().isNetworkConnected(this)) {

                if (listSelectedUsers.isNotEmpty()) {
                    workspaceRequestViewModel.deleteUserRequests(
                        workspaceId,
                        listSelectedUsers
                    ) { res, message ->
                        if (res) {
                            println("Processo bem sucedido")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Houve um problema ao realizar essa ação",
                                Toast.LENGTH_LONG
                            ).show()
                            println("Processo com problemas. ${message}")
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "É necessário conexão com a internet para realizar essa ação",
                    Toast.LENGTH_LONG
                ).show()
            }
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

    private fun loadUserList() {
        var previousUserIds: List<String>? = null

        // Declare o Observer antes de chamar a função que altera o LiveData
        workspaceRequestViewModel.listWorkspaceRequestModel.observe(
            this,
            Observer { listUser ->
                if (listUser.isNotEmpty()) {
                    val listUserIds = listUser.map { it.userId }

                    if (listUserIds != previousUserIds) {
                        previousUserIds = listUserIds
                        if (listUserIds.isNotEmpty()) {
                            userViewModel.loadListUserModel(listUserIds) { list ->
                                if (list.isNotEmpty()) {
                                    userList = reorderUsersList(list, workspaceCreator)

                                    configureRecycleViewAdapter()
                                    Log.d(
                                        "Teste Notify List Users",
                                        "Lista de usuarios requiridos tem: ${userList.size}"
                                    )
                                } else {
                                    Log.d("Teste Notify List Users", "Lista de usuarios está vazia")
                                }
                            }
                        } else {
                            Log.d("Teste Notify List Users", "Lista de ids está vazia")
                        }
                    }
                }

                managerViewLayoutRequired(
                    listUser.isNotEmpty(),
                    NetworkChangeReceiver().isNetworkConnected(this)
                )
            }
        )

        // Chame a função que altera o LiveData depois de definir o Observer
        workspaceRequestViewModel.loadRequestJoin(workspaceId)
    }

    private fun managerViewLayoutRequired(isNotEmpty: Boolean, isConnect: Boolean) {
        managerProgressBar(false)
        binding.textEmptyList.visibility = if (isNotEmpty) View.GONE else View.VISIBLE

//        binding.textInitial.visibility = if (isNotEmpty && isConnect) View.VISIBLE else View.GONE
        binding.recycleviewNotificationIncomingOrders.visibility =
            if (isConnect && isNotEmpty) View.VISIBLE else View.GONE

        binding.buttonPermission.visibility =
            if (isConnect && workspaceCreator == userId && isNotEmpty) View.VISIBLE else View.GONE
        binding.buttonDeleteRequest.visibility =
            if (isConnect && workspaceCreator == userId && isNotEmpty) View.VISIBLE else View.GONE
    }

    private fun managerProgressBar(isActive: Boolean) {
        binding.progressBar.visibility = if (isActive) View.VISIBLE else View.GONE
    }

}