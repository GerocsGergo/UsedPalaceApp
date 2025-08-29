package com.example.usedpalace.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import network.RetrofitClient
import com.example.usedpalace.dataClasses.SaleWithSid
import com.example.usedpalace.fragments.homeFragmentHelpers.HomeFragmentSingleSaleActivity
import com.example.usedpalace.fragments.homeFragmentHelpers.SalesAdapter
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.example.usedpalace.requests.SearchRequestName
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService


class HomeFragment : Fragment() {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private lateinit var searchView: SearchView
    private lateinit var clearButton :ImageButton
    private lateinit var filterButton: ImageView

    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageIndicator: TextView

    private var currentPage = 1
    private val pageSize = 10
    private var totalPages = 1

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SalesAdapter
    private var isLoading = false

    // default filter: mindkettő
    private var currentFilter: Int = R.id.filter_both


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewSales)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = SalesAdapter(mutableListOf()) { sale -> onProductClick(sale) }
        recyclerView.adapter = adapter

        fetchSalesData()

        setupViews(view)
        initialize()
        setupClickListeners()
        setupPagination()

        return view
    }

    private fun setupViews(view: View) {
        clearButton = view.findViewById(R.id.clearSearchButton)
        searchView = view.findViewById(R.id.searchBar)
        filterButton = view.findViewById(R.id.filterButton)
        prevPageButton = view.findViewById(R.id.prevPageButton)
        nextPageButton = view.findViewById(R.id.nextPageButton)
        pageIndicator = view.findViewById(R.id.pageIndicator)

    }

    private fun setupPagination() {
        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                fetchSalesData()
            }
        }
        nextPageButton.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                fetchSalesData()
            }
        }
    }


    private fun initialize() {
        prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(requireContext().applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupClickListeners() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    fetchSalesDataSearch(apiService, query)
                    searchView.clearFocus()

                    clearButton.visibility = View.VISIBLE
                } else {
                    clearButton.visibility = View.GONE
                    ErrorHandler.toaster(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT)
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                return false
            }
        })



        clearButton.setOnClickListener {
            searchView.setQuery("", false)
            searchView.clearFocus()
            //containerLayout.removeAllViews()
            fetchSalesData()
            //fetchSalesData(apiService, containerLayout, inflater)
            clearButton.visibility = View.GONE
        }

        filterButton.setOnClickListener { v ->
            val popup = PopupMenu(requireContext(), v)
            popup.menuInflater.inflate(R.menu.search_filter_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                currentFilter = item.itemId
                Toast.makeText(requireContext(), "Szűrés: ${item.title}", Toast.LENGTH_SHORT).show()
                true
            }
            popup.show()
        }
    }


    private fun fetchSalesDataSearch(
        apiService: ApiService,
        searchParam: String
    ) {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = when (currentFilter) {
                    R.id.filter_category -> apiService.searchSalesCategory(SearchRequestName(searchParam))
                    R.id.filter_name     -> apiService.searchSales(SearchRequestName(searchParam))
                    else -> {
                        val nameResponse = apiService.searchSales(SearchRequestName(searchParam))
                        val catResponse = apiService.searchSalesCategory(SearchRequestName(searchParam))
                        if (nameResponse.success || catResponse.success) {
                            val merged = mutableListOf<SaleWithSid>()
                            if (nameResponse.success) merged.addAll(nameResponse.data)
                            if (catResponse.success) merged.addAll(catResponse.data)
                            nameResponse.copy(success = true, data = merged)
                        } else nameResponse
                    }
                }

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        adapter.setSales(response.data)
                        pageIndicator.text = "1 / 1"
                        prevPageButton.isEnabled = false
                        nextPageButton.isEnabled = false
                        if (response.data.isEmpty()) {
                            ErrorHandler.toaster(requireContext(),"Nincs megjeleníthető hirdetés")
                        }
                    } else {
                        ErrorHandler.handleApiError(requireContext(), null, response.message)
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            }
        }
    }

    private fun fetchSalesData() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getSales(currentPage, pageSize)
                withContext(Dispatchers.Main) {
                    totalPages = response.totalPages

                    // teljes lista frissítése
                    adapter.setSales(response.data)

                    // lapozó frissítése
                    pageIndicator.text = "$currentPage / $totalPages"
                    prevPageButton.isEnabled = currentPage > 1
                    nextPageButton.isEnabled = currentPage < totalPages
                    if (response.data.isEmpty()) {
                          ErrorHandler.toaster(requireContext(),"Nincs megjeleníthető hirdetés")
                    }
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Connection error", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }
        }
    }


    private fun onProductClick(sale: SaleWithSid) {
        val saleId = sale.Sid
        val sellerId = sale.SellerId
        val intent = Intent(context, HomeFragmentSingleSaleActivity::class.java).apply {
            putExtra("SALE_ID", saleId) //Give the saleId to the activity
            putExtra("SELLER_ID", sellerId)
        }
        startActivity(intent)
    }

}
