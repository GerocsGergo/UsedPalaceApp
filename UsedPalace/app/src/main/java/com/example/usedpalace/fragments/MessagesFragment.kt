package com.example.usedpalace.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.usedpalace.R
import com.example.usedpalace.SaleWithEverything
import com.example.usedpalace.SaleWithSid
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.messageHelpers.ChatItem
import com.example.usedpalace.fragments.messageHelpers.SearchChatRequest
import com.example.usedpalace.requests.SearchRequestID
import com.example.usedpalace.requests.SearchRequestName
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class MessagesFragment : Fragment() {
    private lateinit var apiService: ApiService
    private var buyerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)
        val containerLayout = view.findViewById<LinearLayout>(R.id.container)

        setupRetrofit()
        fetchChats(apiService, containerLayout, inflater)

        return view
    }


    private fun setupRetrofit() {
        apiService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun showErrorMessage(
        containerLayout: LinearLayout?,
        inflater: LayoutInflater,
        message: String = "No chats found"
    ) {
        containerLayout?.removeAllViews()
        val noChatsView = inflater.inflate(R.layout.show_error_message, containerLayout, false)
        noChatsView.findViewById<TextView>(R.id.messageText).text = message
        containerLayout?.addView(noChatsView)
    }

    private fun displayChats(apiService: ApiService, chats: List<ChatItem>, containerLayout: LinearLayout?, inflater: LayoutInflater){
        if (chats.isEmpty()) {

            showErrorMessage(containerLayout, inflater)
        } else {
            for (chat in chats) {
                val itemView = inflater.inflate(R.layout.item_fragment_messages, containerLayout, false)

                itemView.findViewById<TextView>(R.id.profile_name).text = chat.sellerId.toString() //TODO NEVET IRJA KI
                itemView.findViewById<TextView>(R.id.last_message_date).text = chat.lastMessageAt

                // Load sale info asynchronously
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val sale = withContext(Dispatchers.IO) {
                            apiService.searchSalesSID(SearchRequestID(chat.saleId))
                        }

                        if (sale.success && sale.data != null) {
                            itemView.findViewById<TextView>(R.id.product_name).text = sale.data.Name

                            val folderName = sale.data.SaleFolder
                            val imageUrl = "http://10.0.2.2:3000/${folderName}/image1.jpg"
                            val imageView: ImageView = itemView.findViewById(R.id.image1)
                            Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.baseline_loading_24)
                                .error(R.drawable.baseline_error_24)
                                .into(imageView)
                        } else {
                        }
                    } catch (e: Exception) {
                    }
                }

                containerLayout?.addView(itemView)

                //Add event listener
                itemView.setOnClickListener {
                    //onProductClick(sale)
                }
            }
        }
    }

    private fun fetchChats(apiService: ApiService, containerLayout: LinearLayout?, inflater: LayoutInflater) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    containerLayout?.removeAllViews()
                }
                buyerId = UserSession.getUserId()!!
                Log.d("MessagesFragment", "Fetching chats for buyerId: $buyerId") // Log request

                val response = apiService.getAllChats(SearchChatRequest(buyerId))
                Log.d("MessagesFragment", "API Response: ${response.success} - ${response.message}")

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        if (response.data.isNotEmpty()) {
                            displayChats(apiService, response.data, containerLayout, inflater)
                        } else {
                            Log.d("MessagesFragment", "No chats found")
                            showErrorMessage(containerLayout, inflater, response.message)
                        }
                    } else {
                        Log.e("MessagesFragment", "API Error: ${response.message}")
                        Toast.makeText(context, "Search failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MessagesFragment", "Network error", e) // This will log the full stack trace
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showErrorMessage(containerLayout, inflater, "Connection error")
                }
            }
        }
    }



}


