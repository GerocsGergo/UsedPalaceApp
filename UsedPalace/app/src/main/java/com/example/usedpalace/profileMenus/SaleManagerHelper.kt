package com.example.usedpalace.profileMenus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.example.usedpalace.MainMenuActivity
import com.example.usedpalace.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class SaleManagerHelper(private val context: Context, private val apiService: ApiService) {

    //region Category Management
    fun setupCategorySpinners(
        bigCategorySpinner: Spinner,
        smallCategorySpinner: Spinner,
        onCategorySelected: (position: Int) -> Unit
    ) {
        ArrayAdapter.createFromResource(
            context,
            R.array.big_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            bigCategorySpinner.adapter = adapter
        }

        bigCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) updateSubcategories(position, smallCategorySpinner)
                onCategorySelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun updateSubcategories(categoryPosition: Int, spinner: Spinner) {
        val subcategoryArray = when (categoryPosition) {
            1 -> R.array.book_subcategories
            2 -> R.array.dvd_subcategories
            3 -> R.array.bluray_subcategories
            4 -> R.array.cd_subcategories
            5 -> R.array.console_subcategories
            6 -> R.array.games_subcategories
            else -> null
        }

        subcategoryArray?.let {
            ArrayAdapter.createFromResource(
                context,
                it,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
        } ?: run {
            spinner.adapter = null
        }
    }

    fun getSelectedCategories(bigCategorySpinner: Spinner, smallCategorySpinner: Spinner): Pair<String?, String?> {
        return Pair(
            bigCategorySpinner.takeIf { it.selectedItemPosition > 0 }?.selectedItem?.toString(),
            smallCategorySpinner.takeIf { it.adapter != null && it.selectedItemPosition >= 0 }?.selectedItem?.toString()
        )
    }

    fun setSpinnerCategory(
        spinner: Spinner,
        category: String?,
        defaultPosition: Int = 0
    ): Boolean {
        if (category == null) {
            spinner.setSelection(defaultPosition)
            return false
        }

        val adapter = spinner.adapter as? ArrayAdapter<*>
        if (adapter == null) {
            spinner.setSelection(defaultPosition)
            return false
        }

        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == category) {
                spinner.setSelection(i)
                return true
            }
        }

        spinner.setSelection(defaultPosition)
        return false
    }

    fun navigateBackToProfile(activity: Activity) {
        val intent = Intent(activity, MainMenuActivity::class.java).apply {
            putExtra("SHOW_PROFILE_FRAGMENT", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        activity.startActivity(intent)
        activity.finish()
    }


    //Képkezelés
    fun pickImages(activity: Activity, requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun handleActivityResult(
        data: Intent?,
        currentImages: MutableList<Uri>,
        maxImages: Int,
        context: Context
    ) {
        data?.let { intentData ->
            val clipData = intentData.clipData
            val allowedToAdd = maxImages - currentImages.size
            if (clipData != null) {
                val count = clipData.itemCount
                val limit = if (count > allowedToAdd) allowedToAdd else count
                for (i in 0 until limit) {
                    val imageUri = clipData.getItemAt(i).uri
                    currentImages.add(imageUri)
                }
            } else {
                intentData.data?.let { uri ->
                    if (allowedToAdd > 0) {
                        currentImages.add(uri)
                    } else {
                        Toast.makeText(context, "Maximum $maxImages kép tölthető fel", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun uriToMultipart(context: Context, uri: Uri, key: String): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.d("SaleImageHelper", "Failed to open InputStream for uri: $uri")
                    return@withContext null
                }

                val bytes = inputStream.readBytes()
                val mime = context.contentResolver.getType(uri) ?: "image/*"
                val requestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
                MultipartBody.Part.createFormData(key, "image.jpg", requestBody)
            } catch (e: Exception) {
                null
            }
        }
    }


    fun uploadImages(
        context: Context,
        saleFolder: String,
        uris: List<Uri>,
        onComplete: () -> Unit
    ) {
        if (uris.isEmpty()) {
            onComplete()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val saleFolderBody = saleFolder.toRequestBody("text/plain".toMediaTypeOrNull())
                val imageParts = mutableListOf<MultipartBody.Part>()

                for ((index, uri) in uris.withIndex()) {
                    val part = uriToMultipart(context, uri, "images")
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
                        Toast.makeText(context, "Image upload failed: ${response.message}", Toast.LENGTH_LONG).show()
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error uploading images: ${e.message}", Toast.LENGTH_LONG).show()
                    onComplete()
                }
            }
        }
    }

    fun showSuccessDialog(
        activity: Activity,
        title: String,
        message: String,
        onDismiss: () -> Unit
    ) {
        activity.runOnUiThread {
            val builder = androidx.appcompat.app.AlertDialog.Builder(activity)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss()
            }
            builder.setCancelable(false)
            builder.show()
        }
    }


}
