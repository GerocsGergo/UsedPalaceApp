package com.example.usedpalace.profileMenus.ownSalesActivity

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.profileMenus.SaleManagerHelper
import com.example.usedpalace.dataClasses.SaleWithEverything
import com.example.usedpalace.requests.DeleteImagesRequest
import com.example.usedpalace.requests.ModifySaleRequest
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class ModifySaleActivity : AppCompatActivity() {

    private lateinit var saleManagerHelper: SaleManagerHelper
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private var saleId: Int = -1
    //private val baseImageUrl = "http://10.224.83.75:3000/sales"
    private val baseImageUrl = "http://10.224.86.54:3000/sales" // Zsolti tablethez

    // Képkezelő listák
    private val oldImages = mutableListOf<Uri>()      // szerverről betöltött képek
    private val newImages = mutableListOf<Uri>()      // új képek
    private val deletedImages = mutableListOf<Uri>()  // törlésre kijelölt képek
    private val imageUris = mutableListOf<Uri>()      // adapter által használt kombinált lista

    private lateinit var imageAdapter: ImageAdapter

    companion object {
        private const val REQUEST_CODE_PICK_IMAGES = 2001
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

        saleId = intent.getIntExtra("SALE_ID", -1)
        if (saleId == -1) {
            Toast.makeText(this, "Invalid sale ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initialize()
        saleManagerHelper = SaleManagerHelper(this, apiService)

        setupUI()
        setupClickListeners()

        fetchSaleData()
    }

    private fun initialize() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupUI() {
        imageUris.clear()
        imageAdapter = ImageAdapter(imageUris) { position ->
            val removedUri = imageUris[position]
            if (oldImages.contains(removedUri)) {
                oldImages.remove(removedUri)
                deletedImages.add(removedUri)
            } else if (newImages.contains(removedUri)) {
                newImages.remove(removedUri)
            }
            refreshImageList()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.imageRecyclerView)
        recyclerView.adapter = imageAdapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        findViewById<Button>(R.id.btnAddImage).setOnClickListener {
            if (imageUris.size >= MAX_IMAGES) {
                Toast.makeText(this, "Maximum $MAX_IMAGES kép tölthető fel", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saleManagerHelper.pickImages(this, REQUEST_CODE_PICK_IMAGES)
        }

        saleManagerHelper.setupCategorySpinners(
            findViewById(R.id.mainCategory),
            findViewById(R.id.subCategory)
        ) { position ->
            if (position > 0) {
                saleManagerHelper.updateSubcategories(position, findViewById(R.id.subCategory))
            }
        }
    }

    private fun refreshImageList() {
        imageUris.clear()
        imageUris.addAll(oldImages)
        imageUris.addAll(newImages)
        imageAdapter.notifyDataSetChanged()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            val clipData = data.clipData
            val selectedUris = mutableListOf<Uri>()

            if (clipData != null) {
                val allowedToAdd = MAX_IMAGES - imageUris.size
                val count = clipData.itemCount
                val limit = if (count > allowedToAdd) allowedToAdd else count

                for (i in 0 until limit) {
                    val uri = clipData.getItemAt(i).uri
                    selectedUris.add(uri)
                }

                if (count > allowedToAdd) {
                    Toast.makeText(this, "Maximum $MAX_IMAGES kép tölthető fel", Toast.LENGTH_SHORT).show()
                }
            } else {
                data.data?.let { uri ->
                    if (imageUris.size < MAX_IMAGES) {
                        selectedUris.add(uri)
                    } else {
                        Toast.makeText(this, "Maximum $MAX_IMAGES kép tölthető fel", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            newImages.addAll(selectedUris)
            refreshImageList()
        }
    }


    private fun fetchSaleData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchSalesSID(SearchRequestID(saleId))
                withContext(Dispatchers.Main) {
                    if (response.success && response.data != null) {
                        displaySale(response.data)
                    } else {
                        Toast.makeText(this@ModifySaleActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ModifySaleActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displaySale(sale: SaleWithEverything) {
        findViewById<EditText>(R.id.inputSaleName).setText(sale.Name)
        findViewById<EditText>(R.id.inputCost).setText(sale.Cost.toString())
        findViewById<EditText>(R.id.inputDesc).setText(sale.Description)

        saleManagerHelper.setSpinnerCategory(findViewById(R.id.mainCategory), sale.mainCategory)
        saleManagerHelper.updateSubcategories(
            findViewById<Spinner>(R.id.mainCategory).selectedItemPosition,
            findViewById(R.id.subCategory)
        )
        saleManagerHelper.setSpinnerCategory(findViewById(R.id.subCategory), sale.subCategory)

        oldImages.clear()
        sale.Images?.forEach { imageName ->
            oldImages.add(Uri.parse("$baseImageUrl/${sale.SaleFolder}/$imageName"))
        }

        newImages.clear()
        deletedImages.clear()
        refreshImageList()
    }

    private fun setupClickListeners() {
        val modifyButton = findViewById<Button>(R.id.createSale)
        modifyButton.text = "Módosítás"
        modifyButton.setOnClickListener {
            modifySale(modifyButton)
        }

        findViewById<Button>(R.id.buttonBack).setOnClickListener {
            saleManagerHelper.navigateBackToProfile(this@ModifySaleActivity)
        }
    }



    private fun modifySale(modifyButton: Button) {
        val name = findViewById<EditText>(R.id.inputSaleName).text.toString().trim()
        val description = findViewById<EditText>(R.id.inputDesc).text.toString().trim()
        val cost = findViewById<EditText>(R.id.inputCost).text.toString().toIntOrNull() ?: 0

        val (bigCategory, smallCategory) = saleManagerHelper.getSelectedCategories(
            findViewById(R.id.mainCategory),
            findViewById(R.id.subCategory)
        )

        if (name.isEmpty() || description.isEmpty() || cost <= 0 || bigCategory == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Gomb inaktiválása a módosítás idejére
        modifyButton.isEnabled = false

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
                        deleteSaleImages(response.saleFolder!!, deletedImages)

                        // Feltöltés után visszatérés
                        saleManagerHelper.uploadImages(this@ModifySaleActivity,response.saleFolder!!, newImages) {
                            saleManagerHelper.showSuccessDialog(
                                this@ModifySaleActivity,
                                "Sikeres módosítás",
                                "A hirdetésed sikeresen módosult."
                            ) {
                                saleManagerHelper.navigateBackToProfile(this@ModifySaleActivity)
                            }
                        }
                    } else {
                        Toast.makeText(this@ModifySaleActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        modifyButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ModifySaleActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    modifyButton.isEnabled = true
                }
            }
        }
    }



    private fun deleteSaleImages(saleFolder: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Uri-ból az index kinyerése (image1.jpg -> 1)
                val deletedFileNames = deletedImages.mapNotNull { uri ->
                    uri.lastPathSegment?.substringAfterLast("/")  // vagy teljes fájlnevet
                }
                Log.d("Deleted file names", deletedFileNames[0])
                Log.d("Deleted file names", deletedFileNames[1])

                if (deletedFileNames.isNotEmpty()) {
                    apiService.deleteImages(DeleteImagesRequest(saleFolder, deletedFileNames))
                }
            } catch (_: Exception) { }
        }
    }

}
