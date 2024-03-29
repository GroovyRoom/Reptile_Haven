package com.example.pethaven.ui.features.shop

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pethaven.R
import com.example.pethaven.domain.PostViewModel
import com.example.pethaven.ui.features.home.AddEditReptileViewModel
import com.example.pethaven.util.AndroidExtensions.makeToast
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 *  Fragment for displaying list of posts in the database
 */
class TradeListFragment : Fragment(), TradeTestAdapter.OnPostClickedListener {
/*    private var _binding: FragmentTradeListBinding? = null
    private val binding get() = _binding!!*/

    private lateinit var tradeListViewModel: PostViewModel
    private lateinit var recyclerView: RecyclerView

    private lateinit var tradeListSearchView: androidx.appcompat.widget.SearchView
    private lateinit var filterAllButton: Button
    private lateinit var filterUserButton: Button
    private lateinit var filterOtherButton: Button
    private lateinit var filterToggleButtonGroup: MaterialButtonToggleGroup

    private lateinit var tradeListProgressBar: ProgressBar
    private lateinit var adapter: TradeTestAdapter
    private lateinit var uid: String

    private lateinit var qrLauncher: ActivityResultLauncher<ScanOptions>

    companion object {
        private const val FILTER_ALL_BUTTON_ID = 1
        private const val FILTER_USER_BUTTON_ID = 2
        private const val FILTER_OTHER_BUTTON_ID = 3
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        //_binding = FragmentTradeListBinding.inflate(inflater, container, false)
        uid = FirebaseAuth.getInstance().currentUser!!.uid

        val view =  inflater.inflate(R.layout.fragment_trade_list, container, false)
        setUpSearchView(view)
        setUpFilterButtons(view)
        tradeListProgressBar = view.findViewById(R.id.tradeListProgressBar)

        val menuHost: MenuHost = requireActivity()

        // Add menu items without using the Fragment Menu APIs
        // Note how we can tie the MenuProvider to the viewLifecycleOwner
        // and an optional Lifecycle.State (here, RESUMED) to indicate when
        // the menu should be visible
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_trade_list, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.postQrScanner -> {
                        launchQrLauncher()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setUpQrLauncher()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //_binding = null
    }

    ///------------------------ Initializing Views-----------------------///
    private fun setUpFilterButtons(view: View) {
        filterToggleButtonGroup = view.findViewById(R.id.filterToggleButtonGroup)
        filterAllButton = view.findViewById(R.id.filterAllButton)
        filterUserButton = view.findViewById(R.id.filterUserButton)
        filterOtherButton = view.findViewById(R.id.filterOtherButton)

        filterToggleButtonGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            when (checkedId) {
                R.id.filterAllButton -> {
                    if (isChecked) {
                        switchFilterType(FILTER_ALL_BUTTON_ID)
                    }
                }
                R.id.filterUserButton -> {
                    if (isChecked) {
                        switchFilterType(FILTER_USER_BUTTON_ID)
                    }
                }
                R.id.filterOtherButton -> {
                    if (isChecked) {
                        switchFilterType(FILTER_OTHER_BUTTON_ID)
                    }
                }
            }
        }
    }

    private fun switchFilterType(filterButtonID: Int) {
        tradeListViewModel.currentFilterButtonID.value = filterButtonID
    }

    private fun setUpSearchView(view: View) {
        tradeListSearchView = view.findViewById(R.id.tradeListSearchView)
        tradeListSearchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean { return false }

            override fun onQueryTextChange(newText: String): Boolean {
                filterPost(newText)
                return true
            }
        })

    }

    private fun filterPost(text: String) {
        when (tradeListViewModel.currentFilterButtonID.value) {
            FILTER_ALL_BUTTON_ID -> { adapter.filter.filter(text) }
            FILTER_USER_BUTTON_ID -> { adapter.getUserFilter(uid).filter(text) }
            FILTER_OTHER_BUTTON_ID -> { adapter.getOtherFilter(uid).filter(text) }
            else -> adapter.filter.filter(text)
        }

        /*
           Dense: Remove This line if you don't want the recycler view to scroll to the top position
           whenever a new Text/post is pressed/inserted
        */
        recyclerView.smoothScrollToPosition(0)
    }

    ///------------------------ Initializing Recycler View and ViewModel -----------------------///
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //adapter = TradeListRecyclerViewAdapter(ArrayList())
        adapter = TradeTestAdapter(requireActivity(), this)
        //recyclerView = binding.tradeListRecyclerView
        recyclerView = view.findViewById(R.id.trade_list_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        tradeListViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        // Check if there is a new post added to the list and notify the adapter.
        tradeListViewModel.allPosts.observe(viewLifecycleOwner) {
            tradeListProgressBar.visibility = View.GONE
            adapter.updatePostList(it)
            filterPost(tradeListSearchView.query.toString())
        }

        tradeListViewModel.currentFilterButtonID.observe(requireActivity()) {
            filterPost(tradeListSearchView.query.toString())
        }
    }

    ///------------------------ Qr Functionality----------------------///
    private fun setUpQrLauncher() {
        qrLauncher = registerForActivityResult(ScanContract()) {
            if (it.contents == null) {
                makeToast("Cancelled")
                return@registerForActivityResult
            }

            val intent = EditTradePostActivity.makeIntent(requireActivity(), it.contents)
            startActivity(intent)
        }
    }

    private fun launchQrLauncher() {
        val scanOptions = ScanOptions().apply {
            setPrompt("Place barcode inside the rectangle view\n" +
                    "A clear background and adequate barcode size is recommended")
            setBeepEnabled(false)
        }
        qrLauncher.launch(scanOptions)
    }

    override fun onResume() {
        super.onResume()
        tradeListViewModel.getPosts()
    }
    override fun onPostClicked(key: String?) {
        if (key != null) {
            val intent = EditTradePostActivity.makeIntent(requireActivity(), key)
            startActivity(intent)
        } else {
            makeToast("Error: Post Key not found!")
        }
    }

}