package com.miguelprojects.myapplication.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.databinding.FragmentHomeEmptyBinding
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.CreateEditWorkspaceActivity
import com.miguelprojects.myapplication.ui.fragments.user.JoinWorkspaceFragment
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class HomeEmptyFragment : Fragment() {
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private var _binding: FragmentHomeEmptyBinding? = null
    private val networkChangeReceiver = NetworkChangeReceiver()
    private val binding get() = _binding!!
    private var userId: String = ""
    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("Teste Workspace Recycle", "Entrou aqui no result")
                val newWorkspace = result.data?.getParcelableExtra<WorkspaceModel>("newWorkspace")
                if (newWorkspace != null && isAdded) {
                    if (!networkChangeReceiver.isNetworkConnected(requireContext())) {
                        val newList = mutableListOf<WorkspaceModel>()
                        newList.add(newWorkspace)
                        println(newList)
                        Log.d("Teste Workspace Recycle", "Entrou aqui no list")
                        workspaceViewModel.updateListWorkspaceRoom(
                            newWorkspace.toWorkspaceEntity(
                                !networkChangeReceiver.isNetworkConnected(
                                    requireContext()
                                )
                            )
                        )
                        requireActivity().supportFragmentManager.popBackStack()
                        loadFragment(
                            HomeFragment.newInstance(
                                userId,
                            ), false
                        )
                    }
                    Log.d("Teste Workspace Recycle", "O novo valor está aqui")
                } else {
                    Log.d("Teste Workspace Recycle", "O novo valor não está aqui")
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USED_ID).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        workspaceViewModel = ViewModelProvider(requireActivity())[WorkspaceViewModel::class.java]

        _binding = FragmentHomeEmptyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAddNew.setOnClickListener {
            Log.d("Teste Click", "Clicou no button participe")
            val intent = Intent(requireContext(), CreateEditWorkspaceActivity::class.java)
            intent.putExtra("userId", userId)
            result.launch(intent)
        }

        binding.buttonParticipe.setOnClickListener {
            if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                loadFragment(JoinWorkspaceFragment.newInstance(userId), true)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Essa ação precisa de conexão com a rede.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
        val activity = activity

        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.fragment_container, fragment)
            if (addToBack) {
                fragmentTransaction.addToBackStack(null)
            }
            fragmentTransaction.commit()
        }
    }

    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment HomeEmptyFragment.
//         */

        private const val ARG_USED_ID = "user_id"

        fun newInstance(userId: String): HomeEmptyFragment {
            val fragment = HomeEmptyFragment()
            val args = Bundle()
            args.putString(ARG_USED_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }
}