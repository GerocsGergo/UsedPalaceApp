package com.example.usedpalace.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.dataClasses.SaleWithSid
import com.example.usedpalace.fragments.homeFragmentHelpers.HomeFragmentSingleSaleActivity
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.example.usedpalace.requests.SearchRequestName
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService


class HomeFragment : Fragment() {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private val baseImageUrl = "http://10.224.83.75:3000/sales"

    //private val baseImageUrl = "http://10.0.2.2:3000"


    private lateinit var containerLayout: LinearLayout

    private lateinit var exampleText: TextView

    private lateinit var searchView: SearchView

    private lateinit var clearButton :Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        setupViews(view)
        initialize()
        setupClickListeners(inflater)

        fetchSalesData(apiService, containerLayout, inflater)

        return view
    }

    private fun setupViews(view: View) {
        containerLayout = view.findViewById(R.id.productList)
        clearButton = view.findViewById(R.id.clearSearchButton)
        exampleText = view.findViewById(R.id.forExample)
        searchView = view.findViewById(R.id.searchBar)
    }

    private fun initialize() {
        prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(requireContext().applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupClickListeners(inflater: LayoutInflater) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    containerLayout.removeAllViews()
                    fetchSalesDataSearch(apiService, containerLayout, inflater, query)
                    searchView.clearFocus()

                    clearButton.visibility = View.VISIBLE
                    exampleText.visibility = View.GONE
                } else if (query.trim().isEmpty()) {
                    //Toast.makeText(context, "Please enter a search term", Toast.LENGTH_SHORT).show()
                    clearButton.visibility = View.GONE
                    exampleText.visibility = View.VISIBLE
                    ErrorHandler.toaster(requireContext(), "Please enter a search term", Toast.LENGTH_SHORT)
                    return true
                }
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                //TODO realtime search?
                return false
            }
        })

        clearButton.setOnClickListener {
            searchView.setQuery("", false)
            searchView.clearFocus()
            containerLayout.removeAllViews()
            fetchSalesData(apiService, containerLayout, inflater)
            clearButton.visibility = View.GONE
            exampleText.visibility = View.VISIBLE
        }
    }


    private fun fetchSalesDataSearch(
        apiService: ApiService,
        containerLayout: LinearLayout?,
        inflater: LayoutInflater,
        searchParam: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear previous results
                withContext(Dispatchers.Main) {
                    containerLayout?.removeAllViews()
                }

                // Make API call
                val response = apiService.searchSales(SearchRequestName(searchParam))

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        if (response.data.isNotEmpty()) {
                            displaySales(response.data, containerLayout, inflater)
                        } else {
                            showNoProductsMessage(containerLayout, inflater, response.message)
                        }
                    } else {
                        //Toast.makeText(context, "Search failed: ${response.message}", Toast.LENGTH_SHORT).show()
                        ErrorHandler.handleApiError(requireContext(), null, response.message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showNoProductsMessage(containerLayout, inflater, "Connection error")
                    ErrorHandler.handleNetworkError(requireContext(), e)
                }
            }
        }
    }


    private fun showNoProductsMessage(
        containerLayout: LinearLayout?,
        inflater: LayoutInflater,
        message: String = "No products found"
    ) {
        containerLayout?.removeAllViews()
        val noProductsView = inflater.inflate(R.layout.show_error_message, containerLayout, false)
        noProductsView.findViewById<TextView>(R.id.messageText).text = message
        containerLayout?.addView(noProductsView)
    }

    private fun displaySales(sales: List<SaleWithSid>, containerLayout: LinearLayout?, inflater: LayoutInflater) {
        if (sales.isEmpty()) {
            // Create and show "No products found" message
            val noProductsView =
                inflater.inflate(R.layout.show_error_message, containerLayout, false)
            containerLayout?.addView(noProductsView)
        } else {
            for (sale in sales) {
                val itemView = inflater.inflate(R.layout.item_fragment_single_sale, containerLayout, false)

                itemView.findViewById<TextView>(R.id.productLabel).text = sale.Name
                itemView.findViewById<TextView>(R.id.productPrice).text = "${sale.Cost} Ft"
                itemView.findViewById<TextView>(R.id.productDescription).text = sale.Description

                val imageView: ImageView = itemView.findViewById(R.id.productImage) // <<< Ezt hozzá kell tenni

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val imageResponse = apiService.getSaleImages(GetSaleImagesRequest(sale.Sid))
                        withContext(Dispatchers.Main) {
                            if (imageResponse.success) {
                                val images = imageResponse.images ?: emptyList()
                                if (images.isNotEmpty()) {
                                    Picasso.get()
                                        .load(images.first()) // az első képet betöltöd
                                        .placeholder(R.drawable.baseline_loading_24)
                                        .error(R.drawable.baseline_error_24)
                                        .into(imageView)
                                } else {
                                    imageView.setImageResource(R.drawable.baseline_eye_40) // nincs kép
                                }
                            } else {
                                imageView.setImageResource(R.drawable.baseline_info_outline_40)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            imageView.setImageResource(R.drawable.baseline_home_filled_24)
                        }
                    }
                }

                containerLayout?.addView(itemView)

                itemView.setOnClickListener {
                    onProductClick(sale)
                }
            }

        }
    }

    private fun fetchSalesData(
        apiService: ApiService,
        containerLayout: LinearLayout?,
        inflater: LayoutInflater
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getSales()
                val sales = response.data

                withContext(Dispatchers.Main) {
                    containerLayout?.removeAllViews()

                    if (sales.isEmpty()) {
                        showNoProductsMessage(containerLayout, inflater, "No products found")
                        return@withContext
                    }

                    for (sale in sales) {
                        val itemView = inflater.inflate(R.layout.item_fragment_single_sale, containerLayout, false)
                        itemView.findViewById<TextView>(R.id.productLabel).text = sale.Name
                        itemView.findViewById<TextView>(R.id.productPrice).text = "${sale.Cost} Ft"
                        itemView.findViewById<TextView>(R.id.productDescription).text = sale.Description

                        val imageView = itemView.findViewById<ImageView>(R.id.productImage)

                        try {
                            val request = GetSaleImagesRequest(sale.Sid)
                            val imageResponse = apiService.getSaleImages(request)

                            withContext(Dispatchers.Main) {
                                if (imageResponse.success && !imageResponse.images.isNullOrEmpty()) {
                                    val imageUrl = imageResponse.images.first()
                                    Picasso.get()
                                        .load(imageUrl)
                                        .placeholder(R.drawable.baseline_loading_24)
                                        .error(R.drawable.baseline_error_24)
                                        .into(imageView)
                                } else {
                                    imageView.setImageResource(R.drawable.baseline_eye_40)
                                }
                            }
                        } catch (e: Exception) {
                            //Log.d("HomeFragment", "Error fetching images: ${e.message}")
                            ErrorHandler.handleApiError(requireContext(), null, e.message)
                            withContext(Dispatchers.Main) {
                                imageView.setImageResource(R.drawable.baseline_home_filled_24)
                            }
                        }

                        containerLayout?.addView(itemView)

                        itemView.setOnClickListener {
                            onProductClick(sale)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNoProductsMessage(containerLayout, inflater, "Connection error")
                }
            }
        }
    }




    private fun onProductClick(sale: SaleWithSid) {
        val saleId = sale.Sid
        val sellerId = sale.SellerId
        val intent = Intent(context, HomeFragmentSingleSaleActivity::class.java).apply {
            putExtra("SALE_ID", saleId) //Give the saleId to the activity
            putExtra("SELLER_ID", sellerId)
        }
        startActivity(intent)
    }

}
