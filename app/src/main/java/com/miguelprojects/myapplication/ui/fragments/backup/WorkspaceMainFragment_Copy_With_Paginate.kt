package com.miguelprojects.myapplication.ui.fragments.backup

import androidx.fragment.app.Fragment


class WorkspaceMainFragment_Copy_With_Paginate : Fragment() {
//    private lateinit var binding: FragmentWorkspaceMainBinding
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var citizenListAdapter: CitizenListAdapter
//    private lateinit var citizenViewModel: CitizenViewModel
//    private var isLoading = false
//    private var workspaceId: String = ""
//    private var lastCitizenKey: String? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        arguments?.let {
//            workspaceId = it.getString(ARG_WORKSPACE_ID) ?: ""
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentWorkspaceMainBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        recyclerView = view.findViewById(R.id.recycleview_citizen)
//        citizenListAdapter = CitizenListAdapter(CitizenOnClickListener { item ->
//            Toast.makeText(requireContext(), "Item clicado", Toast.LENGTH_SHORT).show()
//        })
//        recyclerView.adapter = citizenListAdapter
//
//        val layoutManager = LinearLayoutManager(context)
//        recyclerView.layoutManager = layoutManager
//
////        Initialize viewModel
//        citizenViewModel = ViewModelProvider(this)[CitizenViewModel::class.java]
//
//        // Load the first page
//        loadMoreCitizens()
//
//        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//
//                Log.d("Teste recycle view scroll", "Scroll Listener em ação")
//
//                val totalItemCount = layoutManager.itemCount
//                val visibleItemCount = layoutManager.childCount
//                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
//
//                if (!isLoading && (visibleItemCount + firstVisibleItemPosition >= totalItemCount)) {
//                    loadMoreCitizens()
//                }
//            }
//        })
//    }
//
//    private fun loadMoreCitizens() {
//        isLoading = true
//        var newLastCitizenKey = lastCitizenKey ?: ""
//
//        citizenViewModel.loadPaginatedCitizens(workspaceId, newLastCitizenKey, 10)
//        citizenViewModel.citizenListModel.observe(viewLifecycleOwner, Observer { list ->
//            if (list.isNotEmpty()) {
//                citizenListAdapter.addCitizens(list)
//                lastCitizenKey = list.last().name  // Assumindo que o modelo tem um campo name
//            }
//            isLoading = false
//        })
//    }
//
//
//    companion object {
//        private const val ARG_WORKSPACE_ID = "workspace_id"
//
//        fun newInstance(workspaceId: String): WorkspaceMainFragment_Copy_With_Paginate {
//            val fragment = WorkspaceMainFragment_Copy_With_Paginate()
//            val args = Bundle()
//            args.putString(ARG_WORKSPACE_ID, workspaceId)
//            fragment.arguments = args
//            return fragment
//        }
//
//    }
}