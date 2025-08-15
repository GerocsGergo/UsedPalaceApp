package com.example.usedpalace.profileMenus.ownSalesActivity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.requests.DeleteSaleRequest
import com.example.usedpalace.MainMenuActivity

import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.dataClasses.SaleWithSid
import com.example.usedpalace.requests.SearchRequestID
import com.example.usedpalace.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class OwnSalesActivity : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private lateinit var containerLayout: LinearLayout
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
        setupClickListeners()


        initialize()
        fetchSalesDataSearch(apiService, containerLayout)
    }
    private fun setupViews(){
        buttonBack = findViewById(R.id.buttonBack)
        containerLayout = findViewById(R.id.container)
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

    //TODO search for ID not product name OR FOR USER NAME?
    //Functions from HomeFragment.kt, but modified
    private fun fetchSalesDataSearch(
        apiService: ApiService,
        containerLayout: LinearLayout
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear previous results
                withContext(Dispatchers.Main) {
                    containerLayout.removeAllViews()
                }

                val searchParamId = UserSession.getUserId() ?: return@launch
                // Make API call
                val response = apiService.searchSalesID(SearchRequestID(searchParamId))

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        if (response.data.isNotEmpty()) {
                            displaySales(apiService, response.data, containerLayout)
                        } else {
                            showNoProductsMessage(containerLayout, response.message)
                        }
                    } else {
                        Toast.makeText(this@OwnSalesActivity, "Search failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OwnSalesActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showNoProductsMessage(containerLayout, "Connection error")
                }
            }
        }
    }

    private fun displaySales(apiService: ApiService, sales: List<SaleWithSid>, containerLayout: LinearLayout) {
        val inflater = LayoutInflater.from(this)  // Get inflater here

        if (sales.isEmpty()) {
            // Create and show "No products found" message
            val noProductsView = inflater.inflate(R.layout.show_error_message, containerLayout, false)
            containerLayout.addView(noProductsView)
        } else {
            for (sale in sales) {
                val itemView = inflater.inflate(R.layout.item_own_sales, containerLayout, false)

                itemView.findViewById<TextView>(R.id.productName).text = sale.Name

                val open = itemView.findViewById<ImageButton>(R.id.open)
                val modify = itemView.findViewById<ImageButton>(R.id.modify)
                val delete = itemView.findViewById<ImageButton>(R.id.delete)

                containerLayout.addView(itemView)

                val saleId = sale.Sid
                // Add event listeners
                open.setOnClickListener {
                    onOpenClick(sale)
                }
                modify.setOnClickListener {
                    onModifyClick(sale)
                }
                delete.setOnClickListener {
                    showDeleteConfirmationDialog(apiService, saleId, itemView, containerLayout)
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


    private fun showNoProductsMessage(
        containerLayout: LinearLayout?,
        message: String = "No products found"
    ) {
        containerLayout?.removeAllViews()
        val noProductsView = LayoutInflater.from(this).inflate(R.layout.show_error_message, containerLayout, false)
        noProductsView.findViewById<TextView>(R.id.messageText).text = message
        containerLayout?.addView(noProductsView)
    }

    //For delete modify and open
    private fun showDeleteConfirmationDialog(apiService: ApiService, saleId: Int, itemView: View, containerLayout: LinearLayout) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sale")
            .setMessage("Are you sure you want to delete this sale?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteSale(apiService, saleId, itemView, containerLayout)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteSale(apiService: ApiService, saleId: Int, itemView: View, containerLayout: LinearLayout) {
        val userId = UserSession.getUserId()!!
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.deleteSale(DeleteSaleRequest(saleId, userId))
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        // Remove from UI
                        containerLayout.removeView(itemView)
                        Toast.makeText(
                            this@OwnSalesActivity,
                            "Sale deleted successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Refresh the list or show empty state if needed
                        if (containerLayout.childCount == 0) {
                            showNoProductsMessage(containerLayout, "No sales found")
                        }
                    } else {
                        Toast.makeText(
                            this@OwnSalesActivity,
                            "Failed to delete: ${response.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@OwnSalesActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
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