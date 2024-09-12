package com.miguelprojects.myapplication.ui.activitys.activity_workspace

import WorkspaceRepository
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.ActivityRegisterCitizenBinding
import com.miguelprojects.myapplication.factory.CitizenViewModelFactory
import com.miguelprojects.myapplication.factory.WorkspaceViewModelFactory
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.room.database.MyAppDatabase
import com.miguelprojects.myapplication.ui.fragments.citizen.RegisterPersonalDataFragment
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.DrawerConfigurator
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StringsFormattingManager.generateCPF
import com.miguelprojects.myapplication.util.StringsFormattingManager.generatePhoneNumber
import com.miguelprojects.myapplication.util.StringsFormattingManager.generateRandomManName
import com.miguelprojects.myapplication.util.StringsFormattingManager.generateRandomName
import com.miguelprojects.myapplication.util.StringsFormattingManager.generateRandomWomanName
import com.miguelprojects.myapplication.util.StringsFormattingManager.generateSUSNumber
import com.miguelprojects.myapplication.util.StringsFormattingManager.generateSex
import com.miguelprojects.myapplication.util.StyleSystemManager
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class RegisterCitizenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterCitizenBinding
    private lateinit var database: MyAppDatabase
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var workspaceModel: WorkspaceModel
    private lateinit var citizenModel: CitizenModel
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var workspaceId: String = ""
    private var citizenId: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterCitizenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StyleSystemManager.changeNavigationBarStyleWithColor(this, window)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = (application as MyApplication).database

        getDataIds()

        startTools()

        DrawerConfigurator(
            this,
            UserModel(),
            0,
            0,
            mapOf("userId" to userId)
        ).configureSimpleTopNavigation()

        if (citizenModel.id.isNotEmpty()) {
            loadFragmentCitizen(citizenModel)
        } else {
            val resultSex = generateSex()

            val newCitizen = CitizenModel(
                name = if (resultSex == "f") generateRandomWomanName() else if(resultSex == "m") generateRandomManName() else generateRandomName(),
                cpf = generateCPF(),
                sus = generateSUSNumber(),
                telephone = generatePhoneNumber(),
                sex = resultSex,
                birthdate = ConvertManager.generateRandomBirthdateTimestamp(),
                fathername = generateRandomManName(),
                mothername = generateRandomWomanName(),
                birthplace = "Terra natal",
                street = "Rua x",
                numberhouse = 7,
                cep = workspaceModel.cep,
                state = workspaceModel.state,
                city = workspaceModel.city,
                neighborhood = workspaceModel.neighborhood,
            )

            loadFragmentCitizen(newCitizen)
        }
    }

    private fun loadFragmentCitizen(citizenModel: CitizenModel) {
        println("No register Citizen: citizenId = $citizenId")
        loadFragment(
            RegisterPersonalDataFragment.newInstance(
                citizenModel,
                citizenModel,
                workspaceId,
                citizenId,
            )
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

    private fun getDataIds() {
        workspaceId = intent.getStringExtra("workspaceId") ?: ""
        citizenId = intent.getStringExtra("citizenId") ?: ""
        workspaceModel =
            intent.getParcelableExtra<WorkspaceModel>("workspaceModel") ?: WorkspaceModel()
        citizenModel =
            intent.getParcelableExtra<CitizenModel>("citizenModel") ?: CitizenModel()

        if (workspaceId.isEmpty()) errorLoadingData()
    }

    private fun errorLoadingData() {
        Toast.makeText(
            this,
            "Erro ao carregar esta tela. Entre em contato com o suporte!",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun loadFragment(fragment: Fragment) {
        if (!isFinishing && !isDestroyed) {
            val transition = supportFragmentManager.beginTransaction()
            transition.replace(R.id.fragment_container, fragment)
//        transition.addToBackStack(null)
            transition.commit()
        }
    }
}