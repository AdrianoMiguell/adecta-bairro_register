package com.miguelprojects.myapplication.ui.fragments.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.databinding.FragmentJoinWorkspaceBinding
import com.miguelprojects.myapplication.factory.WorkspaceRequestViewModelFactory
import com.miguelprojects.myapplication.repository.WorkspaceRequestRepository
import com.miguelprojects.myapplication.viewmodel.WorkspaceRequestViewModel

class JoinWorkspaceFragment : Fragment() {
    private lateinit var binding: FragmentJoinWorkspaceBinding
    private lateinit var workspaceRequestViewModel: WorkspaceRequestViewModel
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentJoinWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        workspaceRequestViewModel = ViewModelProvider(
            this,
            WorkspaceRequestViewModelFactory(WorkspaceRequestRepository())
        )[WorkspaceRequestViewModel::class.java]
        setClickListeners()
    }

    private fun setClickListeners() {
        binding.buttonJoin.setOnClickListener {
//            logica para verificar e mandar pedido ao admin para participar do grupo
            val workspaceCode = binding.editCodeJoin.text.toString()

            workspaceRequestViewModel.sendRequestJoin(workspaceCode, userId!!) { res, message ->
                if (res) {
                    fragmentManager?.popBackStack()
                } else {
                    binding.editCodeJoin.setText("")
                    println("Problema no processo de envio de solicitação para entrar em grupo.")
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val USER_ID = "user_id"

        fun newInstance(userId: String) =
            JoinWorkspaceFragment().apply {
                arguments = Bundle().apply {
                    putString(USER_ID, userId)
                }
            }
    }
}