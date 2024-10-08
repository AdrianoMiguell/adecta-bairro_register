package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityCreateEditWorkspaceBinding
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.room.entity.Workspace
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.NetworkSynchronizeUser
import com.miguelprojects.myapplication.util.NetworkSynchronizeWorkspace
import com.miguelprojects.myapplication.util.StringsFormattingManager.convertCapitalizeWord
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatCep
import com.miguelprojects.myapplication.util.StringsFormattingManager.formattedOrDefault
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class CreateEditWorkspaceActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityCreateEditWorkspaceBinding
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var userViewModel: UserViewModel
    private lateinit var database: MyAppDatabase
    private lateinit var networkSynchronizeWorkspace: NetworkSynchronizeWorkspace
    private lateinit var networkSynchronizeUser: NetworkSynchronizeUser
    private var enableButtonCloseWorkspace = false
    private var addTextEventListener = false
    private var textHandler: Handler? = null
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var oldWorkspaceModel: WorkspaceModel = WorkspaceModel()
    private var newWorkspaceModel = WorkspaceModel()
    private var workspaceId: String = ""
    private var userId: String = ""
    private var userModel = UserModel()
    private var isReceiverRegistered = false
    private var listMembers: List<UserModel> = emptyList()

    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "DATA_SYNCHRONIZED" -> {
                    progressBarView(true)
                    Handler(Looper.getMainLooper()).postDelayed({
                        progressBarView(false)
                        verifyExtrasData()
                        updateUISystem()
                    }, 500)
                }

                "DATA_OFF_SYNCHRONIZED" -> {
                    progressBarView(true)
                    Handler(Looper.getMainLooper()).postDelayed({
                        progressBarView(false)
                        verifyExtrasData()
                        updateUISystem()
                    }, 500)
                }

                "DATA_SYNCHRONIZED_USER" -> {
                    if (intent.getStringExtra("userId").isNullOrEmpty()) {
                        Toast.makeText(
                            this@CreateEditWorkspaceActivity,
                            "Sessão Encerrada! Por favor, realize login novamente!",
                            Toast.LENGTH_SHORT
                        ).show()
                        UserSessionManager.onUserNotFoundOrLogout(
                            this@CreateEditWorkspaceActivity,
                            userViewModel
                        )
                    }
                }

                "DATA_SYNCHRONIZED_WORKSPACE" -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (workspaceId.isNotEmpty() && intent.getStringExtra("workspaceId").isNullOrEmpty()) {
                            Toast.makeText(
                                this@CreateEditWorkspaceActivity,
                                "Sincronização dos dados em andamento!",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(
                                    this@CreateEditWorkspaceActivity,
                                    MainActivity::class.java
                                ).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                            )
                            finish()
                        }
                    }, 1000)
                }

                else -> {
                    println("broadcast inesperado!")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateEditWorkspaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        initializeActivity()
        initReceiverAndBroadcast()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(networkSynchronizeWorkspace)
                unregisterReceiver(networkSynchronizeUser)

                LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(uiUpdateReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    private fun initReceiverAndBroadcast() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        if (!isReceiverRegistered) {
            networkSynchronizeWorkspace =
                NetworkSynchronizeWorkspace(workspaceViewModel, workspaceId)
            networkSynchronizeUser =
                NetworkSynchronizeUser(userViewModel, sharedPreferences, userId)

            // Registrar o receiver do sistema para mudanças de rede
            registerReceiver(networkSynchronizeWorkspace, intentFilter)
            registerReceiver(networkSynchronizeUser, intentFilter)

            LocalBroadcastManager.getInstance(this).registerReceiver(
                uiUpdateReceiver,
                IntentFilter().apply {
                    addAction("DATA_SYNCHRONIZED")
                    addAction("DATA_SYNCHRONIZED_WORKSPACE")
                    addAction("DATA_SYNCHRONIZED_USER")
                }
            )
        }
    }

    private fun initializeActivity() {
        // Recupera o userId do Intent
        userId = intent.getStringExtra("userId") ?: ""
        workspaceId = intent.getStringExtra("workspaceId") ?: ""
        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)

        verifyExtrasData()

        startTools()

        DrawerConfigurator(
            this,
            UserModel(),
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        binding.textPublic.setOnClickListener {
            changeIsChecked(!binding.buttonPublic.isChecked)
        }

        progressBarView(true)
        changeStyleButton(
            binding.buttonCreateEdit,
            R.color.quat_caribbean_green,
            R.color.light_gray,
            false
        )

        getUserData()


        if (workspaceId.isEmpty()) {
            addNewWorkspace()
            updateUISystem()
            progressBarView(false)
        } else {
            prepareWorkspaceData()
        }
    }

    private fun getUserData() {
        userViewModel.loadUserRoom(userId) { user ->
            userModel = User.toUserModel(user!!)
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun verifyExtrasData() {
        if (userId.isEmpty()) {
            showToast("Erro ao captar dados de login. Por favor! Considere realizar login novamente!")
            navigateToMainActivity()
            return
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
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
    }

    private fun getValuesEditFields() {
        val name = binding.editName.text.toString().trim().convertCapitalizeWord()
        val description = binding.editDescription.text.toString().trim()
        val cep = binding.editCep.text.toString().trim()
        val state = binding.editState.text.toString().trim()
        val city = binding.editCity.text.toString().trim()
        val neighborhood = binding.editNeighborhood.text.toString().trim()
        val public = binding.buttonPublic.isChecked

        val workspaceModelTest = WorkspaceModel(
            name = name,
            description = description,
            cep = cep,
            state = state,
            city = city,
            neighborhood = neighborhood,
            public = public,
            inviteCode = oldWorkspaceModel.inviteCode,
            creator = oldWorkspaceModel.creator,
            userIds = oldWorkspaceModel.userIds
        )

        newWorkspaceModel = workspaceModelTest
    }

    private fun verifyIsNotEmptyValues(workspaceModel: WorkspaceModel): Boolean {
        return workspaceModel.name.isNotEmpty() && workspaceModel.cep.isNotEmpty() && workspaceModel.state.isNotEmpty() && workspaceModel.city.isNotEmpty() && workspaceModel.neighborhood.isNotEmpty()
    }

    private fun updateUISystem() {
        if (!addTextEventListener) {
            addEditTextListener(binding.editName, binding.buttonCreateEdit, oldWorkspaceModel.name)
            addEditTextListener(
                binding.editDescription,
                binding.buttonCreateEdit,
                oldWorkspaceModel.description
            )
            addEditTextListener(binding.editCep, binding.buttonCreateEdit, oldWorkspaceModel.cep)
            addEditTextListener(
                binding.editState,
                binding.buttonCreateEdit,
                oldWorkspaceModel.state
            )
            addEditTextListener(binding.editCity, binding.buttonCreateEdit, oldWorkspaceModel.city)
            addEditTextListener(
                binding.editNeighborhood,
                binding.buttonCreateEdit,
                oldWorkspaceModel.neighborhood
            )

            binding.buttonPublic.setOnCheckedChangeListener { _, isChecked ->
                println("Mudou aqui no setOnCheckedChangeListener")
                if (workspaceId.isNotEmpty()) {
                    if (isChecked != oldWorkspaceModel.public) {
                        changeStyleButton(
                            binding.buttonCreateEdit,
                            R.color.quat_caribbean_green,
                            R.color.light_gray,
                            true
                        )
                    } else {
                        changeStyleButton(
                            binding.buttonCreateEdit,
                            R.color.quat_caribbean_green,
                            R.color.light_gray,
                            false
                        )
                    }
                }
            }

            addTextEventListener = true
        }
    }

    private fun addNewWorkspace() {
        progressBarView(false)
        binding.layoutFormsWorkspace.visibility = View.VISIBLE
        binding.layoutInformationWorkspace.informationWorkspace.visibility = View.GONE
        binding.buttonDelete.visibility = View.GONE

        changeStyleButton(
            binding.buttonDelete,
            R.color.red,
            R.color.light_gray,
            false
        )

        binding.buttonCreateEdit.setOnClickListener {
            getValuesEditFields()

            changeStyleButton(
                binding.buttonCreateEdit,
                R.color.quat_caribbean_green,
                R.color.light_gray,
                false
            )

            if (verifyIsNotEmptyValues(newWorkspaceModel)) {
                val needsSync = !networkChangeReceiver.isNetworkConnected(this)

                newWorkspaceModel.id = ""
                newWorkspaceModel.creator = userId

                userViewModel.loadUserRoom(userId) { user ->
                    if (user == null) {
                        showToast("Erro ao captar dados do criador do grupo!")
                        return@loadUserRoom
                    }

                    if (networkChangeReceiver.isNetworkConnected(this)) {
                        val userIds: MutableMap<String, Boolean> = mutableMapOf(userId to true)
                        newWorkspaceModel.userIds = userIds

                        addWorkspaceDataFirebase()
                    } else {
                        addWorkspaceDataRoom(needsSync)
                    }
                }
            } else {
                showToast(
                    "Preencha todos os campos vazios."
                )
            }

            progressBarView(false)
        }
    }

    private fun addWorkspaceDataRoom(
        needsSync: Boolean
    ) {
        try {
            val workspaceEntity = newWorkspaceModel.toWorkspaceEntity(needsSync)
            println(userId)

            workspaceViewModel.saveDataRoom(
                workspaceEntity,
                userId,
                needsSync
            ) { _ ->
                changeStyleButton(
                    binding.buttonCreateEdit,
                    R.color.quat_caribbean_green,
                    R.color.light_gray,
                    true
                )

                animateAfterActionWorkspace("Dados salvos com sucesso!", Activity.RESULT_OK)
            }
        } catch (e: Exception) {
            progressBarView(false)

            showToast("Ocorreu um problema ao execultar esta ação. Por favor, reporte esse erro!")
            Log.d("Erro ao executar salvamento off", "${e.message}")
        } finally {
            changeStyleButton(
                binding.buttonCreateEdit,
                R.color.quat_caribbean_green,
                R.color.light_gray,
                true
            )
        }
    }

    private fun addWorkspaceDataFirebase() {
        workspaceViewModel.saveData(userId, newWorkspaceModel) { success, workspaceId ->
            if (success && workspaceId.isNotEmpty()) {
                newWorkspaceModel.id = workspaceId
                addWorkspaceDataRoom(false)
            } else {
                progressBarView(false)
                showToast("Ocorreu um problema ao execultar esta ação. Por favor, reporte esse erro!")
            }

            changeStyleButton(
                binding.buttonCreateEdit,
                R.color.quat_caribbean_green,
                R.color.light_gray,
                true
            )
        }
    }

    private fun prepareWorkspaceData() {
        if (networkChangeReceiver.isNetworkConnected(this)) {
            workspaceViewModel.loadData(workspaceId)
            workspaceViewModel.workspaceModel.observe(this, Observer { workspace ->
                println(workspace)
                if (workspace != null) {
                    oldWorkspaceModel = workspace
                    if (workspace.creator == userId) {
                        binding.layoutFormsWorkspace.visibility = View.VISIBLE
                        binding.layoutInformationWorkspace.buttonCloseWorkspace.visibility =
                            View.GONE
                        enableButtonCloseWorkspace = true

                        editNewWorkspace()
                        deleteWorkspace()
                    } else {
                        binding.layoutFormsWorkspace.visibility = View.GONE
                        binding.layoutInformationWorkspace.buttonCloseWorkspace.visibility =
                            View.VISIBLE
                        enableButtonCloseWorkspace = true
                        removeWorkspaceMember()
                        setButtonViewMembers()

                        setValuesWorkspace()
                    }

                    updateUISystem()
                    setOnViewTerms()

                    binding.layoutInformationWorkspace.informationWorkspace.visibility =
                        View.VISIBLE
                } else {
                    showToast("Problemas em carregar os dados do grupo")
                    finish()
                }

                progressBarView(false)
            })
        } else {
//            Carregar os dados off
            workspaceViewModel.loadDataRoom(workspaceId) { workspace ->
                if (workspace != null) {
                    oldWorkspaceModel = WorkspaceModel.fromEntity(workspace)
                    if (workspace.creator == userId) {
                        binding.layoutFormsWorkspace.visibility = View.VISIBLE
                        binding.layoutInformationWorkspace.buttonCloseWorkspace.visibility =
                            View.GONE

                        editNewWorkspace()
                        deleteWorkspace()
                    } else {
                        binding.layoutFormsWorkspace.visibility = View.GONE
                        binding.layoutInformationWorkspace.buttonCloseWorkspace.visibility =
                            View.VISIBLE
                        removeWorkspaceMember()
                        setButtonViewMembers()

                        changeStyleButton(
                            binding.buttonCreateEdit,
                            R.color.quat_caribbean_green,
                            R.color.light_gray,
                            false
                        )
                        setValuesWorkspace()
                    }

                    updateUISystem()
                    setOnViewTerms()

                    binding.layoutInformationWorkspace.informationWorkspace.visibility =
                        View.VISIBLE
                } else {
                    showToast("Problemas em carregar os dados do grupo")
                    finish()
                }
            }
        }
    }

    private fun removeWorkspaceMember() {
        binding.layoutInformationWorkspace.buttonCloseWorkspace.setOnClickListener {
            if (networkChangeReceiver.isNetworkConnected(this)) {
                workspaceViewModel.removeWorkspaceMember(userId, workspaceId) { res ->
                    if (res) {
                        animateAfterActionWorkspace("Você saiu do grupo!", UPDATE_CODE)
                    }
                }
            } else {
                showToast("Por segurança, permitimos a saida do grupo apenas quando a conexão for reestabelecida!")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setValuesWorkspace() {
        progressBarView(false)
        3
        binding.layoutInformationWorkspace.textTitleWorkspace.text =
            "${oldWorkspaceModel.name}"
        binding.layoutInformationWorkspace.textDescriptionWorkspace.text =
            "Descrição: ${oldWorkspaceModel.description.formattedOrDefault()}"
        binding.layoutInformationWorkspace.textCepWorkspace.text =
            "CEP: ${oldWorkspaceModel.cep.formatCep()}"
        binding.layoutInformationWorkspace.textStateWorkspace.text =
            "Estado: ${oldWorkspaceModel.state}"
        binding.layoutInformationWorkspace.textCityWorkspace.text =
            "Cidade: ${oldWorkspaceModel.city}"
        binding.layoutInformationWorkspace.textNeighborhoodWorkspace.text =
            "Bairro: ${oldWorkspaceModel.neighborhood}"

        binding.editName.setText(oldWorkspaceModel.name)
        binding.editDescription.setText(oldWorkspaceModel.description)
        binding.editCep.setText(oldWorkspaceModel.cep)
        binding.editState.setText(oldWorkspaceModel.state)
        binding.editCity.setText(oldWorkspaceModel.city)
        binding.editNeighborhood.setText(oldWorkspaceModel.neighborhood)
        binding.buttonPublic.isChecked = oldWorkspaceModel.public
    }

    private fun addEditTextListener(
        editText: EditText,
        buttonRef: Button,
        oldValue: String,
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                textHandler?.removeCallbacksAndMessages(null)
                textHandler =
                    Handler(Looper.getMainLooper()).apply {
                        postDelayed({
                            getValuesEditFields()

                            if (workspaceId.isEmpty()) {
//                    button add
                                changeStyleButton(
                                    buttonRef,
                                    R.color.quat_caribbean_green,
                                    R.color.light_gray,
                                    verifyIsNotEmptyValues(newWorkspaceModel)
                                )
                            } else {
//                    button edit
                                val verifyChange = s?.toString()
                                    ?.trim() != oldValue.trim() && verifyIsNotEmptyValues(
                                    newWorkspaceModel
                                )

                                changeStyleButton(
                                    buttonRef,
                                    R.color.quat_caribbean_green,
                                    R.color.light_gray,
                                    verifyChange
                                )
                            }
                        }, 500)
                    }
            }
        })
    }

    fun changeStyleButton(
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

    private fun setButtonViewMembers() {
        binding.layoutInformationWorkspace.buttonViewWorkspaceMembers.setOnClickListener {
            val intent = Intent(this, WorkspaceMembersActivity::class.java)
            intent.apply {
                putExtra("workspaceId", workspaceId)
                putExtra("userId", userId)
            }
            startActivity(intent)
        }
    }

    private fun editNewWorkspace() {
        setValuesWorkspace()

        binding.titleActivityWorkspace.text = "Editar Grupo de Trabalho"
        binding.buttonCreateEdit.text = "Editar"

        setButtonViewMembers()

        binding.buttonCreateEdit.setOnClickListener {
            newWorkspaceModel = oldWorkspaceModel
            getValuesEditFields()
            changeStyleButton(
                binding.buttonCreateEdit,
                R.color.quat_caribbean_green,
                R.color.light_gray,
                false
            )

            if (!verifyIsNotEmptyValues(newWorkspaceModel)) {
                showToast("Preencha todos os campos vazios.")
                return@setOnClickListener
            }

            newWorkspaceModel.id = workspaceId
            val workspace =
                newWorkspaceModel.toWorkspaceEntity(!networkChangeReceiver.isNetworkConnected(this))
            workspace.id = workspaceId

            if (networkChangeReceiver.isNetworkConnected(this)) {
                try {
                    workspaceViewModel.updateData(newWorkspaceModel) { res, message ->
                        if (res) {
                            try {
                                workspace.needsSync = false
                                workspace.needsUpdate = false
                                workspaceViewModel.updateWorkspaceRoom(workspace)
                                animateAfterActionWorkspace(
                                    "Dados atualizados com sucesso!",
                                    UPDATE_CODE
                                )
                            } catch (e: Exception) {
                                showToast("Erro ao atualizar os dados do grupo.")
                                Log.d("Update - activity", "Erro: ${e.message}")
                            } finally {
                                changeStyleButton(
                                    binding.buttonCreateEdit,
                                    R.color.quat_caribbean_green,
                                    R.color.light_gray,
                                    true
                                )
                            }
                        } else {
                            showToast("Erro ao atualizar os dados do grupo.")
                        }
                    }
                } catch (e: Exception) {
                    showToast("Erro ao atualizar os dados.")
                    Log.d("Update - activity", "Erro: ${e.message}")
                } finally {
                    changeStyleButton(
                        binding.buttonCreateEdit,
                        R.color.quat_caribbean_green,
                        R.color.light_gray,
                        true
                    )
                }
            } else {
//                Update off
                try {
                    workspace.needsSync = false
                    workspace.needsUpdate = true
                    workspaceViewModel.updateWorkspaceRoom(workspace)
                    animateAfterActionWorkspace("Dados atualizados com sucesso!", UPDATE_CODE)
                } catch (e: Exception) {
                    showToast("Erro ao atualizar os dados do grupo.")
                    Log.d("Update - activity", "Erro: ${e.message}")
                } finally {
                    changeStyleButton(
                        binding.buttonCreateEdit,
                        R.color.quat_caribbean_green,
                        R.color.light_gray,
                        true
                    )
                }
            }
        }
    }

    private fun deleteWorkspace() {
        binding.buttonDelete.visibility = View.VISIBLE

        if (networkChangeReceiver.isNetworkConnected(this) && oldWorkspaceModel.creator.isNotEmpty() && userId.isNotEmpty() && oldWorkspaceModel.creator == userId) {
            changeStyleButton(
                binding.buttonDelete,
                R.color.red,
                R.color.light_gray,
                true
            )

            binding.buttonDelete.setOnClickListener {
                changeStyleButton(
                    binding.buttonDelete,
                    R.color.red,
                    R.color.light_gray,
                    false
                )

                val builder = AlertDialog.Builder(this)

                builder.setTitle("Deletar Grupo de Trabalho")

                builder.setMessage("Deseja mesmo deletar este grupo de trabalho? Os seus respectivos dados serão também excluidos.")

                builder.setPositiveButton("Sim") { _, _ ->
                    val workspace = oldWorkspaceModel.toWorkspaceEntity(
                        !networkChangeReceiver.isNetworkConnected(this)
                    )
                    workspace.id = workspaceId
                    deleteWorkspaceFirebase(workspace)
                }

                builder.setNegativeButton("Não") { dialog, _ ->
                    dialog.dismiss()

                    changeStyleButton(
                        binding.buttonDelete,
                        R.color.red,
                        R.color.light_gray,
                        true
                    )
                }

                builder.show()
            }
        } else {
            changeStyleButton(
                binding.buttonDelete,
                R.color.red,
                R.color.light_gray,
                false
            )
        }
    }

    private fun deleteWorkspaceFirebase(workspace: Workspace) {
        progressBarView(true)

        try {
            workspaceViewModel.deleteWorkspaceFirebase(workspaceId) { res, message ->
                if (res) {
                    deleteWorkspaceRoom(workspace)
                } else {
                    showToast(message)
                }
            }
        } catch (e: Exception) {
            progressBarView(false)
            Log.d("Erro delete", "${e.message}")
            showToast("Erro ao deletar os dados do grupo!")
        }
    }

    private fun deleteWorkspaceRoom(workspace: Workspace) {
        try {
            workspaceViewModel.deleteWorkspaceRoom(workspace)
            animateAfterActionWorkspace("Dados salvos com sucesso!", DELETE_CODE)
        } catch (e: Exception) {
            Log.d("deleteWorkspaceRoom", "Erro ao deletar dados do grupo.")
            showToast("Erro ao deletar dados do grupo.")
        } finally {
            changeStyleButton(
                binding.buttonDelete,
                R.color.red,
                R.color.light_gray,
                true
            )

            progressBarView(false)
        }
    }

    private fun progressBarView(status: Boolean) {
        if (status) {
            println("Progress Bar visivel")
            binding.layoutContainsProgressBar.visibility = View.VISIBLE
            binding.includeProgressBar.layoutProgressBar.visibility = View.VISIBLE
        } else {
            println("Progress Bar não visivel")
            binding.layoutContainsProgressBar.visibility = View.GONE
            binding.includeProgressBar.layoutProgressBar.visibility = View.GONE
        }
    }

    private fun changeIsChecked(state: Boolean) {
        binding.buttonPublic.isChecked = state
    }

//    fun String?.formattedOrDefault(default: String = "Não informado"): String {
//        return this?.ifEmpty { default } ?: default
//    }

    private fun animateAfterActionWorkspace(
        message: String, resultCode: Int?,
    ) {
        binding.main.animate()
            .alpha(0.0f)
            .setDuration(250)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)

                    showToast(message)

                    val intent = Intent(
                        this@CreateEditWorkspaceActivity,
                        MainActivity::class.java
                    ).apply {
                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    if (resultCode != null) {
                        setResult(resultCode, intent)
                    }

                    startActivity(intent)
                    finish()
                }
            })
    }

    private fun setOnViewTerms() {
        binding.layoutInformationWorkspace.layoutPrivacyTerms.setOnClickListener {
            try {
                println("Abrindo nova tela privacy terms")
                val intent = Intent(this, PrivacyTermsActivity::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("workspace_id", workspaceId)
                startActivity(intent)
            } catch (e: Exception) {
                Log.d("Erro ao abrir nova tela", "Erro: ${e.message}")
            }
        }
    }

    companion object {
        const val DELETE_CODE = 2
        const val UPDATE_CODE = 3
    }
}
