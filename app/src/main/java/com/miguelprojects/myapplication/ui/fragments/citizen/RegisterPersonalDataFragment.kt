package com.miguelprojects.myapplication.ui.fragments.citizen

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.FragmentRegisterCitizenPersonalDataBinding
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RegisterPersonalDataFragment : Fragment() {
    private lateinit var binding: FragmentRegisterCitizenPersonalDataBinding
    private lateinit var workspaceId: String
    private lateinit var citizenId: String
    private lateinit var citizenModel: CitizenModel
    private lateinit var oldCitizenModel: CitizenModel
    private var valueEditSex: String = ""
    private var networkChangeReceiver = NetworkChangeReceiver()
    private val convertManager = ConvertManager

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
        binding = FragmentRegisterCitizenPersonalDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClickListeners()

        prepareValueSpinner()

        updateWorkspaceValues()

        println("citizenId = $citizenId")
    }

    private fun prepareValueSpinner() {
        val mapSex = mapOf(
            "m" to "Masculino",
            "f" to "Feminino",
            "" to "Prefiro não dizer",
        )

        val adapterSex =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                mapSex.values.toList()
            )
        adapterSex.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.editSex.adapter = adapterSex

        binding.editSex.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                valueEditSex = mapSex.keys.elementAt(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }

        if (citizenModel.sex.isNotEmpty()) {
            val spinnerPosition = mapSex.keys.toList().indexOf(citizenModel.sex)
            if (spinnerPosition != -1) {
                valueEditSex = citizenModel.sex
                binding.editSex.setSelection(spinnerPosition)
            }
        } else {
            valueEditSex = citizenModel.sex
            binding.editSex.setSelection(2)
        }
    }

    private fun updateWorkspaceValues() {
        binding.editName.setText(citizenModel.name)
        binding.editPhone.setText(citizenModel.telephone)
        binding.editBirthdate.setText(convertManager.convertLongForString(citizenModel.birthdate))
        binding.editCpf.setText(citizenModel.cpf)
        binding.editSus.setText(citizenModel.sus)
        binding.editFatherName.setText(citizenModel.fathername)
        binding.editMotherName.setText(citizenModel.mothername)
    }

    private fun setOnClickListeners() {
        binding.buttonNextFragment.setOnClickListener {
            citizenModel.name = binding.editName.text.toString()
            citizenModel.telephone = binding.editPhone.text.toString()
            citizenModel.sex = valueEditSex
            citizenModel.birthdate =
                convertManager.convertStringForDate(binding.editBirthdate.text.toString())
            citizenModel.cpf = binding.editCpf.text.toString()
            citizenModel.sus = binding.editSus.text.toString()
            citizenModel.fathername = binding.editFatherName.text.toString()
            citizenModel.mothername = binding.editMotherName.text.toString()

            Log.d("Next Fragment", "Valores atualizados e mandados para o proximo fragment")

            loadFragment(
                RegisterAdressDataFragment.newInstance(
                    oldCitizenModel,
                    citizenModel,
                    workspaceId,
                    citizenId,
                )
            )
        }

        binding.editBirthdate.setOnClickListener {
            showDatePickerDialog(binding.editBirthdate, citizenModel)
        }
    }

    private fun showDatePickerDialog(editText: EditText, citizenModel: CitizenModel?) {
        // Verifique se o fragmento está anexado
        if (!isAdded) return // Retorna se o fragmento não estiver anexado

        try {
            val calendar = Calendar.getInstance()

            citizenModel?.birthdate?.let {
                if (it != 0L) {
                    try {
                        // Converte o timestamp (em milissegundos) diretamente em um objeto Date
                        val birthDate = Date(it)
                        calendar.time = birthDate // Define a data do calendário como a data do birthdate
                    } catch (e: Exception) {
                        Log.d("Calender Dialog", "Erro ao selecionar data. ${e.message}")
                    }
                }
            }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(selectedYear, selectedMonth, selectedDay)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val dateString = dateFormat.format(selectedDate.time)
                    editText.setText(dateString)
                },
                year,
                month,
                day
            )

            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Erro ao abrir Seletor de Data. Por favor, Reporte esse problema!",
                Toast.LENGTH_SHORT
            ).show()
            println("Erro: ${e.message}")
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

        fun newInstance(
            oldCitizenModel: CitizenModel,
            citizen: CitizenModel,
            workspaceId: String,
            citizenId: String,
        ): RegisterPersonalDataFragment {
            val fragment = RegisterPersonalDataFragment()
            val args = Bundle()
            args.putParcelable(ARG_OLD_CITIZEN, oldCitizenModel)
            args.putParcelable(ARG_CITIZEN, citizen)
            args.putString(ARG_WORKSPACE_ID, workspaceId)
            args.putString(ARG_CITIZEN_ID, citizenId)
            fragment.arguments = args
            return fragment
        }
    }
}