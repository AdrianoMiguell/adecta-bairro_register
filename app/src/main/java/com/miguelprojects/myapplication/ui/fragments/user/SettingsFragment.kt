package com.miguelprojects.myapplication.ui.fragments.user

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.FragmentSettingsBinding
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.room.entity.User
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.UserSessionManager
import com.miguelprojects.myapplication.viewmodel.UserViewModel

class SettingsFragment : Fragment() {
    private lateinit var binding: FragmentSettingsBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var userModel: UserModel
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var userSessionManager = UserSessionManager
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startTools()

        if (networkChangeReceiver.isNetworkConnected(requireContext())) {
            if (userId.isNullOrEmpty()) {
                Log.d(
                    "User id, settings",
                    "Error. Dados do usuário não carregados do banco!"
                )
                requireActivity().supportFragmentManager.popBackStack()
            }

            println(userId)
            userViewModel.loadUserModel(userId!!)
            userViewModel.userModel.observe(viewLifecycleOwner, Observer { user ->
                println(user)
                if (user != null) {
                    userModel = user
                    println(userModel)
                    updateUserData()
                } else {
                    Log.d(
                        "User id, settings",
                        "Error. Dados do usuário não carregados do banco!"
                    )
                    requireActivity().supportFragmentManager.popBackStack()
                }
            })
        } else {
            userViewModel.loadUserRoom(userId!!) { user ->
                if (user != null) {
                    userModel = User.toUserModel(user)
                    println(userModel)
                    updateUserData()
                } else {
                    Log.d(
                        "User id, settings",
                        "Error. Dados do usuário não carregados do banco!"
                    )
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }

        setClickListeners()
    }

    private fun startTools() {
        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
    }

    private fun updateUserData() {
        binding.editUsername.setText(userModel.username)
        binding.editFullname.setText(userModel.fullname)
        binding.textEmail.setText(userModel.email)
    }

    private fun setClickListeners() {
        binding.buttonLogout.setOnClickListener {
            // Exibir um AlertDialog para confirmar a ação de logout
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Logout")
            builder.setMessage("Você tem certeza que deseja sair?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                binding.layoutSettings.animate()
                    .alpha(0.0f)
                    .setDuration(250)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            // Ação de logout
                            userSessionManager.onUserNotFoundOrLogout(
                                requireActivity() as AppCompatActivity,
                                userViewModel
                            )
                            // Mostrar um Toast confirmando a ação
                            Toast.makeText(
                                requireContext(),
                                "Você saiu de sua conta!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    })

            }
            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }

            // Mostrar o AlertDialog
            builder.show()
        }

        binding.buttonDeleteAccount.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Deletar conta")
            builder.setMessage("Você tem certeza que deseja deletar a conta atual?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                // Ação de logout
                loadFragment(
                    UpdateAndDeleteUserFragment.newInstance(
                        userId!!,
                        userModel,
                        UserModel(),
                        "delete"
                    ), true
                )
                // Mostrar um Toast confirmando a ação
                Toast.makeText(
                    requireContext(),
                    "Confirme a realização dessa ação!",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }

            // Mostrar o AlertDialog
            builder.show()
        }

        binding.buttonUpdateUser.setOnClickListener {
            val newUserModel = UserModel(
                userId ?: userModel.id,
                binding.editUsername.text.toString(),
                binding.editFullname.text.toString(),
                userModel.email,
            )

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Atualizar dados da conta")
            builder.setMessage("Você tem certeza que deseja atualizar a conta atual?")

            // Adicionar botões ao AlertDialog
            builder.setPositiveButton("Sim") { dialog, which ->
                updateUserAccount(newUserModel)
            }
            builder.setNegativeButton("Não") { dialog, which ->
                // Fechar o dialog sem fazer nada
                dialog.dismiss()
            }

            // Mostrar o AlertDialog
            builder.show()
        }
    }

    fun updateUserAccount(newUserModel: UserModel) {
        userSessionManager.updateUserAccount(
            requireActivity() as AppCompatActivity,
            newUserModel!!,
            userViewModel,
            userId!!,
        ) { res, message ->
            if (res) {
                println("Processo concluido com sucesso!")
                binding.layoutSettings.animate()
                    .alpha(0.0f)
                    .setDuration(250)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)

                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            val intent = requireActivity().intent
                            startActivity(intent)
                        }
                    })
            } else {
                println("Erro no processo!")
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
        // Carregar o fragmento fornecido no container
        val activity = activity

        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            if (addToBack) {
                transaction.addToBackStack(null) // Adicionar a transação ao back stack (opcional)
            }
            transaction.commit()
        }
        // Ocultar o ProgressBar após a transação de fragmento
    }

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_OFF_USER_ID = "off_user_id"

        fun newInstance(userID: String, offUserId: Long): SettingsFragment {
            val fragment = SettingsFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userID)
            args.putLong(ARG_OFF_USER_ID, offUserId)
            fragment.arguments = args
            return fragment
        }
    }
}