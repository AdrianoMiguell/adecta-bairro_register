package com.miguelprojects.myapplication.ui.fragments.citizen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.FragmentRegisterCitizenAdressDataBinding
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.room.entity.Citizen
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.StringsFormattingManager.isValidBirthdate
import com.miguelprojects.myapplication.util.StringsFormattingManager.isValidCep
import com.miguelprojects.myapplication.util.StringsFormattingManager.isValidCpf
import com.miguelprojects.myapplication.util.StringsFormattingManager.isValidState
import com.miguelprojects.myapplication.util.StringsFormattingManager.isValidSus
import com.miguelprojects.myapplication.util.StringsFormattingManager.isValidTelephone
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class RegisterAdressDataFragment : Fragment() {
    private lateinit var workspaceId: String
    private lateinit var citizenModel: CitizenModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var binding: FragmentRegisterCitizenAdressDataBinding
    private lateinit var citizenId: String
    private lateinit var oldCitizenModel: CitizenModel
    private var addTextEventListener: Boolean = false
    private var textHandler: Handler? = null
    private var networkChangeReceiver = NetworkChangeReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            oldCitizenModel = it.getParcelable(ARG_OLD_CITIZEN)!!
            citizenModel = it.getParcelable(ARG_CITIZEN)!!
            workspaceId = it.getString(ARG_WORKSPACE_ID)!!
            citizenId = it.getString(ARG_CITIZEN_ID)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterCitizenAdressDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        citizenViewModel = ViewModelProvider(requireActivity())[CitizenViewModel::class.java]
        workspaceViewModel = ViewModelProvider(requireActivity())[WorkspaceViewModel::class.java]

        startInitialFeatures()

        updateWorkspaceValues()
    }

    override fun onResume() {
        super.onResume()

        println("CitizenId = $citizenId")

        initTextEventListener()
    }

    private fun updateWorkspaceValues() {
        binding.editBirthplace.setText(citizenModel.birthplace)
        binding.editCep.setText(citizenModel.cep)
        binding.editState.setText(citizenModel.state)
        binding.editCity.setText(citizenModel.city)
        binding.editNeighborhood.setText(citizenModel.neighborhood)
        binding.editStreet.setText(citizenModel.street)
        binding.editHousenumber.setText(citizenModel.numberhouse.toString())
        binding.editAddons.setText(citizenModel.addons)

        Log.d(
            "Update Workspace - Personal data",
            "Dados | birthplace: ${citizenModel.birthplace}, cep: ${citizenModel.cep}, state: ${citizenModel.state}, city: ${citizenModel.city}, neighborhood: ${citizenModel.neighborhood}, street: ${citizenModel.street}, numberhouse: ${citizenModel.numberhouse}, addons: ${citizenModel.addons}"
        )
    }

    private fun setValuesInCitizenModel() {
        citizenModel.birthplace = binding.editBirthplace.text.toString()
        citizenModel.cep = binding.editCep.text.toString()
        citizenModel.state = binding.editState.text.toString()
        citizenModel.city = binding.editCity.text.toString()
        citizenModel.neighborhood = binding.editNeighborhood.text.toString()
        citizenModel.street = binding.editStreet.text.toString()
        citizenModel.addons = binding.editAddons.text.toString()
    }

    private fun startInitialFeatures() {
        binding.buttonBack.setOnClickListener {
            setValuesInCitizenModel()

            val houseNumberText = binding.editHousenumber.text.toString()
            if (houseNumberText.isNotEmpty()) {
                citizenModel.numberhouse = houseNumberText.toInt()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Número da casa não pode estar vazio",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            loadFragment(
                RegisterPersonalDataFragment.newInstance(
                    oldCitizenModel,
                    citizenModel,
                    workspaceId,
                    citizenId,
                )
            )
        }

        if (citizenModel.id.isEmpty()) {
            addNewCitizen()
        } else {
            editCitizen()
        }
    }

    private fun initTextEventListener() {
        if (!addTextEventListener && isAdded) {
            citizenModel.let {
                addEditTextListener(binding.editBirthplace, binding.buttonSave, oldCitizenModel.birthplace)
                addEditTextListener(binding.editCep, binding.buttonSave, oldCitizenModel.cep)
                addEditTextListener(binding.editState, binding.buttonSave, oldCitizenModel.state)
                addEditTextListener(binding.editCity, binding.buttonSave, oldCitizenModel.city)
                addEditTextListener(binding.editNeighborhood, binding.buttonSave, oldCitizenModel.neighborhood)
                addEditTextListener(binding.editStreet, binding.buttonSave, oldCitizenModel.street)
                addEditTextListener(
                    binding.editHousenumber,
                    binding.buttonSave,
                    it.numberhouse.toString()
                )
                addEditTextListener(binding.editAddons, binding.buttonSave, oldCitizenModel.addons ?: "")

                if (citizenId.isEmpty()) {
                    changeStyleButton(
                        binding.buttonSave,
                        R.color.quat_caribbean_green,
                        R.color.light_gray,
                        verifyValuesCitizenNotEmpty()
                    )
                } else {
                    println(
                        "verifyValuesCitizenNotEmpty() - ${verifyValuesCitizenNotEmpty()} && verifyValuesEditCitizen() - ${verifyValuesEditCitizen()} = ${
                            verifyValuesCitizenNotEmpty() && verifyValuesEditCitizen()
                        }"
                    )
                    changeStyleButton(
                        binding.buttonSave,
                        R.color.quat_caribbean_green,
                        R.color.light_gray,
                        verifyValuesCitizenNotEmpty()
//                                && verifyValuesEditCitizen()
                    )
                }

                addTextEventListener = true
            }
        }
    }

    private fun editCitizen() {
        binding.buttonSave.text = "Editar"

        binding.buttonSave.setOnClickListener {
            setValuesInCitizenModel()
            formatNameValues()

            if (!verifyValuesCitizenNotEmpty()) {
                messageToast("Preencha os campos obrigatórios que estão vazios!")
                return@setOnClickListener
            }

            if (verifyValuesCorrectFormat()) {
                binding.buttonSave.isEnabled = false
                if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                    updateCitizenFirebase()
                } else {
                    updateCitizenRoom(!networkChangeReceiver.isNetworkConnected(requireContext()))
                }
            }
        }
    }

    private fun updateCitizenFirebase() {
        citizenViewModel.updateCitizenFirebase(citizenModel, workspaceId) { res, message ->
            if (res) {
                val needsUpdate = !networkChangeReceiver.isNetworkConnected(requireContext())
                updateCitizenRoom(needsUpdate)
            } else {
                binding.buttonSave.isEnabled = true
                messageToast(message)
            }
        }
    }

    private fun updateCitizenRoom(needsUpdate: Boolean) {
        val citizenEntity = Citizen.fromCitizenModel(citizenModel, workspaceId)
        citizenEntity.needsUpdate = needsUpdate

        citizenViewModel.updateCitizenRoom(citizenEntity) { res ->
            if (res) {
                Toast.makeText(
                    requireContext(),
                    "Atualização bem sucedida!",
                    Toast.LENGTH_SHORT
                )
                    .show()
                val resultIntent = Intent()
                resultIntent.putExtra("citizenResult", citizenModel)
                requireActivity().setResult(UPDATE_CODE, resultIntent)
                activity?.finish()
            } else {
                binding.buttonSave.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Problemas ao realizar esta ação. Reporte este erro!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun addNewCitizen() {
        binding.buttonSave.setOnClickListener {
            setValuesInCitizenModel()
            binding.buttonSave.isEnabled = false

            if (!verifyValuesCitizenNotEmpty()) {
                messageToast("Preencha os campos obrigatórios que estão vazios!")
                return@setOnClickListener
            }

            if (verifyValuesCorrectFormat()) {
                formatNameValues()

                if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                    saveCitizenOnFirebase()
                } else {
                    val citizenEntity = Citizen.fromCitizenModel(citizenModel, workspaceId)
                    citizenEntity.needsSync = true
                    saveCitizenOnRoom(citizenEntity)
                }
            }
        }
    }

    private fun verifyValuesCorrectFormat(): Boolean {
        return if (!citizenModel.cpf.isValidCpf()) {
            messageToast("CPF informado não é valido!")
            println(citizenModel.cpf)
            false
        } else if (!citizenModel.telephone.isValidTelephone()) {
            messageToast("Telefone informado não é valido!")
            println(citizenModel.telephone)
            false
        } else if (citizenModel.sus != null && citizenModel.sus!!.isNotEmpty() && !citizenModel.sus.isValidSus()) {
            messageToast("Número do Cartão SUS invalido!")
            false
        } else if (!citizenModel.birthdate.isValidBirthdate()) {
            messageToast("Data de nascimento invalida!")
            false
        } else if (!citizenModel.cep.isValidCep()) {
            messageToast("Número do CEP invalido!")
            false
        } else if (!citizenModel.state.isValidState()) {
            messageToast("Nome do estado invalido!")
            false
        } else {
            true
        }
    }

    private fun formatNameValues() {
        citizenModel.name = ConvertManager.capitalizeWords(citizenModel.name)
        citizenModel.fathername = ConvertManager.capitalizeWords(citizenModel.fathername)
        citizenModel.mothername = ConvertManager.capitalizeWords(citizenModel.mothername)
    }

    private fun messageToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun saveCitizenOnRoom(citizenEntity: Citizen) {
        citizenViewModel.saveCitizenRoom(citizenEntity) { resOffCitizenId ->
            if (resOffCitizenId.isNotEmpty()) {
                messageToast("Dados salvos com sucesso!")

                val resultIntent = Intent()
                resultIntent.putExtra("citizenResult", citizenModel)
                requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                activity?.finish()
            } else {
                binding.buttonSave.isEnabled = true
                messageToast("Problema ao salvar os dados no dispositivo. Por favor, reporte!")
                Log.d(
                    "Salvar dados cidadão",
                    "Problema ao salvar os dados no dispositivo. Por favor, reporte!"
                )
            }
        }
    }

    private fun saveCitizenOnFirebase() {
        if (workspaceId.isEmpty()) {
            println("Erro! Workspace ID está nulo!")
            binding.buttonSave.isEnabled = true
            return
        }

        citizenViewModel.saveCitizenData(workspaceId, citizenModel) { res, citizenId ->
            println(citizenId)
            if (res && citizenId.isNotEmpty()) {
                citizenModel.id = citizenId
                saveCitizenOnRoom(Citizen.fromCitizenModel(citizenModel, workspaceId))
            } else {
                binding.buttonSave.isEnabled = true
                messageToast("Erro ao executar essa ação!")
            }
        }
    }

    private fun verifyValuesCitizenNotEmpty(): Boolean {
        val houseNumberText = binding.editHousenumber.text.toString()
        if (houseNumberText.isNotEmpty()) {
            citizenModel.numberhouse = houseNumberText.toInt()
        } else {
            messageToast("Número da casa não pode estar vazio")
            return false
        }

        return !(citizenModel.name.isEmpty() ||
                citizenModel.telephone.isEmpty() || citizenModel.cpf.isEmpty() || citizenModel.fathername.isEmpty() || citizenModel.mothername.isEmpty() || citizenModel.birthplace.isEmpty() || citizenModel.cep.isEmpty() || citizenModel.neighborhood.isEmpty() || citizenModel.street.isEmpty())
    }

    private fun verifyValuesEditCitizen(): Boolean {
        val resultDif = oldCitizenModel.name != citizenModel.name ||
                oldCitizenModel.telephone != citizenModel.telephone ||
                oldCitizenModel.sex != citizenModel.sex ||
                oldCitizenModel.cpf != citizenModel.cpf ||
                oldCitizenModel.sus != citizenModel.sus ||
                oldCitizenModel.numberregister != citizenModel.numberregister ||
                oldCitizenModel.birthdate != citizenModel.birthdate ||
                oldCitizenModel.fathername != citizenModel.fathername ||
                oldCitizenModel.mothername != citizenModel.mothername ||
                oldCitizenModel.birthplace != citizenModel.birthplace ||
                oldCitizenModel.cep != citizenModel.cep ||
                oldCitizenModel.state != citizenModel.state ||
                oldCitizenModel.city != citizenModel.city ||
                oldCitizenModel.neighborhood != citizenModel.neighborhood ||
                oldCitizenModel.street != citizenModel.street ||
                oldCitizenModel.numberhouse != citizenModel.numberhouse ||
                oldCitizenModel.addons != citizenModel.addons

        println(resultDif)

        println(oldCitizenModel)
        println(citizenModel)

        return resultDif
    }

    private fun addEditTextListener(
        editText: EditText,
        buttonRef: Button,
        oldValue: String
    ) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                textHandler?.removeCallbacksAndMessages(null)
                textHandler = Handler(Looper.getMainLooper()).apply {
                    postDelayed({
                        setValuesInCitizenModel()

                        if (citizenId.isEmpty()) {
                            changeStyleButton(
                                buttonRef,
                                R.color.quat_caribbean_green,
                                R.color.light_gray,
                                verifyValuesCitizenNotEmpty()
                            )
                        } else {
                            val verifyChange = s?.toString()
                                ?.trim() != oldValue.trim() && verifyValuesCitizenNotEmpty()
//                                    && verifyValuesEditCitizen()

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
        isEnable: Boolean
    ) {
        if (isAdded) {
            button.backgroundTintList =
                ContextCompat.getColorStateList(
                    requireContext(),
                    if (isEnable) colorEnabled else colorDisabled
                )
            button.isEnabled = isEnable
        }
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    companion object {
        private const val ARG_OLD_CITIZEN = "old_citizen"
        private const val ARG_CITIZEN = "citizen"
        private const val ARG_WORKSPACE_ID = "workspace_id"
        private const val ARG_CITIZEN_ID = "citizen_id"
        private const val UPDATE_CODE = 3

        fun newInstance(
            oldCitizenModel: CitizenModel,
            citizenModel: CitizenModel,
            workspaceId: String,
            citizenId: String,
        ): RegisterAdressDataFragment {
            val fragment = RegisterAdressDataFragment()
            val args = Bundle()
            args.putParcelable(ARG_OLD_CITIZEN, oldCitizenModel)
            args.putParcelable(ARG_CITIZEN, citizenModel)
            args.putString(ARG_WORKSPACE_ID, workspaceId)
            args.putString(ARG_CITIZEN_ID, citizenId)
            fragment.arguments = args
            return fragment
        }
    }
}