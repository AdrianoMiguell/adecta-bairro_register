package com.miguelprojects.myapplication.ui.fragments

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.WorkspaceListAdapter
import com.miguelprojects.myapplication.adapter.decoration.MarginItemDecoration
import com.miguelprojects.myapplication.adapter.listener.WorkspaceOnClickListener
import com.miguelprojects.myapplication.databinding.FragmentHomeBinding
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.CreateEditWorkspaceActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.JoinWorkspaceActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.WorkspaceMainActivity
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.WorkManagerUtil
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class HomeFragment : Fragment() {
    private lateinit var adapter: WorkspaceListAdapter
    private lateinit var binding: FragmentHomeBinding
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var uiUpdateReceiver: BroadcastReceiver
    private var workspaceList: List<WorkspaceModel> = emptyList()
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var dataSynchronized = false
    private var initWorkspaceAccessObserver = false
    private var initWorkspaceModelObserver = false
    private var userId: String = ""
    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (isAdded && !networkChangeReceiver.isNetworkConnected(requireContext())) {
                val workspaceResult =
                    result.data?.getParcelableExtra<WorkspaceModel>("workspaceResult")

                when (result.resultCode) {
                    Activity.RESULT_OK, UPDATE_CODE -> {
                        println("NO Activity.RESULT_OK, UPDATE_CODE")
                        workspaceViewModel.getWorkspacesWithAccess(userId)
                    }

                    DELETE_CODE -> {
                        println("NO Activity.RESULT_OK, UPDATE_CODE")
                        if (workspaceResult != null && ::adapter.isInitialized) {
                            adapter.removeItemById(workspaceResult.id)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID) ?: ""
        }

        uiUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "DATA_SYNCHRONIZED" -> {
                        progressBarLayoutManager(true)
                        dataSynchronized = true
                        if (isAdded) {
                            println("Execultando DATA_SYNCHRONIZED apos verificação de isAdded")
                            WorkManagerUtil.scheduleWorkspaceSync(requireContext(), userId)
                            loadWorkspace()
                        }
                    }

//                    "DATA_OFF_SYNCHRONIZED" -> {
//                        progressBarLayoutManager(true)
//                        if (isAdded) {
//                            println("Execultando DATA_OFF_SYNCHRONIZED apos verificação de isAdded")
//                            loadWorkspace()
//                        }
//                    }

                    else -> {
                        println("broadcast inesperado!")
                    }
                }
            }
        }


        val intentFilter = IntentFilter().apply {
            addAction("DATA_SYNCHRONIZED")
            addAction("DATA_OFF_SYNCHRONIZED")
            addAction("DATA_SYNCHRONIZED_USER")
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(uiUpdateReceiver, intentFilter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        // Configurar o adaptador aqui, se necessário
        // placeAdapterUserWorkspace()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(uiUpdateReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            userId = savedInstanceState.getString("userId").toString()
        }

        progressBarLayoutManager(true)

        startTools()

        setClickListeners()
    }

    override fun onResume() {
        super.onResume()

        loadWorkspace()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("userId", userId)
    }


    private fun startTools() {
        workspaceViewModel =
            ViewModelProvider(requireActivity())[WorkspaceViewModel::class.java]

    }

    private fun workspacesWithAccessObserver() {
        if (!initWorkspaceAccessObserver) {
            workspaceViewModel.workspacesWithAccess.observe(viewLifecycleOwner,
                Observer { listWorkspaces ->
                    Log.d(
                        "ObserverCheck Model",
                        "Acessando observer - Tamanho da lista: ${listWorkspaces.size}"
                    )

                    workspaceList =
                        WorkspaceModel.fromWorkspaceWithAccessList(listWorkspaces)

                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded) {
                            if (::adapter.isInitialized) {
                                println("atualizando o recycleview")
                                adapter.submitList(workspaceList)
                            } else {
                                println("Criando o recycleview")
                                createRecycleview()
                            }
                            progressBarLayoutManager(false)
                        }
                    }, 250)
                })

            initWorkspaceAccessObserver = true
        }
    }

    private fun workspaceListModelObserver() {
        if (!initWorkspaceModelObserver) {
            workspaceViewModel.workspaceListModel.observe(viewLifecycleOwner, Observer { list ->
                Log.d(
                    "workspaceListModelObserver",
                    "Acessando observer - Tamanho da lista: ${list.size}"
                )

                workspaceList = list

                Handler(Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        if (::adapter.isInitialized) {
                            println("atualizando o recycleview")
                            adapter.submitList(workspaceList)
                        } else {
                            println("Criando o recycleview")
                            createRecycleview()
                        }
                        progressBarLayoutManager(false)
                    }
                }, 250)
            })

            initWorkspaceModelObserver = true
        }
    }

    private fun loadWorkspace() {
        println("no loadWorkspace")

        // Configurar os observers primeiro
        workspacesWithAccessObserver()
        workspaceListModelObserver()

        if (networkChangeReceiver.isNetworkConnected(requireActivity())) {
            println("Na verificação de conexão para carregar workspaces")
            workspaceViewModel.loadListFirebaseListener(userId)
            workspaceViewModel.loadListData(userId)
        } else {
            workspaceViewModel.cancelFirebaseListener()
            workspaceViewModel.getWorkspacesWithAccess(userId)
        }
    }


    private fun progressBarLayoutManager(progressBarView: Boolean) {
        binding.progressBar.visibility = if (progressBarView) View.VISIBLE else View.GONE

        binding.layoutHomeEmpty.visibility =
            if (workspaceList.isEmpty() && !progressBarView) View.VISIBLE else View.GONE
        binding.layoutHomeRecycleview.visibility =
            if (workspaceList.isNotEmpty() && !progressBarView) View.VISIBLE else View.GONE
    }

    private fun createRecycleview() {
        binding.recycleviewWorkspace.layoutManager = LinearLayoutManager(context)
        adapter = WorkspaceListAdapter(workspaceList, WorkspaceOnClickListener { workspace ->
            if (isAdded) {
                val intent = Intent(requireContext(), WorkspaceMainActivity::class.java)
                intent.putExtra("workspaceId", workspace.id)
                intent.putExtra("userId", userId)
                startActivity(intent)
            }
        })

        val marginTop =
            resources.getDimensionPixelSize(R.dimen.margin_top_item_recycleview) // Defina sua dimensão
        val marginBottom =
            resources.getDimensionPixelSize(R.dimen.margin_bottom_item_recycleview) // Defina sua dimensão

        val itemDecoration = MarginItemDecoration(requireContext(), marginTop, marginBottom)
        binding.recycleviewWorkspace.addItemDecoration(itemDecoration)

        binding.recycleviewWorkspace.adapter = adapter
    }

    private fun setClickListeners() {
        binding.buttonAdd.setOnClickListener {
            if (isAdded) {
                val intent = Intent(requireContext(), CreateEditWorkspaceActivity::class.java)
                intent.putExtra("userId", userId)
                result.launch(intent)
            }
        }

        binding.buttonAddNew.setOnClickListener {
            Log.d("Teste Click", "Clicou no button participe")
            val intent = Intent(requireContext(), CreateEditWorkspaceActivity::class.java)
            intent.putExtra("userId", userId)
            result.launch(intent)
        }

        binding.buttonParticipe.setOnClickListener {
            if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                if (isAdded) {
                    val intent = Intent(requireContext(), JoinWorkspaceActivity::class.java)
                    intent.apply {
                        putExtra("userId", userId)
                    }
                    startActivity(intent)
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Essa ação precisa de conexão com a rede.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val ARG_USER_ID = "user_id"

        private const val DELETE_CODE = 2
        private const val UPDATE_CODE = 3

        fun newInstance(
            userId: String,
        ): HomeFragment {
            val fragment = HomeFragment()
            val args = Bundle()
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }
}
