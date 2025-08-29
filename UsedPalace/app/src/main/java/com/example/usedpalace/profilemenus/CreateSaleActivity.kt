package com.example.usedpalace.profileMenus

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import network.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.profileMenus.ownSalesActivity.ImageAdapter
import com.example.usedpalace.requests.CreateSaleRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class CreateSaleActivity : AppCompatActivity() {

    private lateinit var saleManagerHelper: SaleManagerHelper
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private val imageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImageAdapter

    companion object {
        private const val REQUEST_CODE_PICK_IMAGES = 1001
        private const val MAX_IMAGES = 5
    }

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
        saleManagerHelper = SaleManagerHelper(this, apiService)

        setupUI()
        setupClickListeners()
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupUI() {
        // Képfeltöltés RecyclerView
        imageUris.clear()
        imageAdapter = ImageAdapter(imageUris) { position ->
            imageUris.removeAt(position)
            imageAdapter.notifyDataSetChanged()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.imageRecyclerView)
        recyclerView.adapter = imageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        findViewById<MaterialButton>(R.id.btnAddImage).setOnClickListener {
            if (imageUris.size >= MAX_IMAGES) {
                ErrorHandler.toaster(this,"Maximum $MAX_IMAGES kép tölthető fel")
                //Toast.makeText(this, "Maximum $MAX_IMAGES kép tölthető fel", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saleManagerHelper.pickImages(this, REQUEST_CODE_PICK_IMAGES)
        }

        // Spinner beállítás
        val mainCategory = findViewById<Spinner>(R.id.mainCategory)
        val subCategory = findViewById<Spinner>(R.id.subCategory)

        saleManagerHelper.setupCategorySpinners(mainCategory, subCategory) { position ->
            if (position > 0) {
                saleManagerHelper.updateSubcategories(position, subCategory)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK) {
            saleManagerHelper.handleActivityResult(data, imageUris, MAX_IMAGES, this)
            imageAdapter.notifyDataSetChanged()
        }
    }

    private fun setupClickListeners() {
        val buttonBack = findViewById<MaterialButton>(R.id.buttonBack)
        buttonBack.setOnClickListener {
            saleManagerHelper.navigateBackToProfile(this@CreateSaleActivity)
        }

        val createButton = findViewById<MaterialButton>(R.id.createSale)
        createButton.setOnClickListener {
            createSale(createButton)
        }
    }

    private fun createSale(createButton: MaterialButton) {
        val name = findViewById<TextInputEditText>(R.id.inputSaleName).text.toString().trim()
        val description = findViewById<TextInputEditText>(R.id.inputDesc).text.toString().trim()
        val cost = findViewById<TextInputEditText>(R.id.inputCost).text.toString().toIntOrNull() ?: 0

        val mainCategory = findViewById<Spinner>(R.id.mainCategory)
        val subCategory = findViewById<Spinner>(R.id.subCategory)
        val (bigCategory, smallCategory) = saleManagerHelper.getSelectedCategories(mainCategory, subCategory)

        if (name.isEmpty() || description.isEmpty() || cost <= 0 || bigCategory == null) {
            //Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            ErrorHandler.toaster(this,"Kérjük töltsön ki minden mezőt!")
            return
        }

        createButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.createSale(
                    CreateSaleRequest(
                        name = name,
                        description = description,
                        cost = cost,
                        bigCategory = bigCategory,
                        smallCategory = smallCategory,
                        userId = UserSession.getUserId()!!
                    )
                )

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        saleManagerHelper.uploadImages(this@CreateSaleActivity, response.saleFolder!!, imageUris) {
                            saleManagerHelper.showSuccessDialog(
                                this@CreateSaleActivity,
                                "Sikeres módosítás",
                                "A hirdetésed sikeresen létre lett hozva."
                            ) {
                                saleManagerHelper.navigateBackToProfile(this@CreateSaleActivity)
                            }
                        }
                    } else {
                        ErrorHandler.handleApiError(this@CreateSaleActivity,null,response.message)
                        //Toast.makeText(this@CreateSaleActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        createButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //Toast.makeText(this@CreateSaleActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    ErrorHandler.handleNetworkError(this@CreateSaleActivity,e)
                    createButton.isEnabled = true
                }
            }
        }
    }
}
