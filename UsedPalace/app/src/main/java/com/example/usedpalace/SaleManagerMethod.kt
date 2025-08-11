package com.example.usedpalace

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Spinner
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.usedpalace.requests.DeleteSingleImageRequest
import com.example.usedpalace.responses.ResponseMessage
import network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStream

class SaleManagerMethod(private val context: Context, private val apiService: ApiService) {

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

    //endregion

    //region Image Upload


    private fun uploadImageUri(saleFolder: String, uri: Uri, imageIndex: Int) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val file = createTempImageFile("upload_${System.currentTimeMillis()}", stream)
            file?.let { uploadImageFile(saleFolder, it, imageIndex) }
        }
    }

    private fun uploadImageDrawable(saleFolder: String, drawable: Drawable, imageIndex: Int) {
        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> drawable.toBitmap()
        }
        bitmap?.let {
            val file = createTempImageFile("upload_${System.currentTimeMillis()}", it)
            file?.let { uploadImageFile(saleFolder, it, imageIndex) }
        }
    }

    private fun uploadImageFile(saleFolder: String, file: File, imageIndex: Int) {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
        val saleFolderPart = MultipartBody.Part.createFormData("saleFolder", saleFolder)
        val imageIndexPart = MultipartBody.Part.createFormData("imageIndex", imageIndex.toString())

        apiService.uploadImage(saleFolderPart, imageIndexPart, imagePart).enqueue(
            object : Callback<ResponseMessage> {
                override fun onResponse(call: Call<ResponseMessage>, response: Response<ResponseMessage>) {
                    file.delete()
                    if (!response.isSuccessful) {
                        Log.e("SaleManager", "Upload failed: ${response.errorBody()?.string()}")
                    }
                }
                override fun onFailure(call: Call<ResponseMessage>, t: Throwable) {
                    file.delete()
                    Log.e("SaleManager", "Upload error: ${t.message}")
                }
            }
        )
    }

    fun isDefaultImage(imageView: ImageView): Boolean {
        return try {
            val defaultAddIcon = ContextCompat.getDrawable(context, R.drawable.baseline_add_24)
            imageView.drawable?.constantState?.equals(defaultAddIcon?.constantState) == true
        } catch (e: Exception) {
            false
        }
    }


    suspend fun uploadChangedImages(
        saleFolder: String,
        changedIndexes: Set<Int>,
        imageViews: List<ImageView>,
        imageUris: List<Uri?>
    ) {
        changedIndexes.forEach { index ->
            try {
                when {
                    imageViews[index].tag == "deleted" -> {
                        // Try to delete, but don't fail if image doesn't exist
                        deleteSingleImage(saleFolder, index + 1)
                    }
                    imageUris[index] != null -> {
                        uploadImageUri(saleFolder, imageUris[index]!!, index + 1)
                    }
                }
            } catch (e: Exception) {
                Log.e("Upload", "Failed to process image at position ${index + 1}", e)
                // Continue with other images even if one fails
            }
        }
    }

    private suspend fun deleteSingleImage(saleFolder: String, imageIndex: Int) {
        try {
            val response = apiService.deleteSingleImage(
                DeleteSingleImageRequest(
                    saleFolder = saleFolder,
                    imageIndex = imageIndex
                )
            )

            if (!response.success) {
                // Only log as error if it's not a "not found" case
                if (!response.message.contains("not found", ignoreCase = true)) {
                    Log.e("ImageDelete", "Failed to delete image at index $imageIndex")
                }
            }
        } catch (e: Exception) {
            // Only log as error if it's not a 404
            if ((e as? retrofit2.HttpException)?.code() != 404) {
                Log.e("ImageDelete", "Error deleting image: ${e.message}")
            }
        }
    }


    //region Private Helpers
    private fun createTempImageFile(prefix: String, bitmap: Bitmap): File? {
        return try {
            File.createTempFile(prefix, ".jpg", context.cacheDir).apply {
                outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            }
        } catch (e: Exception) {
            Log.e("SaleManager", "File creation failed: ${e.message}")
            null
        }
    }

    private fun createTempImageFile(prefix: String, inputStream: InputStream): File? {
        return try {
            File.createTempFile(prefix, ".jpg", context.cacheDir).apply {
                outputStream().use { inputStream.copyTo(it) }
            }
        } catch (e: Exception) {
            Log.e("SaleManager", "File creation failed: ${e.message}")
            null
        }
    }
    //endregion
}