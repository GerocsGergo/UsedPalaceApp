package com.example.usedpalace.profilemenus.forownsalesactivity

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.usedpalace.R
import com.example.usedpalace.dataClasses.SaleWithEverything
import com.example.usedpalace.fragments.homefragmentHelpers.ImageSliderAdapter
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OpenSaleActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private var saleId: Int = -1

    // Views
    private lateinit var imageSlider: ViewPager2
    private lateinit var productTitle: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var mainLayout: ConstraintLayout

    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_sale)
        enableEdgeToEdge()

        initializeViews()
        setUpClickListeners()
        setupRetrofit()
        getIntentData()

        if (saleId != -1) {
            fetchSaleData()
        }
    }

    private fun setUpClickListeners(){
        backButton = findViewById(R.id.backButton)
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

    private fun setupRetrofit() {
        apiService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun getIntentData() {
        saleId = intent.getIntExtra("SALE_ID", -1)
        if (saleId == -1) {
            showErrorMessage("Invalid sale ID")
            finish()
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
                        } ?: showErrorMessage("Sale data is null")
                    } else {
                        showErrorMessage(response.message ?: "Failed to load sale")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorMessage("Network error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun displaySale(sale: SaleWithEverything) {
        productTitle.text = sale.Name
        productPrice.text = "${sale.Cost} Ft"
        productDescription.text = sale.Description

        val imageUrls = mutableListOf(
            "http://10.0.2.2:3000/${sale.SaleFolder}/image1.jpg",
            "http://10.0.2.2:3000/${sale.SaleFolder}/image2.jpg",
            "http://10.0.2.2:3000/${sale.SaleFolder}/image3.jpg",
            "http://10.0.2.2:3000/${sale.SaleFolder}/image4.jpg",
            "http://10.0.2.2:3000/${sale.SaleFolder}/image5.jpg"
        )

        val adapter = ImageSliderAdapter(this, imageUrls)
        imageSlider.adapter = adapter
    }

    private fun showErrorMessage(message: String) {
        mainLayout.removeAllViews()

        val errorView = layoutInflater.inflate(
            R.layout.show_error_message,
            mainLayout,
            false
        )

        errorView.findViewById<TextView>(R.id.messageText).text = message
        mainLayout.addView(errorView)
    }
}