package com.example.usedpalace.fragments

import android.content.Intent
import android.inputmethodservice.AbstractInputMethodService.AbstractInputMethodImpl
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.messagesHelpers.ChatActivity
import com.example.usedpalace.fragments.messagesHelpers.ChatHelper
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


class MessagesFragment : Fragment() {
    private lateinit var apiService: ApiService
    private var userId: Int = -1

    private lateinit var allChats: Button
    private lateinit var deletedChats: Button
    private lateinit var activeChats: Button

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

        allChats = view.findViewById(R.id.allChats)
        deletedChats = view.findViewById(R.id.deletedChats)
        activeChats  = view.findViewById(R.id.activeChats)

        setupClickListeners(apiService, containerLayout, inflater)
        fetchChats(apiService, containerLayout, inflater, "activeChats")

        return view
    }


    private fun setupRetrofit() {
        apiService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun setupClickListeners(apiService: ApiService, containerLayout: LinearLayout?, inflater: LayoutInflater) {
        allChats.setOnClickListener {
            fetchChats(apiService, containerLayout, inflater, "allChats")
        }

        deletedChats.setOnClickListener {
            fetchChats(apiService, containerLayout, inflater, "deletedChats")
        }

        activeChats.setOnClickListener {
            fetchChats(apiService, containerLayout, inflater, "activeChats")
        }
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

    private fun displayDeletedChats(apiService: ApiService, chats: List<ChatItem>, containerLayout: LinearLayout?, inflater: LayoutInflater){
        if (chats.isEmpty()) {
            showErrorMessage(containerLayout, inflater)
        } else {
            containerLayout?.removeAllViews()

            for (chat in chats) {
                val itemView = inflater.inflate(R.layout.item_fragment_messages, containerLayout, false)
                var username:String? = null

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                         username = if (userId == chat.buyerId){
                            fetchUsername(chat.sellerId)
                        }else{
                            fetchUsername(chat.buyerId)
                        }
                        if (username != null) {
                            itemView.findViewById<TextView>(R.id.profile_name).text = username
                        } else {
                            Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                        }

                    }catch (e: Exception){
                        Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = ChatHelper.formatDateString(chat.lastMessageAt)

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val deletedSale = withContext(Dispatchers.IO) {
                            Log.d("MessagesFragment", "Fetching sale for saleId: ${chat.saleId}")
                            apiService.searchDeletedSalesSID(SearchRequestID(chat.saleId))
                        }
                        if (deletedSale.success && deletedSale.data != null) {
                            val productName = "This sale has been deleted!"
                            itemView.findViewById<TextView>(R.id.product_name).text = productName
                            containerLayout?.addView(itemView)
                        } else {
                            val activeSale = withContext(Dispatchers.IO) {
                                Log.d("MessagesFragment", "Fetching sale for saleId: ${chat.saleId}")
                                apiService.searchSalesSID(SearchRequestID(chat.saleId))
                            }
                            if (activeSale.success && activeSale.data != null && username == "Deleted User") {
                                itemView.findViewById<TextView>(R.id.product_name).text = activeSale.data.Name

                                val folderName = activeSale.data.SaleFolder
                                val imageUrl = "http://10.0.2.2:3000/${folderName}/image1.jpg"
                                val imageView: ImageView = itemView.findViewById(R.id.image1)
                                Picasso.get()
                                    .load(imageUrl)
                                    .placeholder(R.drawable.baseline_loading_24)
                                    .error(R.drawable.baseline_error_24)
                                    .into(imageView)
                                containerLayout?.addView(itemView)

                                itemView.setOnClickListener {
                                    onProductClick(chat.chatId, username)
                                }
                            } else {
                                Log.e("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MessagesFragment", "Error loading chat details", e)
                        showErrorMessage(containerLayout, inflater, message = "server error")
                    }
                }



            }
        }
    }

    private fun displayActiveChats(apiService: ApiService, chats: List<ChatItem>, containerLayout: LinearLayout?, inflater: LayoutInflater){
        if (chats.isEmpty()) {

            showErrorMessage(containerLayout, inflater)
        } else {
            containerLayout?.removeAllViews()

            for (chat in chats) {
                val itemView = inflater.inflate(R.layout.item_fragment_messages, containerLayout, false)
                var username:String? = null
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        username = if (userId == chat.buyerId){
                            fetchUsername(chat.sellerId)

                        }else{
                            fetchUsername(chat.buyerId)

                        }
                        if (username != null) {
                            itemView.findViewById<TextView>(R.id.profile_name).text = username
                        } else {
                            Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                        }
                        itemView.setOnClickListener {
                            onProductClick(chat.chatId, username)
                        }
                    }catch (e: Exception){
                        Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = ChatHelper.formatDateString(chat.lastMessageAt)
                // Load sale info asynchronously
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val sale = withContext(Dispatchers.IO) {
                            Log.d("MessagesFragment", "Fetching sale for saleId: ${chat.saleId}")
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

                            if (username != "Deleted User") {
                                containerLayout?.addView(itemView)
                            }

                        } else {
                            Log.e("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                        }
                    } catch (e: Exception) {
                        Log.e("MessagesFragment", "Error loading chat details", e)
                        showErrorMessage(containerLayout, inflater, message = "server error")
                    }
                }
            }
        }
    }

    private fun displayChats(apiService: ApiService, chats: List<ChatItem>, containerLayout: LinearLayout?, inflater: LayoutInflater){
        if (chats.isEmpty()) {

            showErrorMessage(containerLayout, inflater)
        } else {
            containerLayout?.removeAllViews()

            for (chat in chats) {
                val itemView = inflater.inflate(R.layout.item_fragment_messages, containerLayout, false)
                var username:String? = null

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                         username = if (userId == chat.buyerId){
                            fetchUsername(chat.sellerId)

                        }else{
                            fetchUsername(chat.buyerId)

                        }
                        if (username != null) {
                            itemView.findViewById<TextView>(R.id.profile_name).text = username
                        } else {
                            Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                        }


                        itemView.setOnClickListener {
                            onProductClick(chat.chatId, username)
                        }
                    }catch (e: Exception){
                        Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = ChatHelper.formatDateString(chat.lastMessageAt)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val sale = withContext(Dispatchers.IO) {
                            Log.d("MessagesFragment", "Fetching sale for saleId: ${chat.saleId}")
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
                            Log.e("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                            itemView.findViewById<TextView>(R.id.product_name).text = "Warning! This sale has been deleted!"
                        }
                    } catch (e: Exception) {
                        Log.e("MessagesFragment", "Error loading chat details", e)
                        showErrorMessage(containerLayout, inflater, message = "server error")
                    }
                }

                containerLayout?.addView(itemView)

            }
        }
    }

    private fun onProductClick(chatId: Int, username: String?) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
            putExtra("USERNAME", username)
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


    private fun fetchChats(apiService: ApiService, containerLayout: LinearLayout?, inflater: LayoutInflater, flag: String) {

        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    containerLayout?.removeAllViews()
                }

                userId = UserSession.getUserId()!!
                Log.d("MessagesFragment", "Fetching chats for buyerId: $userId")

                val response = apiService.getAllChats(SearchChatRequest(userId))

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        if (response.data.isNotEmpty()) {

                            when (flag) {
                                "activeChats" -> displayActiveChats(apiService, response.data, containerLayout, inflater)
                                "deletedChats" -> displayDeletedChats(apiService, response.data, containerLayout, inflater)
                                "allChats" -> displayChats(apiService, response.data, containerLayout, inflater)
                                else -> displayActiveChats(apiService, response.data, containerLayout, inflater)
                            }

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
                Log.e("MessagesFragment fetch", "Network error", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showErrorMessage(containerLayout, inflater, "Connection error")
                }
            }
        }
    }





}


