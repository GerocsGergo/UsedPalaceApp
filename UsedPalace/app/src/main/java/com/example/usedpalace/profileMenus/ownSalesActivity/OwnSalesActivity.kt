package com.example.usedpalace.profileMenus.ownSalesActivity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.requests.DeleteSaleRequest
import com.example.usedpalace.MainMenuActivity

import com.example.usedpalace.R
import network.RetrofitClient
import com.example.usedpalace.dataClasses.SaleWithSid
import com.example.usedpalace.requests.SearchRequestID
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.homeFragmentHelpers.SalesAdapter
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class OwnSalesActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private var currentPage = 1
    private val pageSize = 10
    private var totalPages = 1
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageIndicator: TextView
    private lateinit var adapter: OwnSalesAdapter
    private var isLoading = false
    private lateinit var noSalesMessage: TextView

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_own_sales)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        setupPagination()
        setupClickListeners()


        initialize()
        fetchSalesDataSearch()
    }
    private fun setupViews() {
        buttonBack = findViewById(R.id.buttonBack)
        recyclerView = findViewById(R.id.recyclerViewOwnSales)
        noSalesMessage = findViewById(R.id.noSalesMessage)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = OwnSalesAdapter(
            mutableListOf(),
            onOpen = { sale -> onOpenClick(sale) },
            onModify = { sale -> onModifyClick(sale) },
            onDelete = { sale -> deleteSale(apiService, sale.Sid) }
        )
        recyclerView.adapter = adapter
    }


    private fun setupPagination() {
        prevPageButton = findViewById(R.id.prevPageButton)
        nextPageButton = findViewById(R.id.nextPageButton)
        pageIndicator = findViewById(R.id.pageIndicator)

        prevPageButton.setOnClickListener {
            if (currentPage > 1 && !isLoading) {
                currentPage--
                fetchSalesDataSearch()
            }
        }

        nextPageButton.setOnClickListener {
            if (currentPage < totalPages && !isLoading) {
                currentPage++
                fetchSalesDataSearch()
            }
        }
    }

    private fun setupClickListeners(){
        buttonBack.setOnClickListener {
            navigateBackToProfile()

        }
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun fetchSalesDataSearch() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = UserSession.getUserId() ?: return@launch
                val response = apiService.searchSalesID(
                    userId = userId,
                    page = currentPage,
                    limit = pageSize
                )

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        val sales = response.data
                        totalPages = response.totalPages.coerceAtLeast(1) // legalább 1 oldal

                        if (sales.isNotEmpty()) {
                            hideNoProductsMessage()
                            adapter.setSales(sales)
                        } else {
                            showNoProductsMessage("Nincs hirdetésed.")
                            adapter.setSales(emptyList())
                        }

                        // Pagination gombok frissítése
                        pageIndicator.text = "$currentPage / $totalPages"
                        prevPageButton.isEnabled = currentPage > 1
                        nextPageButton.isEnabled = currentPage < totalPages
                    } else {
                        showNoProductsMessage("Hiba történt: ${response.message}")
                        adapter.setSales(emptyList())
                    }

                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@OwnSalesActivity, e)
                    showNoProductsMessage("Ismeretlen hiba történt.")
                    isLoading = false
                }
            }
        }
    }


    private fun onModifyClick(sale: SaleWithSid) {
        val saleId = sale.Sid
        val intent = Intent(this, ModifySaleActivity::class.java).apply {
            putExtra("SALE_ID", saleId) //Give the saleId to the activity
        }
        startActivity(intent)
    }

    private fun onOpenClick(sale: SaleWithSid) {
        val saleId = sale.Sid
        val intent = Intent(this, OpenSaleActivity::class.java).apply {
            putExtra("SALE_ID", saleId) //Give the saleId to the activity
        }
        startActivity(intent)
    }


    private fun showNoProductsMessage(message: String) {
        noSalesMessage.text = message
        noSalesMessage.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideNoProductsMessage() {
        noSalesMessage.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }


    private fun deleteSale(apiService: ApiService, saleId: Int) {
        val userId = UserSession.getUserId()!!
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.deleteSale(DeleteSaleRequest(saleId, userId))
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        val currentList = adapter.sales.toMutableList()
                        val toRemove = currentList.find { it.Sid == saleId }
                        if (toRemove != null) {
                            currentList.remove(toRemove)
                            adapter.setSales(currentList)
                        }

                        ErrorHandler.toaster(this@OwnSalesActivity, "Sikeresen törölve")
                    } else {
                        ErrorHandler.handleApiError(this@OwnSalesActivity, null, response.message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@OwnSalesActivity, e)
                }
            }
        }
    }


    //For navigation
    private fun navigateBackToProfile() {
        // Create an intent to return to MainMenuActivity
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            // Add flag to indicate we want to show ProfileFragment
            putExtra("SHOW_PROFILE_FRAGMENT", true)
            // Clear the activity stack so we don't have multiple MainMenuActivities
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish() // Close the current SettingsActivity
    }

    // Handle system back button press
    override fun onBackPressed() {
        super.onBackPressed()
        navigateBackToProfile()
    }
}