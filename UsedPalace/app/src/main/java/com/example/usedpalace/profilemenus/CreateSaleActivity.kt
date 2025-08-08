package com.example.usedpalace.profilemenus

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.dataClasses.SaleManagerMethod
import com.example.usedpalace.UserSession
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CreateSaleActivity : AppCompatActivity() {

    private lateinit var saleManagerMethod: SaleManagerMethod
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    // Image URIs
    private val imageUris = mutableListOf<Uri?>().apply {
        repeat(5) { add(null) }
    }

    // Image contracts
    private val imageContracts = listOf(
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUris[0] = it
                findViewById<ImageView>(R.id.image1).setImageURI(it)
            }
        },
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUris[1] = it
                findViewById<ImageView>(R.id.image2).setImageURI(it)
            }
        },
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUris[2] = it
                findViewById<ImageView>(R.id.image3).setImageURI(it)
            }
        },
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUris[3] = it
                findViewById<ImageView>(R.id.image4).setImageURI(it)
            }
        },
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUris[4] = it
                findViewById<ImageView>(R.id.image5).setImageURI(it)
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_sale)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initialize()
        saleManagerMethod = SaleManagerMethod(this, apiService)

        setupUI()
        setupClickListeners()
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupUI() {
        // Setup spinners
        saleManagerMethod.setupCategorySpinners(
            findViewById(R.id.mainCategory),
            findViewById(R.id.subCategory)
        ) { position ->
            if (position > 0) {
                saleManagerMethod.updateSubcategories(position, findViewById(R.id.subCategory))
            }
        }

        // Setup image click listeners
        listOf(R.id.image1, R.id.image2, R.id.image3, R.id.image4, R.id.image5).forEachIndexed { index, resId ->
            findViewById<ImageView>(resId).setOnClickListener {
                imageContracts[index].launch("image/*")
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            navigateBackToProfile()
        }

        findViewById<Button>(R.id.createSale).setOnClickListener {
            createSale()
        }
    }

    private fun createSale() {
        try {
            val name = findViewById<EditText>(R.id.inputSaleName).text.toString().trim()
            val description = findViewById<EditText>(R.id.inputDesc).text.toString().trim()
            val cost = findViewById<EditText>(R.id.inputCost).text.toString().toIntOrNull() ?: 0

            val (bigCategory, smallCategory) = saleManagerMethod.getSelectedCategories(
                findViewById(R.id.mainCategory),
                findViewById(R.id.subCategory)
            )

            if (name.isEmpty() || description.isEmpty() || cost <= 0 || bigCategory == null) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return
            }

            saleManagerMethod.createSale(
                name = name,
                description = description,
                cost = cost,
                bigCategory = bigCategory,
                smallCategory = smallCategory,
                userId = UserSession.getUserId()!!
            ) { result ->
                runOnUiThread {
                    result.onSuccess { response ->
                        // Upload images after successful sale creation
                        uploadImages(response.saleFolder!!)
                        Toast.makeText(this, "Sale created successfully!", Toast.LENGTH_SHORT).show()
                        clearForm()
                    }.onFailure {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImages(saleFolder: String) {
        val imageViews = listOf(
            findViewById<ImageView>(R.id.image1),
            findViewById(R.id.image2),
            findViewById(R.id.image3),
            findViewById(R.id.image4),
            findViewById(R.id.image5)
        )

        // Set URI tags for selected images
        imageViews.forEachIndexed { index, imageView ->
            imageUris.getOrNull(index)?.let { uri ->
                imageView.tag = uri
            }
        }

        // Upload all images (selected or default)
        saleManagerMethod.uploadSaleImages(saleFolder, *imageViews.toTypedArray())
    }

    private fun clearForm() {
        findViewById<EditText>(R.id.inputSaleName).text.clear()
        findViewById<EditText>(R.id.inputDesc).text.clear()
        findViewById<EditText>(R.id.inputCost).text.clear()

        // Clear images
        listOf(R.id.image1, R.id.image2, R.id.image3, R.id.image4, R.id.image5).forEach { resId ->
            findViewById<ImageView>(resId).setImageResource(R.drawable.baseline_add_24)
        }
        imageUris.fill(null)
    }

    private fun navigateBackToProfile() {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            putExtra("SHOW_PROFILE_FRAGMENT", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }


}