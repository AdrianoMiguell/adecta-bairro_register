package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityCitizenDetailBinding
import com.miguelprojects.myapplication.factory.CitizenViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.util.CitizenManager
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatCep
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatCpf
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatSus
import com.miguelprojects.myapplication.util.StringsFormattingManager.formatTelephone
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class CitizenDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCitizenDetailBinding
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var workspaceId: String
    private lateinit var userId: String
    private lateinit var citizenId: String
    private lateinit var database: MyAppDatabase
    private var networkChangeReceiver = NetworkChangeReceiver()
    private var citizenModel: CitizenModel? = null
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

//        binding.layoutNavbarTop.buttonOpenMenu.setImageResource(R.drawable.baseline_arrow_back_24)
//        binding.layoutNavbarTop.buttonOpenMenu.setOnClickListener {
//            finish()
//        }
    }

    private fun updateUICitizen() {
        binding.imageCitizen.setImageResource(
            CitizenManager.getCitizenImage(citizenModel!!.birthdate, citizenModel!!.sex)
        )
    }

    private fun startTools() {
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

        citizenModel = intent.getParcelableExtra("citizenModel")

        if (networkChangeReceiver.isNetworkConnected(this)) {
            if (userId.isEmpty() || workspaceId.isEmpty() || citizenId.isEmpty() || citizenModel == null) {
                println("(${workspaceId} == null ${userId} == null)")
                println(citizenModel)
                Toast.makeText(this, "Erro ao carregar os dados do cidadão!", Toast.LENGTH_SHORT)
                    .show()
                finish()
                return
            }
        } else {
            println("( ${citizenModel} == null)")
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

    fun String?.formattedOrDefault(default: String = "Não informado"): String {
        return this?.ifEmpty { default } ?: default
    }

    companion object {
        private const val INACTIVE_CODE = 2
        private const val UPDATE_CODE = 3
    }
}