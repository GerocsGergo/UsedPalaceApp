package com.example.usedpalace.profilemenus.forownsalesactivity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.SaleManagerMethod
import com.example.usedpalace.dataClasses.SaleWithEverything
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.ModifySaleRequest
import com.example.usedpalace.requests.SearchRequestID
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class ModifySaleActivity : AppCompatActivity() {

    private lateinit var saleManagerMethod: SaleManagerMethod
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private var saleId: Int = -1
    private lateinit var imageUrl1: String
    private lateinit var imageUrl2: String
    private lateinit var imageUrl3: String
    private lateinit var imageUrl4: String
    private lateinit var imageUrl5: String

    private lateinit var imageView1: ImageView
    private lateinit  var imageView2: ImageView
    private lateinit  var imageView3: ImageView
    private lateinit  var imageView4: ImageView
    private lateinit  var imageView5: ImageView

    private val changedImageIndexes = mutableSetOf<Int>()
    // Image URIs
    private val imageUris = mutableListOf<Uri?>().apply {
        repeat(5) { add(null) }
    }

    // Image contracts
    private val imageContracts = List(5) { index ->
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUris[index] = it
                getImageViewIndex(index).setImageURI(it)
                changedImageIndexes.add(index) // Track that this image was changed
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_modify_sale)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve the saleId from intent
        saleId = intent.getIntExtra("SALE_ID", -1)
        if (saleId == -1) {
            Toast.makeText(this, "Invalid sale ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }


        setupRetrofit()
        saleManagerMethod = SaleManagerMethod(this, apiService)

        setupUI()
        setupClickListeners()
        setupViewItems()

        fetchSalesDataSearch(apiService, saleId)

    }

    private fun setupViewItems(){
        imageView1 = findViewById(R.id.image1)
        imageView2 = findViewById(R.id.image2)
        imageView3 = findViewById(R.id.image3)
        imageView4 = findViewById(R.id.image4)
        imageView5 = findViewById(R.id.image5)
    }

    private fun setupRetrofit(){
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun getImageViewIndex(index: Int): ImageView = when(index) {
        0 -> imageView1
        1 -> imageView2
        2 -> imageView3
        3 -> imageView4
        else -> imageView5
    }

    private fun modifySale(apiService: ApiService, saleId: Int) {
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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.modifySale(
                    ModifySaleRequest(
                        saleId = saleId,
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
                        if (changedImageIndexes.isNotEmpty()) {
                            val imageViews = listOf(
                                findViewById<ImageView>(R.id.image1),
                                findViewById(R.id.image2),
                                findViewById(R.id.image3),
                                findViewById(R.id.image4),
                                findViewById(R.id.image5)
                            )

                            // Start a new coroutine for image uploads
                            CoroutineScope(Dispatchers.IO).launch {
                                saleManagerMethod.uploadChangedImages(
                                    saleFolder = response.saleFolder!!,
                                    changedIndexes = changedImageIndexes,
                                    imageViews = imageViews,
                                    imageUris = imageUris
                                )

                                Picasso.get().invalidate(imageUrl1)
                                Picasso.get().invalidate(imageUrl2)
                                Picasso.get().invalidate(imageUrl3)
                                Picasso.get().invalidate(imageUrl4)
                                Picasso.get().invalidate(imageUrl5)

                                withContext(Dispatchers.Main) {
                                    changedImageIndexes.clear()
                                    Toast.makeText(
                                        this@ModifySaleActivity,
                                        "Sale and images updated successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                this@ModifySaleActivity,
                                "Sale updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this@ModifySaleActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ModifySaleActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun fetchSalesDataSearch(apiService: ApiService, saleId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchImages(SearchRequestID(saleId))

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        response.data?.let { sale ->
                            displaySales(sale)
                        } ?: run {
                            // Handle case when data is null
                            resetImageViews()
                            Toast.makeText(
                                this@ModifySaleActivity,
                                "No product found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        resetImageViews()
                        Toast.makeText(
                            this@ModifySaleActivity,
                            response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resetImageViews()
                    Toast.makeText(
                        this@ModifySaleActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("ModifySaleActivity", "Error fetching sale data", e)
                }
            }
        }
    }

    private fun resetImageViews() {
        imageView1.setImageResource(R.drawable.baseline_add_40)
        imageView2.setImageResource(R.drawable.baseline_add_40)
        imageView3.setImageResource(R.drawable.baseline_add_40)
        imageView4.setImageResource(R.drawable.baseline_add_40)
        imageView5.setImageResource(R.drawable.baseline_add_40)
    }
    //For image reset, for the delete buttons
    private fun resetImageView(index: Int) {
        // Set default image
        getImageViewIndex(index).setImageResource(R.drawable.click_to_change)

        // Clear any stored URI for this image
        imageUris[index] = null

        // Mark this image as changed
        changedImageIndexes.add(index)

        // Set a flag that this image was deleted
        getImageViewIndex(index).tag = "deleted" // Add this line

        //Clear cache
        Picasso.get().invalidate(imageUrl1)
        Picasso.get().invalidate(imageUrl2)
        Picasso.get().invalidate(imageUrl3)
        Picasso.get().invalidate(imageUrl4)
        Picasso.get().invalidate(imageUrl5)
    }

    private fun displaySales(sale: SaleWithEverything) {
        val saleManager = SaleManagerMethod(this, apiService)

        saleManager.setSpinnerCategory(
            spinner = findViewById(R.id.mainCategory),
            category = sale.mainCategory,
            defaultPosition = 0 // Position of "Select" item
        )

        saleManager.updateSubcategories(
            categoryPosition = findViewById<Spinner>(R.id.mainCategory).selectedItemPosition,
            spinner = findViewById(R.id.subCategory)
        )

        saleManager.setSpinnerCategory(
            spinner = findViewById(R.id.subCategory),
            category = sale.subCategory
        )


        findViewById<TextView>(R.id.inputSaleName).text = sale.Name
        findViewById<TextView>(R.id.inputCost).text = sale.Cost.toString()
        findViewById<TextView>(R.id.inputDesc).text = sale.Description
        //TODO legyen szep

        val folder = sale.SaleFolder.takeIf { it.isNotEmpty() }
            ?.let { "http://10.0.2.2:3000/$it/" }
            ?: return
        imageUrl1 = folder + "image1.jpg"
        imageUrl2 = folder + "image2.jpg"
        imageUrl3 = folder + "image3.jpg"
        imageUrl4 = folder + "image4.jpg"
        imageUrl5 = folder + "image5.jpg"

        // Load images with error handling
        fun loadImage(imageView: ImageView, url: String) {
            Picasso.get()
                .load(url)
                .placeholder(R.drawable.baseline_loading_24)
                .error(R.drawable.click_to_change)
                .into(imageView)
        }

        loadImage(imageView1, imageUrl1)
        loadImage(imageView2, imageUrl2)
        loadImage(imageView3, imageUrl3)
        loadImage(imageView4, imageUrl4)
        loadImage(imageView5, imageUrl5)


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
        listOf(
            R.id.image1,
            R.id.image2,
            R.id.image3,
            R.id.image4,
            R.id.image5
        ).forEachIndexed { index, resId ->
            findViewById<ImageView>(resId).setOnClickListener {
                imageContracts[index].launch("image/*")
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            navigateBackToProfile()
        }

        findViewById<Button>(R.id.confirm).setOnClickListener {
            modifySale(apiService, saleId)
        }


        findViewById<Button>(R.id.deleteButtonCover).setOnClickListener {
            if (!saleManagerMethod.isDefaultImage(imageView1)) {
                resetImageView(0)
            }
        }

        findViewById<Button>(R.id.deleteButton1).setOnClickListener {
            if (!saleManagerMethod.isDefaultImage(imageView2)) {
                resetImageView(1)
            }
        }

        findViewById<Button>(R.id.deleteButton2).setOnClickListener {
            if (!saleManagerMethod.isDefaultImage(imageView3)) {
                resetImageView(2)
            }
        }

        findViewById<Button>(R.id.deleteButton3).setOnClickListener {
            if (!saleManagerMethod.isDefaultImage(imageView4)) {
                resetImageView(3)
            }
        }

        findViewById<Button>(R.id.deleteButton4).setOnClickListener {
            if (!saleManagerMethod.isDefaultImage(imageView5)) {
                resetImageView(4)
            }
        }

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