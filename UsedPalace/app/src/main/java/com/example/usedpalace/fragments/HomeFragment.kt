package com.example.usedpalace.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.usedpalace.R
import com.example.usedpalace.SaleWithSid
import com.example.usedpalace.fragments.homefragmentHelpers.HomeFragmentSingleSaleActivity
import com.example.usedpalace.requests.SearchRequestName
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class HomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Find the container inside the ScrollView
        val containerLayout = view.findViewById<LinearLayout>(R.id.productList)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // Use 10.0.2.2 for localhost in Android emulator
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)


        // Fetch data from the API
        fetchSalesData(apiService, containerLayout, inflater)

        val clearButton = view.findViewById<ImageButton>(R.id.clearSearchButton)
        val exampleText = view.findViewById<TextView>(R.id.forExample)


        val searchView = view.findViewById<SearchView>(R.id.searchBar)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isNotEmpty()) {
                    containerLayout.removeAllViews()
                    fetchSalesDataSearch(apiService, containerLayout, inflater, query)
                    // TODO Collapse the keyboard
                    searchView.clearFocus()

                    clearButton.visibility = View.VISIBLE
                    exampleText.visibility = View.GONE

                }else if (query.trim().isEmpty()) {
                    Toast.makeText(context, "Please enter a search term", Toast.LENGTH_SHORT).show()
                    return true
                }
                return true
            }



            override fun onQueryTextChange(newText: String): Boolean {
                //TODO realtime search? (ahogy irja a felhasz dobja ki a cuccokat)
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

        return view
    }

    //TODO retrofit

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
                        Toast.makeText(context, "Search failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showNoProductsMessage(containerLayout, inflater, "Connection error")
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
                val itemView = inflater.inflate(R.layout.item_home, containerLayout, false)

                itemView.findViewById<TextView>(R.id.productLabel).text = sale.Name
                itemView.findViewById<TextView>(R.id.productPrice).text = "${sale.Cost} Ft"
                itemView.findViewById<TextView>(R.id.productDescription).text = sale.Description

                val imageUrl = "http://10.0.2.2:3000/${sale.SaleFolder}/image1.jpg"
                val imageView: ImageView = itemView.findViewById(R.id.productImage)
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_loading_24)
                    .error(R.drawable.baseline_error_24)
                    .into(imageView)

                containerLayout?.addView(itemView)

                //Add event listener
                itemView.setOnClickListener {
                    onProductClick(sale)
                }
            }
        }
    }

    private fun fetchSalesData(apiService: ApiService, containerLayout: LinearLayout?, inflater: LayoutInflater) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch sales data from the API
                val sales = apiService.getSales()

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    for (sale in sales) {
                        // Inflate the item layout
                        val itemView = inflater.inflate(R.layout.item_home, containerLayout, false)

                        // Populate the views with data
                        itemView.findViewById<TextView>(R.id.productLabel).text = sale.Name
                        val costForView = "${sale.Cost} Ft"
                        itemView.findViewById<TextView>(R.id.productPrice).text = costForView
                        itemView.findViewById<TextView>(R.id.productDescription).text = sale.Description

                        // Load the first image from the SaleFolder
                        val imageUrl = "http://10.0.2.2:3000/${sale.SaleFolder}/image1.jpg" // Adjust the image path
                        val itemImageView = itemView.findViewById<ImageView>(R.id.productImage) //.setImageBitmap(myBitmap)

                        Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.baseline_loading_24) // Placeholder image while loading
                            .error(R.drawable.baseline_error_24) // Error image if loading fails
                            .into(itemImageView)


                        // Add the inflated view to the container
                        containerLayout?.addView(itemView)

                        //Add event listener
                        itemView.setOnClickListener {
                            onProductClick(sale)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
