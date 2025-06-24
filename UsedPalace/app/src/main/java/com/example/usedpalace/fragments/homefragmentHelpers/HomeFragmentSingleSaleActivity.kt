package com.example.usedpalace.fragments.homefragmentHelpers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.usedpalace.R
import com.example.usedpalace.SaleWithEverything
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.messagesHelpers.ChatActivity
import com.example.usedpalace.fragments.messagesHelpers.Requests.InitiateChatRequest
import com.example.usedpalace.requests.SearchRequestID
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HomeFragmentSingleSaleActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private var saleId: Int = -1
    private var sellerId: Int = -1
    private var buyerId: Int = -1

    // Views
    private lateinit var imageSlider: ViewPager2
    private lateinit var productTitle: TextView
    private lateinit var productPrice: TextView
    private lateinit var productDescription: TextView
    private lateinit var mainLayout: ConstraintLayout
    private lateinit var messageButton: Button
    private lateinit var backButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_fragment_single_sale)
        enableEdgeToEdge()

        initializeViews()
        setupRetrofit()
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

        messageButton.setOnClickListener {
            initiateChat()
        }
    }

    private fun initiateChat() {
        buyerId = UserSession.getUserId() ?: run {
            Toast.makeText(this, "You must be logged in to message sellers", Toast.LENGTH_SHORT).show()
            return
        }

        if (sellerId == buyerId) {
            Toast.makeText(this, "You can't message yourself", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.initiateChat(
                    InitiateChatRequest(
                        sellerId = sellerId,
                        buyerId = buyerId,
                        saleId = saleId
                    )
                )
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        if (response.chatId != null) {
                            val intent = Intent(this@HomeFragmentSingleSaleActivity, ChatActivity::class.java).apply {
                                putExtra("CHAT_ID", response.chatId)
                                val userId = UserSession.getUserId()
                                val username = if (userId == buyerId){
                                    fetchUsername(sellerId)
                                }else{
                                    fetchUsername(buyerId)
                                }
                                putExtra("USERNAME", username)
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                this@HomeFragmentSingleSaleActivity,
                                response.message ?: "Failed to initiate chat",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@HomeFragmentSingleSaleActivity,
                            "Error: " + response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@HomeFragmentSingleSaleActivity,
                        "Network error: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private suspend fun fetchUsername(userId: Int): String? {

        return try {
            val response = apiService.searchUsername(SearchRequestID(userId))
            if (response.success && response.fullname != null) {
                response.fullname
            } else {
                Log.e("Search", "Username not found: ${response.message}")
                null // Return null if not found
            }
        } catch (e: Exception) {
            Log.e("Search", "Error fetching username", e)
            null // Return null on error
        }
    }


    private fun initializeViews() {
        imageSlider = findViewById(R.id.imageSlider)
        productTitle = findViewById(R.id.productLabel)
        productPrice = findViewById(R.id.productPrice)
        productDescription = findViewById(R.id.productDescription)
        mainLayout = findViewById(R.id.main)

        backButton = findViewById(R.id.backButton)
        messageButton = findViewById(R.id.messageButton)
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
        sellerId = intent.getIntExtra("SELLER_ID", -1)
        if (sellerId == -1) {
            showErrorMessage("Invalid seller ID")
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