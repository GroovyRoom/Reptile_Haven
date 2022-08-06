package com.example.pethaven.ui.features.home

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pethaven.R
import com.example.pethaven.adapter.ReptileBoxAdaptor
import com.example.pethaven.adapter.ReptileInfoAdapter
import com.example.pethaven.domain.Reptile
import com.example.pethaven.ui.features.shop.TradePostActivity
import com.example.pethaven.util.AndroidExtensions.makeToast
import com.example.pethaven.util.FactoryUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment(), ReptileInfoAdapter.OnReptileItemCLickedListener {

    private lateinit var progressBar: ProgressBar

    private lateinit var searchView: androidx.appcompat.widget.SearchView

    private lateinit var recyclerSearchView: RecyclerView
    private lateinit var reptileInfoAdapter: ReptileInfoAdapter
    private lateinit var reptileBoxRecyclerview: RecyclerView
    private lateinit var reptileBoxAdaptor: ReptileBoxAdaptor

    private lateinit var testViewModel: HomeTestViewModel

    private lateinit var addFab: FloatingActionButton


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.fragment_home_test, container, false)

        setUpTestViewModel()
        setUpRecyclerView(view)
        setUpProgressBar(view)
        setUpFloatingActionButton(view)

        receiveAllReptiles(view)
        setUpReptileBoxRecyclerView(view)
        setUpSearchView(view)

        return view
    }

    private val swipeItemCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT){
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        // Start Trade Post Activity when swiped
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val reptileKey = reptileInfoAdapter.getReptile(viewHolder.adapterPosition).key
            if (reptileKey != null) {
                val intent = TradePostActivity.makeIntent(requireActivity(),reptileKey)
                startActivity(intent)
            } else {
                makeToast("Error: Reptile Key not found!")
            }
            reptileInfoAdapter.notifyItemChanged(viewHolder.adapterPosition)
        }
    }


    // --------------------- Initializing Views ---------------- //
    private fun setUpProgressBar(view: View) {
        progressBar = view.findViewById(R.id.reptileListProgressBar)
        progressBar.isIndeterminate = true
    }

    private fun setUpRecyclerView(view: View) {
        recyclerSearchView = view.findViewById(R.id.reptileInfoRecyclerView)
        recyclerSearchView.setHasFixedSize(true)
        recyclerSearchView.layoutManager = LinearLayoutManager(requireActivity())

        reptileInfoAdapter = ReptileInfoAdapter(requireActivity(), ArrayList(), this)
        recyclerSearchView.adapter = reptileInfoAdapter

        val itemTouchHelper = ItemTouchHelper(swipeItemCallback)
        itemTouchHelper.attachToRecyclerView(recyclerSearchView)
    }

    private fun setUpReptileBoxRecyclerView(view: View)
    {
        reptileBoxRecyclerview = view.findViewById(R.id.reptileBoxRecyclerView)
        reptileBoxRecyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            reptileBoxAdaptor = ReptileBoxAdaptor()
            adapter = reptileBoxAdaptor
            reptileBoxAdaptor.setOnItemClickListener(object : ReptileBoxAdaptor.OnItemClickListener
            {
                override fun onItemClick(position: Int) {
                    testViewModel.toggleBtnSwitch(position)
                }
            })
        }
    }

    private fun setUpFloatingActionButton(view: View) {
        addFab = view.findViewById(R.id.fabAddReptile)
        addFab.setOnClickListener{
            testViewModel.isSearchOn.value = !testViewModel.isSearchOn.value!!
            val intent = Intent(requireActivity(), AddEditReptileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setUpSearchView(view: View) {
        searchView = view.findViewById(R.id.reptileSearchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
                reptileInfoAdapter.filter.filter(query)
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                reptileInfoAdapter.filter.filter(newText)
                return true
            }
        })
    }


    // --------------------- Initializing View Model  ---------------- //
    private fun setUpTestViewModel() {
        val factory = FactoryUtil.generateReptileViewModelFactory(requireActivity())
        testViewModel = ViewModelProvider(this, factory)[HomeTestViewModel::class.java]

        testViewModel.reptilesBoxes.observe(viewLifecycleOwner)
        {
            println("debug: reptile boxes changed")
            reptileBoxAdaptor.updateList(testViewModel.reptilesBoxes.value!!, testViewModel.btnSwitches.value!!)
        }

        testViewModel.btnSwitches.observe(viewLifecycleOwner)
        {
            println("debug: btn switches changed")
            reptileBoxAdaptor.updateList(testViewModel.reptilesBoxes.value!!, testViewModel.btnSwitches.value!!)
        }
    }


    // --------------------- Database Operations  ---------------- //
    private fun receiveAllReptiles(view: View) {
        testViewModel.reptileTask.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    progressBar.visibility = View.GONE
                    return
                }

                val list = ArrayList<Reptile>()
                testViewModel.reptilesBoxes.value!!.clear()
                for (postSnapShot in snapshot.children) {
                    val reptile = postSnapShot.getValue(Reptile::class.java)
                    reptile?.let {
                        //println(it.isFav)
                        it.key = postSnapShot.key
                        list.add(it)
                        testViewModel.addReptile(it)
                    }
                }
                reptileInfoAdapter.setReptileList(list)
                reptileInfoAdapter.filter.filter(searchView.query)
                // searchView.setQuery(searchView.query, true)

                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                makeToast(error.message)
                progressBar.visibility = View.GONE
            }
        })
    }


    // --------------------- Adapter OnClick Listener  ---------------- //
    override fun onReptileClicked(position: Int) {
        val reptileKey = reptileInfoAdapter.getReptile(position).key

        if (reptileKey != null) {
            val intent = ReptileProfileActivity.makeIntent(requireActivity(), reptileKey)
            startActivity(intent)
        } else {
            makeToast("Error: Reptile Key not found!")
        }
    }
}