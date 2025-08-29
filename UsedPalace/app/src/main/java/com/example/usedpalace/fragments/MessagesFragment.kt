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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.fragment.app.Fragment
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import network.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.ChatAndMessages.ChatActivity
import com.example.usedpalace.fragments.ChatAndMessages.ChatHelper
import com.example.usedpalace.fragments.ChatAndMessages.ChatItem
import com.example.usedpalace.fragments.ChatAndMessages.Requests.SearchChatRequest
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.example.usedpalace.requests.SaveFcmTokenRequest
import com.example.usedpalace.requests.SearchRequestID
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService


class MessagesFragment : Fragment() {
    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences
    private var userId: Int = -1

    private lateinit var allChats: Button
    private lateinit var deletedChats: Button
    private lateinit var activeChats: Button
    private val baseImageUrl = "http://10.224.83.75:3000"

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

        setupViews(view)
        initialize()
        setupClickListeners(apiService, containerLayout, inflater)
        //fetchChats(apiService, containerLayout, inflater, "activeChats")

        return view
    }

    override fun onResume() {
        super.onResume()
        val containerLayout = view?.findViewById<LinearLayout>(R.id.container)
        containerLayout?.removeAllViews()
        fetchChats(apiService, containerLayout, layoutInflater, "activeChats")


        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentToken = task.result
                ErrorHandler.logToLogcat("firebase token","firebase token: $currentToken")
                // Küldjük el a szerverre
                CoroutineScope(Dispatchers.IO).launch {
                    apiService.saveFcmToken(SaveFcmTokenRequest(UserSession.getUserId()!!, currentToken))
                }
            }
        }
    }


    private fun setupViews(view: View) {
        allChats = view.findViewById(R.id.allChats)
        deletedChats = view.findViewById(R.id.deletedChats)
        activeChats  = view.findViewById(R.id.activeChats)


    }


    private fun initialize() {
        prefs = requireContext().getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(requireContext().applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun setupClickListeners(apiService: ApiService, containerLayout: LinearLayout?, inflater: LayoutInflater) {
        allChats.setOnClickListener {
            allChats.isEnabled = false
            fetchChats(apiService, containerLayout, inflater, "allChats")
            allChats.isEnabled = true
        }

        deletedChats.setOnClickListener {
            deletedChats.isEnabled = false
            fetchChats(apiService, containerLayout, inflater, "deletedChats")
            deletedChats.isEnabled = true
        }

        activeChats.setOnClickListener {
            activeChats.isEnabled = false
            fetchChats(apiService, containerLayout, inflater, "activeChats")
            activeChats.isEnabled = true
        }
    }

    private fun showErrorMessage(
        containerLayout: LinearLayout?,
        inflater: LayoutInflater,
        message: String = "Nem találhatóak üzenetek!"
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
                            //Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                            ErrorHandler.toaster(requireContext(), "Ismeretlen hiba történt", Toast.LENGTH_SHORT)
                        }

                    }catch (e: Exception){
                        //Toast.makeText(context, "Username not found", Toast.LENGTH_SHORT).show()
                        ErrorHandler.toaster(requireContext(), "Ismeretlen hiba történt", Toast.LENGTH_SHORT)

                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = ChatHelper.formatDateString(chat.lastMessageAt)

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val deletedSale = withContext(Dispatchers.IO) {
                            apiService.searchDeletedSalesSID(SearchRequestID(chat.saleId))
                        }
                        if (deletedSale.success && deletedSale.data != null) {
                            val productName = "This sale has been deleted!"
                            itemView.findViewById<TextView>(R.id.product_name).text = productName
                            containerLayout?.addView(itemView)
                        } else {
                            val activeSale = withContext(Dispatchers.IO) {
                                apiService.searchSalesSID(SearchRequestID(chat.saleId))
                            }
                            if (activeSale.success && activeSale.data != null && username == "Deleted User") {
                                itemView.findViewById<TextView>(R.id.product_name).text = activeSale.data.Name

                               val imageView: ImageView = itemView.findViewById(R.id.image1)
                                try {
                                    val imageResponse = apiService.getSaleImages(
                                        GetSaleImagesRequest(chat.saleId)
                                    )
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


                                containerLayout?.addView(itemView)

                                itemView.setOnClickListener {
                                    onProductClick(chat.chatId, username, chat.saleId, chat.sellerId)
                                }
                            } else {
                                //Log.e("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                                ErrorHandler.logToLogcat("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                                //ErrorHandler.handleApiError(requireContext(), null, "No chat found with this saleID: " + chat.saleId)
                            }
                        }
                    } catch (e: Exception) {
                        //Log.e("MessagesFragment", "Error loading chat details", e)
                        ErrorHandler.handleNetworkError(requireContext(),e)
                        showErrorMessage(containerLayout, inflater, message = "Ismeretlen hiba, kérjük próbáld újra később.")
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

                val unreadProductDot = itemView.findViewById<View>(R.id.unread_dot_product)

                 if (chat.unreadCount > 0) {
                    unreadProductDot.visibility = View.VISIBLE
                } else {
                    unreadProductDot.visibility = View.GONE
                }
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
                            ErrorHandler.toaster(requireContext(), "Ismeretlen hiba történt", Toast.LENGTH_SHORT)
                        }
                        itemView.setOnClickListener {
                            onProductClick(chat.chatId, username, chat.saleId, chat.sellerId)
                        }
                    }catch (e: Exception){
                        ErrorHandler.toaster(requireContext(), "Ismeretlen hiba történt", Toast.LENGTH_SHORT)
                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = ChatHelper.formatDateString(chat.lastMessageAt)
                // Load sale info asynchronously
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val sale = withContext(Dispatchers.IO) {
                            apiService.searchSalesSID(SearchRequestID(chat.saleId))
                        }
                        if (sale.success && sale.data != null) {
                            itemView.findViewById<TextView>(R.id.product_name).text = sale.data.Name

                            val imageView: ImageView = itemView.findViewById(R.id.image1)

                            try {
                                val imageResponse = apiService.getSaleImages(
                                    GetSaleImagesRequest(chat.saleId)
                                )
                                withContext(Dispatchers.Main) {
                                    if (imageResponse.success) {
                                        val images = imageResponse.images
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
                            if (username != "Deleted User") {
                                containerLayout?.addView(itemView)
                            }

                        } else {
                            //Log.e("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                            //ErrorHandler.handleApiError(requireContext(),null, "No chat found with this saleID: " + chat.saleId)
                            ErrorHandler.logToLogcat("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                        }
                    } catch (e: Exception) {
                        //Log.e("MessagesFragment", "Error loading chat details", e)
                        ErrorHandler.handleNetworkError(requireContext(),e)
                        showErrorMessage(containerLayout, inflater, message = "Ismeretlen hiba, kérjük próbáld újra később.")
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
                var username:String?


                val unreadProductDot = itemView.findViewById<View>(R.id.unread_dot_product)
                if (chat.unreadCount > 0) {
                    unreadProductDot.visibility = View.VISIBLE
                } else {
                    unreadProductDot.visibility = View.GONE
                }

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
                            ErrorHandler.toaster(requireContext(), "Ismeretlen hiba történt", Toast.LENGTH_SHORT)
                        }


                        itemView.setOnClickListener {
                            onProductClick(chat.chatId, username, chat.saleId, chat.sellerId)
                        }
                    }catch (e: Exception){
                        ErrorHandler.toaster(requireContext(), "Ismeretlen hiba történt", Toast.LENGTH_SHORT)
                    }
                }

                itemView.findViewById<TextView>(R.id.last_message_date).text = ChatHelper.formatDateString(chat.lastMessageAt)
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val sale = withContext(Dispatchers.IO) {
                            apiService.searchSalesSID(SearchRequestID(chat.saleId))
                        }
                        if (sale.success && sale.data != null) {
                            itemView.findViewById<TextView>(R.id.product_name).text = sale.data.Name

                            val imageView: ImageView = itemView.findViewById(R.id.image1)
                            try {
                                val imageResponse = apiService.getSaleImages(
                                    GetSaleImagesRequest(chat.saleId)
                                )
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
                        } else {
                            //Log.e("MessagesFragment", "No chat found with this saleID: " + chat.saleId)
                            //ErrorHandler.handleApiError(requireContext(),null, "No chat found with this saleID: " + chat.saleId)
                            ErrorHandler.logToLogcat("MessagesFragment", "No chat found with this saleID: " + chat.saleId)

                            itemView.findViewById<TextView>(R.id.product_name).text = "Warning! This sale has been deleted!"
                        }
                    } catch (e: Exception) {
                        //Log.e("MessagesFragment", "Error loading chat details", e)
                        ErrorHandler.handleNetworkError(requireContext(),e)
                        showErrorMessage(containerLayout, inflater, message = "Ismeretlen hiba, kérjük probáld újra később.")
                    }
                }

                containerLayout?.addView(itemView)

            }
        }
    }

    private fun onProductClick(chatId: Int, username: String?, saleId: Int, sellerId: Int) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("CHAT_ID", chatId)
            putExtra("USERNAME", username)
            putExtra("SALE_ID", saleId)
            putExtra("SELLER_ID", sellerId)
        }
        startActivity(intent)
    }

    private suspend fun fetchUsername(userId: Int): String? {

        return try {
            val response = apiService.searchUsername(SearchRequestID(userId))
            if (response.success && response.fullname != null) {
                response.fullname
            } else {
                //Log.e("Search", "Username not found: ${response.message}")
                ErrorHandler.logToLogcat("Search", "Username not found: ${response.message}", ErrorHandler.LogLevel.ERROR)
                null // Return null if not found
            }
        } catch (e: Exception) {
            //Log.e("Search", "Error fetching username", e)
            ErrorHandler.logToLogcat("Search", "Error fetching username", ErrorHandler.LogLevel.ERROR, e)
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

                val response = apiService.getAllChats(SearchChatRequest(userId))

                withContext(Dispatchers.Main) {
                    if (response.success) {
                        if (response.data.isNotEmpty()) {
                            //val sortedChats = response.data.sortedByDescending { it.lastMessageAt }
                            when (flag) {
                                "activeChats" -> displayActiveChats(apiService, response.data, containerLayout, inflater)
                                "deletedChats" -> displayDeletedChats(apiService, response.data, containerLayout, inflater)
                                "allChats" -> displayChats(apiService, response.data, containerLayout, inflater)
                                else -> displayActiveChats(apiService, response.data, containerLayout, inflater)
                            }

                        } else {
                            Log.e("MessagesFragment", "No chats found")
                            ErrorHandler.logToLogcat("MessagesFragment", "No chats found",ErrorHandler.LogLevel.ERROR)
                            showErrorMessage(containerLayout, inflater, response.message)
                        }
                    } else {
                        //Log.e("MessagesFragment", "API Error: ${response.message}")
                        ErrorHandler.handleApiError(requireContext(),null, response.message)
                        //Toast.makeText(context, "Search failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MessagesFragment fetch", "Network error", e)
                withContext(Dispatchers.Main) {
                    ErrorHandler.handleNetworkError(requireContext(),e)
                    showErrorMessage(containerLayout, inflater, "Connection error")
                }
            }
        }
    }





}


