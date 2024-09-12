package com.miguelprojects.myapplication.ui.fragments.user

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.FragmentUpdateAndDeleteUserBinding
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel

class UpdateAndDeleteUserFragment : Fragment() {
    private lateinit var binding: FragmentUpdateAndDeleteUserBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var userViewModel: UserViewModel
    private var userSessionManager = UserSessionManager
    private var action: String? = ""
    private var userId: String? = null
    private var newUserModel: UserModel? = null
    private var userModel: UserModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
            userModel = it.getParcelable(ARG_USER_MODEL)
            newUserModel = it.getParcelable(ARG_NEW_USER_MODEL)
            action = it.getString(ARG_ACTION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUpdateAndDeleteUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        if (userId.isNullOrEmpty()) {
            requireActivity().finish()
        }

        val editText: EditText = binding.editConfirm
        val button: Button = binding.buttonConfirm

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não é necessário implementar neste caso
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Não é necessário implementar neste caso
            }

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString().trim()
                if (text.isEmpty()) {
                    // Campo está vazio, altere a cor do botão para indicar erro
                    button.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.light_gray
                        )
                    )
                    button.isEnabled = false
                } else {
                    // Campo não está vazio, altere a cor do botão de volta ao padrão
                    button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                    button.isEnabled = true
                }
            }
        })

        binding.buttonConfirm.setOnClickListener {
            val passwordConfirm = binding.editConfirm.text.toString()
            if (userModel != null && userModel!!.id == userId && action.toString() == "update") {
//                prepareUpdateUserAccount(passwordConfirm)
            } else if (userModel != null && userModel!!.id == userId && action.toString() == "delete") {
//                prepareDeleteUserAccount(passwordConfirm)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Alguns valores estão nulos por isso não foi possivel completar a ação.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

//    fun prepareDeleteUserAccount(passwordConfirm: String) {
//        val builder = AlertDialog.Builder(requireContext())
//        builder.setTitle("Deletar conta")
//        builder.setMessage("Confirme essa ação. Deseja mesmo deletar a conta atual?")
//
//        // Adicionar botões ao AlertDialog
////        builder.setPositiveButton("Deletar") { dialog, which ->
//            // Ação de logout
////            if (!passwordConfirm.isNullOrEmpty() && !userId.isNullOrEmpty()) {
////                userSessionManager.deleteUserAccount(
////                    requireActivity() as AppCompatActivity,
////                    userViewModel,
////                    userModel!!.email,
////                    passwordConfirm
////                ) { res, message ->
////                    if (res) {
////                        userViewModel.deleteUserModel(userId!!) { res, messageBack ->
////                            if (res) {
////                                println("Processo concluido com sucesso!")
////                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
////                                val intent = requireActivity().intent
////                                startActivity(intent)
//////                        activity?.finish()
////                            } else {
////                                println("Erro no processo!")
////                                Toast.makeText(requireContext(), messageBack, Toast.LENGTH_SHORT)
////                                    .show()
////                            }
////                        }
////                    } else {
////                        println("Erro no processo!")
////                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
////                    }
////                }
////            } else {
////                Toast.makeText(
////                    requireContext(),
////                    "Não é possivel deletar a conta, alguns dados não foram informados.",
////                    Toast.LENGTH_SHORT
////                ).show()
////            }
//        }
//        builder.setNegativeButton("Não") { dialog, which ->
//            // Fechar o dialog sem fazer nada
//            dialog.dismiss()
//        }
//
//        // Mostrar o AlertDialog
//        builder.show()
//    }

    private fun loadFragment(fragment: Fragment) {
        // Carregar o fragmento fornecido no container
        val activity = activity

        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.commit()
        }
        // Ocultar o ProgressBar após a transação de fragmento
    }

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_NEW_USER_MODEL = "new_user_model"
        const val ARG_USER_MODEL = "user_model"
        const val ARG_ACTION = "action"

        fun newInstance(
            userId: String,
            userModel: UserModel,
            newUserModel: UserModel,
            action: String
        ) =
            UpdateAndDeleteUserFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putParcelable(ARG_NEW_USER_MODEL, newUserModel)
                    putParcelable(ARG_USER_MODEL, userModel)
                    putString(ARG_ACTION, action)
                }
            }
    }
}