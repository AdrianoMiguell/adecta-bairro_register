package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.CitizenListAdapter
import com.miguelprojects.myapplication.adapter.listener.CitizenOnClickListener
import com.miguelprojects.myapplication.databinding.ActivityDeleteCitizensBinding
import com.miguelprojects.myapplication.factory.CitizenViewModelFactory
import com.miguelprojects.myapplication.factory.UserViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.repository.UserRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.room.entity.Citizen
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class DeleteCitizensActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeleteCitizensBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var database: MyAppDatabase
    private lateinit var adapter: CitizenListAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private var isReceiverRegistered = false
    private var workspaceModel = WorkspaceModel()
    private var citizenList = mutableListOf<CitizenModel>()
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var listCitizensModelSelected = mutableListOf<CitizenModel>()
    private var listCitizensSelected = mutableListOf<Citizen>()
    private var workspaceId: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDeleteCitizensBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        startTools()

        getUserAndWorkspaceData()

        initializeWorkspace()

        DrawerConfigurator(
            this,
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        setOnClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()

        workspaceViewModel.workspaceModel.removeObservers(this)
        citizenViewModel.citizenListModel.removeObservers(this)
    }

    private fun startTools() {
        // Obtenha a instância do banco de dados a partir da aplicação
        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)

        val userDao = database.userDao()
        val userRepository = UserRepository(userDao)
        val userFactory = UserViewModelFactory(userRepository)

        userViewModel = ViewModelProvider(this, userFactory)[UserViewModel::class.java]

        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)

        workspaceViewModel =
            ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]

        val citizenDao = database.citizenDao()
        val citizenRepository = CitizenRepository(citizenDao)
        val citizenFactory = CitizenViewModelFactory(citizenRepository)
        citizenViewModel = ViewModelProvider(this, citizenFactory)[CitizenViewModel::class.java]
    }

    private fun getUserAndWorkspaceData() {
        userId = intent.getStringExtra("userId") ?: ""
        workspaceId = intent.getStringExtra("workspaceId") ?: ""

        verifyStatusUserAndWorkspaceData()
    }

    private fun verifyStatusUserAndWorkspaceData() {
        if (userId.isEmpty() || workspaceId.isEmpty()) {
            showErrorAndFinish()
            return
        }
    }

    private fun showErrorAndFinish() {
        Toast.makeText(
            this,
            "Erro ao acessar o grupo de trabalho. Reporte esse problema!",
            Toast.LENGTH_SHORT
        ).show()
        Log.d("Teste Workspace Main Activity", "Id do workspace ou user está nulo!")
        finish()
    }

    private fun initializeWorkspace() {
        if (networkChangeReceiver.isNetworkConnected(this)) {
            workspaceViewModel.workspaceModel.observe(
                this,
                Observer { workspace ->
                    if (workspace != null) {
                        workspaceModel = workspace
                        updateUIWorkspace()
                        loadInactiveCitizenFirebase()
                        managerViewDataLayout(false)
                    } else {
                        Log.d("Workspace load", "workspace não encontrado e nulo")
                        showErrorAndFinish()
                    }

                    workspaceViewModel.workspaceModel.removeObservers(this)
                })

            workspaceViewModel.loadData(workspaceId)
        } else {
            workspaceViewModel.loadDataRoom(workspaceId) { workspace ->
                if (workspace != null) {
                    workspaceModel = WorkspaceModel.fromEntity(workspace)
                    updateUIWorkspace()
                    loadInactiveCitizenRoom()
                    managerViewDataLayout(false)
                } else {
                    Log.d("Dados off", "Dados nulos do workspace off")
                    showErrorAndFinish()
                }
            }
        }
    }

    private fun updateUIWorkspace() {
        val titleWorkspace = workspaceModel.name
        println(ConvertManager.capitalizeWords(titleWorkspace))
        binding.textTitleWorkspace.text = ConvertManager.capitalizeWords(titleWorkspace)
    }

    private fun loadInactiveCitizenFirebase() {
        println("delete activity - no loadInactiveCitizenFirebase")
        citizenViewModel.loadInactiveCitizenFirebase(workspaceId)
        citizenViewModel.citizenListModel.observe(this, Observer { list ->
            citizenList = list.toMutableList()
            if (::adapter.isInitialized) {
                adapter.submitList(citizenList)
            } else {
                initializeRecyclerView()
            }
            managerViewDataLayout(true)
        })
    }

    private fun loadInactiveCitizenRoom() {
        citizenViewModel.loadInactiveCitizenRoom(workspaceId) { list ->
            citizenList = CitizenModel.fromEntityList(list.toMutableList())
            if (::adapter.isInitialized) {
                adapter.submitList(citizenList)
            } else {
                initializeRecyclerView()
            }
            managerViewDataLayout(true)
        }
    }

    private fun initializeRecyclerView() {
        binding.recycleviewCitizen.layoutManager = LinearLayoutManager(this)
        adapter =
            CitizenListAdapter(citizenList, true, CitizenOnClickListener { _, selectedList ->
                listCitizensModelSelected = selectedList.toMutableList()
                managerViewDataLayout(true)
            })
        binding.recycleviewCitizen.adapter = adapter
    }

    private fun managerViewDataLayout(isView: Boolean) {
        binding.textInitialEmpty.visibility = if (citizenList.isEmpty()) View.VISIBLE else View.GONE
        binding.layoutButtonView.visibility =
            if (listCitizensModelSelected.isEmpty()) View.GONE else View.VISIBLE

        binding.layoutDataMain.visibility = if (isView) View.VISIBLE else View.GONE
        binding.progressBar.visibility = if (isView) View.GONE else View.VISIBLE
    }

    private fun onClickButtonInformation() {
        val intent = Intent(this, CreateEditWorkspaceActivity::class.java)
        intent.putExtra("workspaceId", workspaceId)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun setOnClickListeners() {
        binding.imageInitialWorkspace.setOnClickListener {
            onClickButtonInformation()
        }
        binding.textTitleWorkspace.setOnClickListener {
            onClickButtonInformation()
        }

        binding.buttonDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Remover dados dos cidadãos")
            builder.setMessage("Você deseja remover os dados dos cidadãos selecionados?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { _, _ ->
                try {
                    listCitizensSelected =
                        Citizen.toListCitizen(listCitizensModelSelected, workspaceId)

                    println(listCitizensModelSelected.size)
                    if (networkChangeReceiver.isNetworkConnected(this)) {
                        deleteCitizensFirebase()
                    } else {
                        deleteCitizensRoom(false)
                    }
                } catch (e: Exception) {
                    Log.d("deleteCitizens - setOnClick", "Erro ao deletar os dados: ${e.message}")
                    Toast.makeText(
                        this,
                        "Erro ao executar essa ação. Por favor, Reporte este problema!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }
            // Mostrar o AlertDialog
            builder.show()
        }


        binding.buttonRestoreActive.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Restaurar dados dos cidadãos")
            builder.setMessage("Você deseja restaurar os dados dos cidadãos selecionados?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                try {
                    listCitizensSelected =
                        Citizen.toListCitizen(listCitizensModelSelected, workspaceId)

                    println(listCitizensModelSelected.size)
                    if (networkChangeReceiver.isNetworkConnected(this)) {
                        restoreCitizensFirebase()
                    } else {
                        restoreCitizensRoom(true)
                    }
                } catch (e: Exception) {
                    Log.d("deleteCitizens - setOnClick", "Erro ao restaurar os dados: ${e.message}")
                    Toast.makeText(
                        this,
                        "Erro ao executar essa ação. Por favor, Reporte este problema!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }
            // Mostrar o AlertDialog
            builder.show()
        }
    }

    private fun deleteCitizensFirebase() {
        citizenViewModel.deleteListCitizenFirebase(workspaceId, listCitizensModelSelected) { res ->
            if (res) {
                deleteCitizensRoom(true)
            } else {
                Log.d("deleteCitizensFirebase", "Erro ao deletar")
                Toast.makeText(
                    this,
                    "Erro ao executar essa ação. Por favor, Reporte este problema!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteCitizensRoom(isConnected: Boolean) {
        Log.d("deleteCitizensRoom - setOnClick", "No delete")
        citizenViewModel.deleteListCitizenRoom(listCitizensSelected, isConnected) { res ->
            if (res) {
                val intent = Intent()
                setResult(DELETE_CODE, intent)
                finish()
            } else {
                Log.d("deleteCitizensRoom", "Erro ao deletar")
                Toast.makeText(
                    this,
                    "Erro ao executar essa ação. Por favor, Reporte este problema!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun restoreCitizensFirebase() {
        citizenViewModel.restoreCitizensFirebaseList(
            listCitizensModelSelected,
            workspaceId,
            true
        ) { res ->
            if (res) {
                restoreCitizensRoom(false)
            } else {
                Log.d("deleteCitizensFirebase", "Erro ao deletar")
                Toast.makeText(
                    this,
                    "Erro ao executar essa ação. Por favor, Reporte este problema!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun restoreCitizensRoom(needsUpdate: Boolean) {
        Log.d("deleteCitizensRoom - setOnClick", "No delete")

        citizenViewModel.restoreListCitizenRoom(listCitizensSelected, needsUpdate) { res ->
            if (res) {
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent()
                    setResult(DELETE_CODE, intent)
                    finish()
                }, 100)
            } else {
                Log.d("deleteCitizensRoom", "Erro ao deletar")
                Toast.makeText(
                    this,
                    "Erro ao executar essa ação. Por favor, Reporte este problema!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
        val transition = this.supportFragmentManager.beginTransaction()
        transition.replace(R.id.fragment_container_citizens, fragment)
        if (addToBack) {
            transition.addToBackStack(null)
        }
        transition.commit()
    }

    companion object {
        const val DELETE_CODE = 4
    }
}