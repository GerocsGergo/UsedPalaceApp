package com.example.usedpalace.profilemenus

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
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
import com.example.usedpalace.requests.CreateSaleRequest
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


class CreateSaleActivity : AppCompatActivity() {

    private lateinit var saleManagerMethod: SaleManagerMethod
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
                    val count = clipData.itemCount
                    val limit = if (count > allowedToAdd) allowedToAdd else count
                    for (i in 0 until limit) {
                        val imageUri = clipData.getItemAt(i).uri
                        imageUris.add(imageUri)
                    }
                } else {
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
        val buttonBack = findViewById<Button>(R.id.buttonBack)
            buttonBack.setOnClickListener {
            navigateBackToProfile()
        }
        val createButton = findViewById<Button>(R.id.createSale)
            createButton.setOnClickListener {
            createSale(createButton)
        }
    }

    private fun createSale(createButton: Button) {
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
                        // Feltöltjük a képeket az új mappába
                        uploadImages(response.saleFolder!!, imageUris) {
                            showSuccessDialog {
                                navigateBackToProfile()
                            }
                        }
                    } else {
                        Toast.makeText(this@CreateSaleActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        createButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateSaleActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    createButton.isEnabled = true
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
                        Toast.makeText(this@CreateSaleActivity, "Image upload failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateSaleActivity, "Error uploading images: ${e.message}", Toast.LENGTH_LONG).show()
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
            builder.setMessage("A hirdetésed sikeresen létre lett hozva.")
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            builder.setCancelable(false)
            builder.show()
        }
    }
}
