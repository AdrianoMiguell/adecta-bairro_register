package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityCitizenDetailBinding
import com.miguelprojects.myapplication.factory.CitizenViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.ui.activitys.MainActivity
import com.miguelprojects.myapplication.util.CitizenManager
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.NetworkSynchronizeUser
import com.miguelprojects.myapplication.util.NetworkSynchronizeWorkspace
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatCep
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatCpf
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatSus
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatTelephone
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class CitizenDetailActivity : AppCompatActivity() {
    private lateinit var userViewModel: UserViewModel
    private lateinit var binding: ActivityCitizenDetailBinding
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var workspaceId: String
    private lateinit var userId: String
    private lateinit var citizenId: String
    private lateinit var database: MyAppDatabase
    private lateinit var networkSynchronizeUser: NetworkSynchronizeUser
    private lateinit var networkSynchronizeWorkspace: NetworkSynchronizeWorkspace
    private lateinit var sharedPreferences: SharedPreferences
    private var isReceiverRegistered = false
    private var networkChangeReceiver = NetworkChangeReceiver()
    private var citizenModel: CitizenModel? = null
    private var workspaceModel: WorkspaceModel? = null
    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val citizenResult = result.data?.getParcelableExtra<CitizenModel>("citizenResult")

            when (result.resultCode) {
                UPDATE_CODE -> {
                    println("Chegou aqui no update code OK!")
                    if (citizenResult != null) {
                        println(citizenResult)
                        citizenModel = citizenResult
                        setCitizenValues()

                        val resultIntent = Intent()
                        resultIntent.putExtra("citizenResult", citizenModel)
                        setResult(UPDATE_CODE, resultIntent)
                        finish()
                    }
                }
            }
        }

    private val uiUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "DATA_OFF_SYNCHRONIZED" -> finish()
                "DATA_SYNCHRONIZED_USER" -> {
                    if (intent.getStringExtra("userId").isNullOrEmpty()) {
                        Toast.makeText(
                            this@CitizenDetailActivity,
                            "Sessão Encerrada! Por favor, realize login novamente!",
                            Toast.LENGTH_SHORT
                        ).show()
                        UserSessionManager.onUserNotFoundOrLogout(
                            this@CitizenDetailActivity,
                            userViewModel
                        )
                    }
                }

                "DATA_SYNCHRONIZED_WORKSPACE" -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (intent.getStringExtra("workspaceId").isNullOrEmpty()) {
                            Toast.makeText(
                                this@CitizenDetailActivity,
                                "Sincronização dos dados em andamento!",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(
                                Intent(
                                    this@CitizenDetailActivity,
                                    MainActivity::class.java
                                ).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                            )
                            finish()
                        }
                    }, 1000)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCitizenDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        getExtraDatas()

        startTools()

        updateUICitizen()

        DrawerConfigurator(this, UserModel(), 0, 0, mapOf("userId" to userId)).configureSimpleTopNavigation()

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
//                    addAction("DATA_SYNCHRONIZED")
                    addAction("DATA_OFF_SYNCHRONIZED")
                    addAction("DATA_SYNCHRONIZED_USER")
                    addAction("DATA_SYNCHRONIZED_WORKSPACE")
                }
            )
            isReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(networkSynchronizeUser)
            } catch (e: IllegalArgumentException) {
                // O receptor não estava registrado, não faça nada
            } finally {
                isReceiverRegistered = false
            }
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(uiUpdateReceiver)
        workspaceViewModel.workspaceModel.removeObservers(this)
        citizenViewModel.citizenListModel.removeObservers(this)
    }

    private fun updateUICitizen() {
        binding.imageCitizen.setImageResource(
            CitizenManager.getCitizenImage(citizenModel!!.birthdate, citizenModel!!.sex)
        )
    }

    private fun startTools() {
        sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE)

        val userDao = database.workspaceDao()
        val userRepository = WorkspaceRepository(userDao)
        val userFactory = WorkspaceViewModelFactory(userRepository)

        userViewModel =
            ViewModelProvider(this, userFactory)[UserViewModel::class.java]

        val workspaceDao = database.workspaceDao()
        val workspaceRepository = WorkspaceRepository(workspaceDao)
        val workspaceFactory = WorkspaceViewModelFactory(workspaceRepository)

        workspaceViewModel =
            ViewModelProvider(this, workspaceFactory)[WorkspaceViewModel::class.java]

        val citizenDao = database.citizenDao()
        val citizenRepository = CitizenRepository(citizenDao)
        val citizenFactory = CitizenViewModelFactory(citizenRepository)

        citizenViewModel =
            ViewModelProvider(this, citizenFactory)[CitizenViewModel::class.java]
    }

    private fun getExtraDatas() {
        userId = intent.getStringExtra("userId") ?: ""
        workspaceId = intent.getStringExtra("workspaceId") ?: ""
        citizenId = intent.getStringExtra("citizenId") ?: ""

        workspaceModel = intent.getParcelableExtra("workspaceModel")
        citizenModel = intent.getParcelableExtra("citizenModel")

        if (networkChangeReceiver.isNetworkConnected(this)) {
            if (userId.isEmpty() || workspaceId.isEmpty() || citizenId.isEmpty() || citizenModel == null || workspaceModel == null) {
                Toast.makeText(this, "Erro ao carregar os dados do cidadão!", Toast.LENGTH_SHORT)
                    .show()
                finish()
                return
            }
        } else {
            if (citizenModel == null) {
                Toast.makeText(this, "Erro ao carregar os dados do cidadão!", Toast.LENGTH_SHORT)
                    .show()
                finish()
                return
            }
        }

        setCitizenValues()
        setClickListeners()

        println(citizenModel)
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun setCitizenValues() {
//        TODO("Not yet implemented")
        val age = ConvertManager.calculateFullAge(citizenModel!!.birthdate)
        val date = ConvertManager.convertLongForString(citizenModel!!.birthdate)
        val sex = when (citizenModel!!.sex) {
            "f" -> "Feminino"
            "m" -> "Masculino"
            else -> "Indefinido"
        }

        binding.textNameCitizen.text = if (citizenModel != null) {
            "Nome: ${citizenModel!!.name}"
        } else {
            "Nome: Desconhecido"
        }

        binding.textNumberregisterCitizen.text =
            "Número do Registro: ${citizenModel!!.numberregister}"
        binding.textAgeCitizen.text = "Idade: ${age.ifEmpty { "Desconhecida" }}"
        binding.textSexCitizen.text = "Sexo: ${sex}"
        binding.textTelephoneCitizen.text = "Telefone: ${citizenModel!!.telephone.formatTelephone()}"
        binding.textBirthdateCitizen.text = "Data de nascimento: ${date.ifEmpty { "Desconhecida" }}"
        binding.textBirthplaceCitizen.text = "Local de Nascimento: ${citizenModel!!.birthplace}"
        binding.textCpfCitizen.text = "CPF: ${citizenModel!!.cpf.formatCpf()}"
        binding.textSusCitizen.text = "Cartão SUS: ${citizenModel?.sus?.formatSus()}"
        binding.textMotherCitizen.text = "Nome da Mãe: ${citizenModel!!.mothername}"
        binding.textFatherCitizen.text = "Nome do Pai: ${citizenModel!!.fathername}"
        binding.textCepCitizen.text = "CEP: ${citizenModel?.cep.formatCep()}"
        binding.textStateCitizen.text = "Estado: ${citizenModel!!.state}"
        binding.textCityCitizen.text = "Cidade: ${citizenModel!!.city}"
        binding.textNeighborhoodCitizen.text = "Bairro: ${citizenModel!!.neighborhood}"
        binding.textStreetCitizen.text = "Rua: ${citizenModel!!.street}"
        binding.textNumberhouseCitizen.text =
            "Número da casa: ${citizenModel!!.numberhouse}"
        binding.textAddonsCitizen.text = "Complementos: ${citizenModel!!.addons.formattedOrDefault()}"
    }

    private fun setClickListeners() {
        binding.buttonEdit.setOnClickListener {
            val intent = Intent(this, RegisterCitizenActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("workspaceId", workspaceId)
            intent.putExtra("citizenId", citizenId)
            intent.putExtra("citizenModel", citizenModel)
            result.launch(intent)
        }

        binding.buttonDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Remover dados do cidadão")
            builder.setMessage("Você deseja remover os dados deste cidadão?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                if (networkChangeReceiver.isNetworkConnected(this)) {
                    inActiveCitizenFirebase()
                } else {
                    try {
                        inActiveCitizenRoom(true)
                    } catch (e: Exception) {
                        Toast.makeText(this, "Erro ao deletar os dados", Toast.LENGTH_SHORT).show()
                        println("Erro ao deletar os dados do cidadão : ${e.message}")
                    }
                }
            }

            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }
            // Mostrar o AlertDialog
            builder.show()
        }

        binding.layoutPrivacyTerms.setOnClickListener {
            val intent = Intent(this, PrivacyTermsActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("workspace_id", workspaceId)
            intent.putExtra("citizen_id", citizenId)
            startActivity(intent)
        }
    }

    private fun inActiveCitizenFirebase() {
        citizenViewModel.updateActiveCitizenFirebase(citizenModel!!, workspaceId, false) { res ->
            if (res) {
                inActiveCitizenRoom(false)
            } else {
                Toast.makeText(this, "Problemas ao realizar essa ação", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun inActiveCitizenRoom(needsUpdate: Boolean) {
        citizenViewModel.inActiveCitizenRoom(citizenId, false, needsUpdate)
        val resultIntent = Intent()
        resultIntent.putExtra("citizenResult", citizenModel)
        setResult(INACTIVE_CODE, resultIntent)
        finish()
    }

    private fun String?.formattedOrDefault(default: String = "Não informado"): String {
        return this?.ifEmpty { default } ?: default
    }

    companion object {
        private const val INACTIVE_CODE = 2
        private const val UPDATE_CODE = 3
    }
}