package com.miguelprojects.myapplication.ui.fragments.workspace

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.miguelprojects.myapplication.MyApplication
import com.miguelprojects.myapplication.R
import com.miguelprojects.myapplication.adapter.CitizenListAdapter
import com.miguelprojects.myapplication.adapter.listener.CitizenOnClickListener
import com.miguelprojects.myapplication.databinding.FragmentWorkspaceMainBinding
import com.miguelprojects.myapplication.factory.CitizenViewModelFactory
import com.miguelprojects.myapplication.model.CitizenModel
import com.miguelprojects.myapplication.model.UserModel
import com.miguelprojects.myapplication.model.WorkspaceModel
import com.miguelprojects.myapplication.repository.CitizenRepository
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.CitizenDetailActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.CreateEditWorkspaceActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.DeleteCitizensActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.NotificationIncomingOrdersActivity
import com.miguelprojects.myapplication.ui.activitys.activity_workspace.RegisterCitizenActivity
import com.miguelprojects.myapplication.util.ConvertManager
import com.miguelprojects.myapplication.util.NetworkChangeReceiver
import com.miguelprojects.myapplication.util.SpinnerOptions
import com.miguelprojects.myapplication.util.WorkManagerUtil
import com.miguelprojects.myapplication.viewmodel.CitizenViewModel
import com.miguelprojects.myapplication.viewmodel.UserViewModel
import com.miguelprojects.myapplication.viewmodel.WorkspaceViewModel

class WorkspaceMainFragment : Fragment() {
    private lateinit var binding: FragmentWorkspaceMainBinding
    private lateinit var userViewModel: UserViewModel
    private lateinit var citizenViewModel: CitizenViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var adapter: CitizenListAdapter
    private lateinit var uiUpdateReceiver: BroadcastReceiver
    private var needsWorkspaceIdSynchronized = false
    private var searchHandler: Handler? = null
    private var isFirstSelectionAge = true
    private var isFirstSelectionSearch = true
    private var isFirstSelectionLimit = true
    private var isFirstSelectionSort = true
    private var isFirstSelectionSex = true
    private var limitValue: Int = 50
    private var orderAlphabet: Boolean = true
    private var ageCategory: Int = 0
    private var sexCategory: String = "t"
    private var searchCategory: String = "name"
    private var searchValue: String = ""
    private val networkChangeReceiver = NetworkChangeReceiver()
    private var citizenList: List<CitizenModel> = emptyList()
    private var workspaceModel = WorkspaceModel()
    private var userId: String = ""
    private var workspaceId: String = ""
    private var isSynchronizing: Boolean = false
    private var userModel = UserModel()
    private val filtersCitizenList = mutableMapOf<String, Any>(
        "searchValue" to searchValue,
        "sexCategory" to sexCategory,
        "limitValue" to limitValue,
        "searchCategory" to searchCategory,
        "ageCategory" to ageCategory,
        "orderAlphabet" to orderAlphabet
    )

    private val result =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val citizenResult = result.data?.getParcelableExtra<CitizenModel>("citizenResult")
            toggleVisibleProgressBar(true)

            if (citizenResult != null) {
                when (result.resultCode) {
                    Activity.RESULT_OK -> {}

                    INACTIVE_CODE -> {
                        adapter.removeItemById(citizenResult.id)
                    }

                    UPDATE_CODE -> {}

                    DELETE_CODE -> {}
                }
            }

            if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                println("Está no result")
                if (searchValue.isNotEmpty() || ageCategory != 0 || sexCategory != "t" || searchCategory != "name") {
                    actionSearchListFirebase()
                } else {
                    loadListCitizensFirebase()
                }
            } else {
                println("Está no result off")
                if (searchValue.isNotEmpty() || ageCategory != 0 || sexCategory != "t" || searchCategory != "name") {
                    actionSearchListRoom()
                } else {
                    loadListCitizensRoom()
                }
            }

            Handler(Looper.getMainLooper()).postDelayed(
                {
                    toggleVisibleProgressBar(false)
                }, 500
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            workspaceId = it.getString(ARG_WORKSPACE_ID) ?: ""
            userId = it.getString(ARG_USER_ID) ?: ""
            userModel = it.getParcelable(ARG_USER) ?: UserModel()
        }

        uiUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "DATA_SYNCHRONIZED" -> {
                        println(workspaceId)
                        toggleVisibleProgressBar(true)

                        if (!isSynchronizing) {
                            isSynchronizing = true
                            WorkManagerUtil.scheduleCitizenSync(
                                requireContext(),
                                userId,
                                workspaceId
                            )
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            verifyIdsData()
                            initializeWorkspace()
                        }, 250)

