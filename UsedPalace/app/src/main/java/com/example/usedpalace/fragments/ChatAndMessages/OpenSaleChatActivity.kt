package com.example.usedpalace.fragments.ChatAndMessages

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.dataClasses.SaleWithEverything
import com.example.usedpalace.fragments.homeFragmentHelpers.ImageSliderAdapter
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class OpenSaleChatActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private var saleId: Int = -1
    private var sellerId: Int = -1

    // Views
    private lateinit var imageSlider: ViewPager2
    private lateinit var productTitle: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_sale_chat)
        enableEdgeToEdge()

        initializeViews()
        initialize()
        getIntentData()

        if (saleId != -1) {
            fetchSaleData()
        }

        setUpClickListeners()

    }

    private fun setUpClickListeners(){
        backButton.setOnClickListener {
            finish()
        }
    }


    private fun initializeViews() {
        imageSlider = findViewById(R.id.imageSlider)
        productTitle = findViewById(R.id.productLabel)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)
        mainLayout = findViewById(R.id.main)

        backButton = findViewById(R.id.backButton)
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun getIntentData() {
        saleId = intent.getIntExtra("SALE_ID", -1)
        if (saleId == -1) {
            ErrorHandler.toaster(this,"Ismeretlen hiba történt")
            ErrorHandler.logToLogcat("ChatActivity", "Hiba: saleId: $saleId", ErrorHandler.LogLevel.ERROR)
            finish()
        }
        sellerId = intent.getIntExtra("SELLER_ID", -1)
        if (sellerId == -1) {
            ErrorHandler.toaster(this,"Ismeretlen hiba történt")
            ErrorHandler.logToLogcat("ChatActivity", "Hiba: sellerId: $sellerId", ErrorHandler.LogLevel.ERROR)
            finish()
        }
    }

    private fun fetchSaleImages(sid: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Feltételezem, hogy van egy endpoint, ami képeket ad vissza egy eladáshoz
                // Ha nincs, használhatod az apiService.getSaleImages ha van hasonló metódusod, mint a HomeFragmentben

                val imageResponse = apiService.getSaleImages(GetSaleImagesRequest(sid))
                withContext(Dispatchers.Main) {
                    if (imageResponse.success && !imageResponse.images.isNullOrEmpty()) {
                        val imageUrls = imageResponse.images
                        val adapter = ImageSliderAdapter(this@OpenSaleChatActivity, imageUrls)
                        imageSlider.adapter = adapter
                    } else {
                        // Ha nincs kép, adj egy default képet vagy üzenetet
                        val adapter = ImageSliderAdapter(this@OpenSaleChatActivity, listOf())
                        imageSlider.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@OpenSaleChatActivity,e)
                }
            }
        }
    }

    private fun fetchSaleData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchSalesSID(SearchRequestID(saleId))
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        response.data?.let { sale ->
                            displaySale(sale)
                        } ?: ErrorHandler.toaster(this@OpenSaleChatActivity,"Ismeretlen hiba történt")
                    } else {
                        ErrorHandler.handleApiError(this@OpenSaleChatActivity, null, response.message )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(this@OpenSaleChatActivity, e)
                }
            }
        }
    }

    private fun displaySale(sale: SaleWithEverything) {
        productTitle.text = sale.Name
        productPrice.text = "${sale.Cost} Ft"
        productDescription.text = sale.Description

        // Itt hívjuk meg az API-t a képek lekérésére a SaleFolder alapján
        fetchSaleImages(saleId)
    }

}