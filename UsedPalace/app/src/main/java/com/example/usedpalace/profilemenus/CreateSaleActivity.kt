package com.example.usedpalace.profilemenus

import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
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
import com.example.usedpalace.dataClasses.SaleManagerMethod
import com.example.usedpalace.UserSession
import com.example.usedpalace.profilemenus.forownsalesactivity.ImageAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CreateSaleActivity : AppCompatActivity() {

    private lateinit var saleManagerMethod: SaleManagerMethod
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private val imageUris = mutableListOf<Uri?>()
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
        imageUris.clear()
        imageAdapter = ImageAdapter(imageUris) { position ->
            imageUris.removeAt(position)
            imageAdapter.notifyDataSetChanged()
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

        // Spinner beállítás (ahogy korábban)
        saleManagerMethod.setupCategorySpinners(
            findViewById(R.id.mainCategory),
            findViewById(R.id.subCategory)
        ) { position ->
            if (position > 0) {
                saleManagerMethod.updateSubcategories(position, findViewById(R.id.subCategory))
            }
        }
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
                    // Több kép kiválasztva
                    val count = clipData.itemCount
                    val limit = if (count > allowedToAdd) allowedToAdd else count
                    for (i in 0 until limit) {
                        val imageUri = clipData.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else {
                    // Egy kép kiválasztva
                    intentData.data?.let { uri ->
                        if (allowedToAdd > 0) {
                            imageUris.add(uri)
                        } else {
                            Toast.makeText(this, "Maximum $MAX_IMAGES kép tölthető fel", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                imageAdapter.notifyDataSetChanged()
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
        if (imageUris.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // text form field
                val saleFolderBody = saleFolder.toRequestBody("text/plain".toMediaTypeOrNull())

                // Build MultipartBody.Part list sequentially (suspend-safe)
                val imageParts = mutableListOf<MultipartBody.Part>()
                for (uri in imageUris) {
                    uri?.let {
                        val part = uriToMultipart(it, "images")
                        part?.let { imageParts.add(it) }
                    }
                }

                if (imageParts.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateSaleActivity, "Nincs feltölthető kép", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // API call: egyetlen hívásban az összes képpel
                val response = apiService.uploadSaleImages(saleFolderBody, imageParts)

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        Toast.makeText(this@CreateSaleActivity, "Images uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CreateSaleActivity, "Image upload failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateSaleActivity, "Error uploading images: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun uriToMultipart(uri: Uri, key: String): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
                val bytes = inputStream.readBytes()
                inputStream.close()

                val mimeType = getMimeType(uri) ?: "image/*"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

                // getFileName kell hogy legyen kiterjesztéssel
                val fileName = getFileName(uri) ?: "upload_${System.currentTimeMillis()}.jpg"
                MultipartBody.Part.createFormData(key, fileName, requestBody)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }



    private fun getMimeType(uri: Uri): String? {
        val contentResolver: ContentResolver = contentResolver
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }



    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex("_display_name")
                    if (index != -1) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }



    private fun clearForm() {
        findViewById<EditText>(R.id.inputSaleName).text.clear()
        findViewById<EditText>(R.id.inputDesc).text.clear()
        findViewById<EditText>(R.id.inputCost).text.clear()
        imageUris.clear()
        imageAdapter.notifyDataSetChanged()
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
