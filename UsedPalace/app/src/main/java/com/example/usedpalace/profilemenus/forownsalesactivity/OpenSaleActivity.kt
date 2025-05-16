package com.example.usedpalace.profilemenus.forownsalesactivity

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.R
import com.example.usedpalace.SaleWithEverything
import com.example.usedpalace.requests.SearchRequestID
import com.squareup.picasso.Picasso
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
    private lateinit var productImage: ImageView
    private lateinit var image2: ImageView
    private lateinit var image3: ImageView
    private lateinit var image4: ImageView
    private lateinit var image5: ImageView
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
        productImage = findViewById(R.id.productImage)
        image2 = findViewById(R.id.image2)
        image3 = findViewById(R.id.image3)
        image4 = findViewById(R.id.image4)
        image5 = findViewById(R.id.image5)
        productTitle = findViewById(R.id.productLabel)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)
        mainLayout = findViewById(R.id.main)
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

        loadImage(productImage, sale.SaleFolder, "image1.jpg")
        loadImage(image2, sale.SaleFolder, "image2.jpg")
        loadImage(image3, sale.SaleFolder, "image3.jpg")
        loadImage(image4, sale.SaleFolder, "image4.jpg")
        loadImage(image5, sale.SaleFolder, "image5.jpg")
    }

    private fun loadImage(imageView: ImageView, saleFolder: String, imageName: String) {
        val imageUrl = "http://10.0.2.2:3000/$saleFolder/$imageName"
        Picasso.get()
            .load(imageUrl)
            .placeholder(R.drawable.baseline_loading_24)
            .error(R.drawable.baseline_error_24)
            .into(imageView)
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