//                        Handler(Looper.getMainLooper()).postDelayed({
//                            toggleVisibleProgressBar(false)
//                        }, 1000)
                        println("Dados atualizados após sincronização - DATA_SYNCHRONIZED")
                    }

                    "DATA_OFF_SYNCHRONIZED" -> {
                        isSynchronizing = false
                        citizenViewModel.cancelListFirebaseListener(workspaceId)
                        initializeWorkspace() // Atualiza a UI ou dados após a falta de internet
                        println("Dados atualizados para modo off")
                    }

                    "DATA_SYNCHRONIZED_USER" -> {
                        val resUserId = intent.getStringExtra("userId") ?: ""
                        if (userId.isEmpty() && resUserId.isNotEmpty()) {
                            userId = resUserId
                            println(resUserId)
                            println("Dados atualizados de user após retomada de internet")
                        }
                    }

                    "DATA_SYNCHRONIZED_WORKSPACE" -> {
//                        val resWorkspaceId = intent.getStringExtra("workspaceId") ?: ""
//                        if (workspaceId.isEmpty() && resWorkspaceId.isNotEmpty()) {
//                            workspaceId = resWorkspaceId
//                            println(resWorkspaceId)
//                            println("Dados atualizados de workspace após retomada de internet")
//                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction("DATA_SYNCHRONIZED")
            addAction("DATA_OFF_SYNCHRONIZED")
            addAction("DATA_SYNCHRONIZED_USER")
            addAction("DATA_SYNCHRONIZED_WORKSPACE")
        }

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(uiUpdateReceiver, intentFilter)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(uiUpdateReceiver)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWorkspaceMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("userId", userId)
        outState.putString("workspaceId", workspaceId)
        outState.putParcelable("userModel", userModel)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toggleVisibleProgressBar(true)

        if (savedInstanceState != null) {
            userId = savedInstanceState.getString("userId") ?: ""
            workspaceId = savedInstanceState.getString("workspaceId") ?: ""
            userModel = savedInstanceState.getParcelable("userModel") ?: UserModel()
        }

        startTools()

        initializeWorkspace()

        println(workspaceId)

        setOnClickListeners()

        configureSpinners()
    }

    private fun configureSpinners() {
        val options = SpinnerOptions()

        val adapterAge = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_light,
            options.mapCategoryAge.values.toList()
        )
        val adapterSex = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_light,
            options.mapCategorySex.values.toList()
        )
        val adapterSearch = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            options.mapCaregorySearch.values.toList()
        )

        val adapterLimit = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_light,
            options.mapOfLimits.values.toList()
        )
        val adapterOrder = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_light,
            options.mapOrderAlphabetic.values.toList()
        )

        adapterAge.setDropDownViewResource(R.layout.spinner_dropdown_item)
        adapterSex.setDropDownViewResource(R.layout.spinner_dropdown_item)
        adapterSearch.setDropDownViewResource(R.layout.spinner_dropdown_item)
        adapterLimit.setDropDownViewResource(R.layout.spinner_dropdown_item)
        adapterOrder.setDropDownViewResource(R.layout.spinner_dropdown_item)

        // Configure os Spinners com os adapters
        binding.buttonCategoryAge.adapter = adapterAge
        binding.buttonCategorySex.adapter = adapterSex
        binding.buttonCategorySearch.adapter = adapterSearch
        binding.buttonCategoryLimit.adapter = adapterLimit
        binding.buttonCategorySortAlphabetically.adapter = adapterOrder

        // Configure os listeners dos Spinners (como na sua função original)
        configureSpinnerListeners(options)
    }

    private fun configureSpinnerListeners(options: SpinnerOptions) {
        binding.buttonCategoryAge.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isFirstSelectionAge) {
                        isFirstSelectionAge = false
                        return
                    }

                    ageCategory = options.mapCategoryAge.keys.elementAt(position)
                    if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                        actionSearchListFirebase()
                    } else {
                        actionSearchListRoom()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {}
            }

        binding.buttonCategorySex.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isFirstSelectionSex) {
                        isFirstSelectionSex =
                            false // Desativa a flag após a primeira seleção
                        return // Não executa a ação no primeiro clique
                    }

                    sexCategory = options.mapCategorySex.keys.elementAt(position)

                    if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                        actionSearchListFirebase()
                    } else {
                        actionSearchListRoom()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }

        binding.buttonCategorySearch.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isFirstSelectionSearch) {
                        isFirstSelectionSearch =
                            false // Desativa a flag após a primeira seleção
                        return // Não executa a ação no primeiro clique
                    }

                    val selectCategorySearch = options.mapCaregorySearch.keys.elementAt(position)
                    searchCategory = selectCategorySearch
                    if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                        actionSearchListFirebase()
                    } else {
                        actionSearchListRoom()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }


        binding.buttonCategoryLimit.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isFirstSelectionLimit) {
                        isFirstSelectionLimit =
                            false // Desativa a flag após a primeira seleção
                        return // Não executa a ação no primeiro clique
                    }

                    val selectCategoryLimit = options.mapOfLimits.keys.elementAt(position)
                    limitValue = selectCategoryLimit

                    if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                        actionSearchListFirebase()
                    } else {
                        actionSearchListRoom()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }

        binding.buttonCategorySortAlphabetically.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (isFirstSelectionSort) {
                        isFirstSelectionSort =
                            false // Desativa a flag após a primeira seleção
                        return // Não executa a ação no primeiro clique
                    }

                    val selectCategoryOrder = options.mapOrderAlphabetic.keys.elementAt(position)
                    orderAlphabet = selectCategoryOrder

                    if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                        actionSearchListFirebase()
                    } else {
                        actionSearchListRoom()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(resSearchValue: Editable?) {
                searchHandler?.removeCallbacksAndMessages(null)

                searchHandler = Handler(Looper.getMainLooper()).apply {
                    postDelayed({
                        searchValue = resSearchValue.toString()
                        if (networkChangeReceiver.isNetworkConnected(requireContext())) {
                            actionSearchListFirebase()
                        } else {
                            actionSearchListRoom()
                        }
                    }, 500)
                }
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })
    }

    private fun verifyIdsData() {
        if (userId.isEmpty() || workspaceId.isEmpty()) {
            Log.d("Workspace load", "id do workspace não encontrado e nulo")
        } else {
            return
        }

        errorInSystem()
    }

    private fun errorInSystem() {
        Toast.makeText(
            requireContext(),
            "Erro ao carregar os dados. Reporte esse problema.",
            Toast.LENGTH_SHORT
        ).show()
        requireActivity().supportFragmentManager.popBackStack()
        requireActivity().finish()
    }

    private fun startTools() {
        verifyIdsData()

        userViewModel = ViewModelProvider(requireActivity())[UserViewModel::class.java]
        citizenViewModel = ViewModelProvider(
            requireActivity(),
            getCitizenViewModelFactory()
        )[CitizenViewModel::class.java]
        workspaceViewModel =
            ViewModelProvider(requireActivity())[WorkspaceViewModel::class.java]
    }

    private fun getCitizenViewModelFactory(): ViewModelProvider.Factory {
        val citizenDao = (requireActivity().application as MyApplication).database.citizenDao()
        val citizenRepository = CitizenRepository(citizenDao)
        return CitizenViewModelFactory(citizenRepository)
    }

    private fun initializeWorkspace() {
        toggleVisibleProgressBar(true)

        if (networkChangeReceiver.isNetworkConnected(requireContext())) {
            if (needsWorkspaceIdSynchronized) {
                citizenViewModel.getWorkspaceIdSynchronized { success, id ->
                    if (success && id.isNotEmpty()) {
                        workspaceId = id
                        needsWorkspaceIdSynchronized = false

                        println("needsWorkspaceIdSynchronized = false")

                        if (!isSynchronizing) {
                            isSynchronizing = true
                            WorkManagerUtil.scheduleCitizenSync(
                                requireContext(),
                                userId,
                                workspaceId
                            )
                        }

                        citizenViewModel.startListFirebaseListener(workspaceId)
                        workspaceModelObserver()
                        workspaceViewModel.loadData(workspaceId)
                    } else {
                        // Lidar com o erro se o workspaceId não for encontrado
                        errorInSystem()
                    }
                }
            } else {
                citizenViewModel.startListFirebaseListener(workspaceId)
                workspaceModelObserver()
                workspaceViewModel.loadData(workspaceId)
            }
        } else {
            workspaceRoomObserver()
        }
    }

    private fun workspaceModelObserver() {
        workspaceViewModel.workspaceModel.observe(
            viewLifecycleOwner,
            Observer { workspace ->
                if (workspace != null) {
                    workspaceModel = workspace
                    updateUIWorkspace()
                    if (searchValue.isNotEmpty() || ageCategory != 0 || sexCategory != "t" || searchCategory != "name") {
                        actionSearchListFirebase()
                    } else {
                        loadListCitizensFirebase()
                    }

                    println(" É o criador do workspace? ${workspace.creator} != ou = $userId ")
                    startCreatorFeatures(workspace.public)
                } else {
                    Log.d("Workspace load", "workspace não encontrado e nulo")
                    errorInSystem()
                }

                workspaceViewModel.workspaceModel.removeObservers(viewLifecycleOwner)
            })
    }

    private fun workspaceRoomObserver() {
        workspaceViewModel.loadDataRoom(workspaceId) { workspace ->
            println(workspace)
            if (workspace != null) {
                if (workspace.needsSync && workspace.firebaseId.isNullOrEmpty()) {
//                    needsWorkspaceIdSynchronized = true
                    println("needsWorkspaceIdSynchronized = true")
                }

                workspaceModel = WorkspaceModel.fromEntity(workspace)
                updateUIWorkspace()
                startCreatorFeatures(workspace.public)
                if (searchValue.isNotEmpty() || ageCategory != 0 || sexCategory != "t" || searchCategory != "name") {
                    actionSearchListRoom()
                } else {
                    loadListCitizensRoom()
                }
            } else {
                println(workspaceId)
                Log.d("Dados off", "Dados nulos do workspace off")
                errorInSystem()
            }
        }
    }

    private fun updateUIWorkspace() {
        val titleWorkspace = workspaceModel.name
        println(ConvertManager.capitalizeWords(titleWorkspace))
        binding.textTitleWorkspace.text = ConvertManager.capitalizeWords(titleWorkspace)
        binding.textTitleWorkspaceSecond.text = ConvertManager.capitalizeWords(titleWorkspace)
    }

    private fun setOnClickListeners() {
        binding.buttonSearch.setOnClickListener {
            actionSearchListRoom()
        }

        binding.buttonFilters.setOnClickListener {
            if (binding.layoutEditCategory.visibility == View.GONE) {
                binding.layoutEditCategory.visibility = View.VISIBLE
            } else {
                binding.layoutEditCategory.visibility = View.GONE
            }
        }

        binding.textTitleWorkspace.setOnClickListener {
            onClickButtonInformation()
        }
        binding.imageInitialWorkspace.setOnClickListener {
            onClickButtonInformation()
        }
        binding.textTitleWorkspaceSecond.setOnClickListener {
            onClickButtonInformation()
        }
        binding.imageInitialWorkspaceSecond.setOnClickListener {
            onClickButtonInformation()
        }
        binding.buttonListDelete.setOnClickListener {
            onClickButtonDelete()
        }
        binding.buttonListDeleteSecond.setOnClickListener {
            onClickButtonDelete()
        }
        binding.buttonAdd.setOnClickListener {
            openIntentAddNewRegister()
        }
        binding.buttonAddSecond.setOnClickListener {
            openIntentAddNewRegister()
        }
    }

    private fun onClickButtonInformation() {
        val intent = Intent(requireActivity(), CreateEditWorkspaceActivity::class.java)
        intent.putExtra("workspaceId", workspaceId)
        intent.putExtra("userId", userId)
        startActivity(intent)
    }

    private fun onClickButtonDelete() {
        val intent = Intent(requireActivity(), DeleteCitizensActivity::class.java)
        intent.putExtra("workspaceId", workspaceId)
        intent.putExtra("userId", userId)
        result.launch(intent)
    }

    private fun openIntentAddNewRegister() {
        val intent = Intent(requireContext(), RegisterCitizenActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("workspaceId", workspaceId)
        intent.putExtra("workspaceModel", workspaceModel)
        result.launch(intent)
    }

    private fun startCreatorFeatures(isPublic: Boolean) {
        setupViewIncomingOrders(
            binding.buttonViewIncomingOrdersSecond,
            isPublic
        )

        setupViewIncomingOrders(
            binding.buttonViewIncomingOrders,
            isPublic
        )
    }

    private fun setupViewIncomingOrders(
        view: View,
        isPublic: Boolean
    ) {
        when (view) {
            is Button -> {
                view.isEnabled = true
            }

            else -> {
                view.isClickable = true
            }
        }

        view.visibility = if (isPublic) View.VISIBLE else View.GONE

        view.setOnClickListener {
            if (isAdded) {
                if (isPublic) {
                    val intent =
                        Intent(requireActivity(), NotificationIncomingOrdersActivity::class.java)
                    intent.putExtra("userId", userId)
                    intent.putExtra("workspaceId", workspaceId)
                    intent.putExtra("workspaceCode", workspaceModel.inviteCode)
                    intent.putExtra("workspaceCreator", workspaceModel.creator)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        context,
                        "Este grupo é privado. Para acessar a funcionalidade, o grupo deve ser público.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun updateValuesInListOptions() {
        filtersCitizenList["searchValue"] = searchValue
        filtersCitizenList["sexCategory"] = sexCategory
        filtersCitizenList["limitValue"] = limitValue
        filtersCitizenList["searchCategory"] = searchCategory
        filtersCitizenList["ageCategory"] = ageCategory
        filtersCitizenList["orderAlphabet"] = orderAlphabet

    }

    private fun updateUIAfterSearch() {
        binding.layoutTextSearching.visibility = View.VISIBLE

        if (searchValue.isNotEmpty() && binding.editSearch.text.toString()
                .isEmpty()
        ) {
            binding.editSearch.setText(searchValue)
        }

        val textResult = citizenList.let {
            if (it.isEmpty()) {
                "Nenhum resultado encontrado"
            } else if (searchValue.isEmpty()) {
                binding.layoutTextSearching.visibility = View.GONE
                ""
            } else if (it.size > 1) {
                "${it.size} resultados encontrados"
            } else {
                "1 Resultado encontrado"
            }
        }

        binding.textTotalSearching.text = textResult

        if (::adapter.isInitialized) {
            adapter.submitList(citizenList)
        } else {
            initializeRecyclerView()
        }
    }

    private fun actionSearchListFirebase() {
        println("actionSearchListFirebase : option de search : ${filtersCitizenList["searchCategory"]}")
        citizenViewModel.sizeListCitizens(workspaceId, limitValue) { size ->
            toggleLayout(size > 0)
        }

        updateValuesInListOptions()

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                if (searchCategory.isNotEmpty()) {
                    citizenViewModel.searchCitizenByFieldFirebase(
                        workspaceId,
                        filtersCitizenList
                    ) { res, list ->
                        if (res) {
                            citizenList = list
                            updateUIAfterSearch()
                        } else {
                            errorInSystem()
                            Log.d(
                                "Search Firebase",
                                "Erro ao realizar a pesquisa. Tente novamente e Reporte esse problema!"
                            )
                        }
                    }
                }
            } else {
                binding.layoutTextSearching.visibility = View.GONE
            }
        }, 500)
    }

    private fun actionSearchListRoom() {
        citizenViewModel.loadListCitizensRoom(
            workspaceId,
            limitValue,
            orderAlphabet
        ) { list ->
            toggleLayout(list.isNotEmpty())
        }

        updateValuesInListOptions()
        println(filtersCitizenList)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                if (searchCategory.isNotEmpty()) {
                    citizenViewModel.searchCitizenByFieldRoom(
                        workspaceId,
                        filtersCitizenList
                    ) { res, list ->
                        if (res) {
                            citizenList = CitizenModel.fromEntityList(list)
                            updateUIAfterSearch()
                        } else {
                            Log.d(
                                "Search Room",
                                "Erro ao realizar a pesquisa. Tente novamente e Reporte esse problema!"
                            )
                            errorInSystem()
                        }
                    }
                }
            } else {
                binding.layoutTextSearching.visibility = View.GONE
            }
        }, 500)
    }

    private fun loadListCitizensFirebase() {
        toggleVisibleProgressBar(true)
        citizenViewModel.citizenListModel.observe(viewLifecycleOwner, Observer { list ->
            citizenList = list ?: emptyList() // Garantir que a lista não seja nula
            if (::adapter.isInitialized) {
                adapter.submitList(citizenList)
            } else {
                initializeRecyclerView()
            }
            toggleVisibleProgressBar(false)
            citizenViewModel.sizeListCitizens(workspaceId, limitValue) { size ->
                toggleLayout(size > 0)
            }
        })

        citizenViewModel.loadListCitizens(workspaceId, limitValue, orderAlphabet)

//        Handler(Looper.getMainLooper()).postDelayed({
//            toggleVisibleProgressBar(false)
//        }, 1500)
    }

    private fun loadListCitizensRoom() {
        citizenViewModel.loadListCitizensRoom(
            workspaceId,
            limitValue,
            orderAlphabet
        ) { list ->
            citizenList = CitizenModel.fromEntityList(list.toMutableList())

            if (::adapter.isInitialized) {
                adapter.submitList(citizenList)
            } else {
                initializeRecyclerView()
            }

            toggleLayout(citizenList.isNotEmpty())
            toggleVisibleProgressBar(false)
        }
    }

    private fun toggleVisibleProgressBar(state: Boolean) {
        binding.progressBar.visibility = if (state) View.VISIBLE else View.GONE
    }

    private fun toggleLayout(state: Boolean) {
        binding.layoutCitizenWithData.visibility = if (state) View.VISIBLE else View.GONE
        binding.layoutCitizenEmpty.visibility = if (state) View.GONE else View.VISIBLE
    }

    private fun initializeRecyclerView() {
        binding.recycleviewCitizen.layoutManager = LinearLayoutManager(context)
        adapter = CitizenListAdapter(
            citizenList,
            false,
            CitizenOnClickListener { citizenModel, _ ->
                val intent = Intent(requireActivity(), CitizenDetailActivity::class.java)
                intent.putExtra("workspaceId", workspaceId)
                intent.putExtra("userId", userId)
                intent.putExtra("citizenId", citizenModel.id)
                intent.putExtra("citizenModel", citizenModel)
                intent.putExtra("workspaceModel", workspaceModel)
                intent.putExtra("userModel", userModel)
                result.launch(intent)
            })
        binding.recycleviewCitizen.adapter = adapter
    }

    private fun loadFragment(fragment: Fragment, addToBack: Boolean) {
        val activity = activity

        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            val transition = requireActivity().supportFragmentManager.beginTransaction()
            transition.replace(R.id.fragment_container_citizens, fragment)
            if (addToBack) {
                transition.addToBackStack(null)
            }
            transition.commit()
        }
    }

    companion object {
        private const val ARG_WORKSPACE_ID = "workspace_id"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_USER = "user"
        private const val INACTIVE_CODE = 2
        private const val UPDATE_CODE = 3
        private const val DELETE_CODE = 4

        fun newInstance(
            workspaceId: String,
            userId: String,
            userModel: UserModel,
        ): WorkspaceMainFragment {
            val fragment = WorkspaceMainFragment()
            val args = Bundle()
            args.putString(ARG_WORKSPACE_ID, workspaceId)
            args.putString(ARG_USER_ID, userId)
            args.putParcelable(ARG_USER, userModel)
            fragment.arguments = args
            return fragment
        }
    }
}
