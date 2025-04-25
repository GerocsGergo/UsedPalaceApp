package com.example.usedpalace.fragments

import android.content.Intent
import android.os.Build
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
import androidx.annotation.RequiresApi
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.messagesHelpers.ChatActivity
import com.example.usedpalace.fragments.messagesHelpers.ChatItem
import com.example.usedpalace.fragments.messagesHelpers.Requests.SearchChatRequest
import com.example.usedpalace.requests.SearchRequestID
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
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayChats(apiService: ApiService, chats: List<ChatItem>, containerLayout: LinearLayout?, inflater: LayoutInflater){
        if (chats.isEmpty()) {

            showErrorMessage(containerLayout, inflater)
        } else {
            containerLayout?.removeAllViews()

            for (chat in chats) {
                val itemView = inflater.inflate(R.layout.item_fragment_messages, containerLayout, false)

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        if (userId == chat.buyerId){
                            val username= fetchUsername(chat.sellerId)
                            itemView.findViewById<TextView>(R.id.profile_name).text = username

                        }else{
                            val username= fetchUsername(chat.buyerId)
                            itemView.findViewById<TextView>(R.id.profile_name).text = username
                        }
                    }catch (e: Exception){
                        Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = formatDate(chat.lastMessageAt)
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
                            Log.e("MessagesFragment", "No chat found")
                            showErrorMessage(containerLayout, inflater, message = "No chat found")

                        }
                    } catch (e: Exception) {
                        Log.e("MessagesFragment", "Error loading chat details", e)
                        showErrorMessage(containerLayout, inflater, message = "server error")
                    }
                }

                containerLayout?.addView(itemView)

                //Add event listener
                itemView.setOnClickListener {
                    onProductClick(chat.chatId)
                }
            }
        }
    }

    private fun onProductClick(chatId: Int){
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
        }
        startActivity(intent)
    }

    private suspend fun fetchUsername(userId: Int): String? {

        return try {
            val response = apiService.searchUsername(SearchRequestID(userId))
            if (response.success && response.fullname != null) {
                response.fullname
            } else {
                Log.e("Search", "Username not found: ${response.message}")
                null // Return null if not found
            }
        } catch (e: Exception) {
            Log.e("Search", "Error fetching username", e)
            null // Return null on error
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDate(dateString: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm", Locale.getDefault())
            LocalDateTime.parse(dateString).format(formatter)
        } catch (e: Exception) {
            dateString // Return raw string if parsing fails
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchChats(apiService: ApiService, containerLayout: LinearLayout?, inflater: LayoutInflater) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    containerLayout?.removeAllViews()
                }
                userId = UserSession.getUserId()!!
                Log.d("MessagesFragment", "Fetching chats for buyerId: $userId") // Log request

                val response = apiService.getAllChats(SearchChatRequest(userId))
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
                Log.e("MessagesFragment fetch", "Network error", e) // This will log the full stack trace
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showErrorMessage(containerLayout, inflater, "Connection error")
                }
            }
        }
    }



}


