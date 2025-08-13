package com.example.usedpalace.profilemenus.forownsalesactivity

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
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.dataClasses.SaleManagerMethod
import com.example.usedpalace.dataClasses.SaleWithEverything
import com.example.usedpalace.requests.DeleteImagesRequest
import com.example.usedpalace.requests.ModifySaleRequest
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ModifySaleActivity : AppCompatActivity() {

    private lateinit var saleManagerMethod: SaleManagerMethod
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private var saleId: Int = -1
    private val baseImageUrl = "http://10.224.83.75:3000/sales"

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
        saleManagerMethod = SaleManagerMethod(this, apiService)

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
            pickImages()
        }

        saleManagerMethod.setupCategorySpinners(
            findViewById(R.id.mainCategory),
            findViewById(R.id.subCategory)
        ) { position ->
            if (position > 0) {
                saleManagerMethod.updateSubcategories(position, findViewById(R.id.subCategory))
            }
        }
    }

    private fun refreshImageList() {
        imageUris.clear()
        imageUris.addAll(oldImages)
        imageUris.addAll(newImages)
        imageAdapter.notifyDataSetChanged()
    }

    private fun pickImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK) {
            data?.let { intentData ->
                val clipData = intentData.clipData
                val allowedToAdd = MAX_IMAGES - imageUris.size
                if (clipData != null) {
                    val count = clipData.itemCount
                    val limit = if (count > allowedToAdd) allowedToAdd else count
                    for (i in 0 until limit) {
                        val imageUri = clipData.getItemAt(i).uri
                        newImages.add(imageUri)
                    }
                } else {
                    intentData.data?.let { uri ->
                        if (allowedToAdd > 0) {
                            newImages.add(uri)
                        } else {
                            Toast.makeText(this, "Maximum $MAX_IMAGES kép tölthető fel", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                refreshImageList()
            }
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

        saleManagerMethod.setSpinnerCategory(findViewById(R.id.mainCategory), sale.mainCategory)
        saleManagerMethod.updateSubcategories(
            findViewById<Spinner>(R.id.mainCategory).selectedItemPosition,
            findViewById(R.id.subCategory)
        )
        saleManagerMethod.setSpinnerCategory(findViewById(R.id.subCategory), sale.subCategory)

        val folderUrl = sale.SaleFolder.takeIf { it.isNotEmpty() }
            ?.let { "$baseImageUrl/$it" }
            ?: return

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
            navigateBackToProfile()
        }
    }



    private fun modifySale(modifyButton: Button) {
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
                        uploadImages(response.saleFolder!!, newImages) {
                            showSuccessDialog {
                                navigateBackToProfile()
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

    private fun uploadImages(saleFolder: String, uris: List<Uri>, onComplete: () -> Unit) {
        if (uris.isEmpty()) {
            onComplete()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val saleFolderBody = saleFolder.toRequestBody("text/plain".toMediaTypeOrNull())
                val imageParts = mutableListOf<MultipartBody.Part>()

                for ((index, uri) in uris.withIndex()) {
                    val part = uriToMultipart(uri, "images")
                    if (part != null) {
                        val uniquePart = MultipartBody.Part.createFormData(
                            "images",
                            "image_${System.currentTimeMillis()}_$index.jpg",
                            part.body
                        )
                        imageParts.add(uniquePart)
                    }
                }

                if (imageParts.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete() }
                    return@launch
                }

                val response = apiService.uploadSaleImages(saleFolderBody, imageParts)
                withContext(Dispatchers.Main) {
                    if (!response.success) {
                        Toast.makeText(this@ModifySaleActivity, "Image upload failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ModifySaleActivity, "Error uploading images: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }


    private suspend fun uriToMultipart(uri: Uri, key: String): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.d("UploadImages", "Failed to open InputStream for uri: $uri")
                    return@withContext null
                }

                val bytes = inputStream.readBytes()
                val mime = contentResolver.getType(uri) ?: "image/*"
                val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData(key, "image.jpg", requestBody)
            } catch (e: Exception) {
                null
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

    private fun navigateBackToProfile() {
        val intent = Intent(this, MainMenuActivity::class.java).apply {
            putExtra("SHOW_PROFILE_FRAGMENT", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun showSuccessDialog(onDismiss: () -> Unit) {
        runOnUiThread {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Sikeres módosítás")
            builder.setMessage("A hirdetésed sikeresen módosítva lett.")
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            builder.setCancelable(false)
            builder.show()
        }
    }
}
