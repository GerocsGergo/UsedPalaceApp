package com.example.usedpalace.fragments

import android.content.Intent
import android.os.Bundle
import android.service.controls.actions.ControlAction
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.ChatAndMessages.ChatAdapter
import com.example.usedpalace.fragments.ChatAndMessages.ChatItem
import com.example.usedpalace.fragments.ChatAndMessages.ChatActivity
import com.example.usedpalace.requests.SaveFcmTokenRequest
import com.example.usedpalace.requests.SearchRequestID
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import network.RetrofitClient

class MessagesFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var prevPageButton: Button
    private lateinit var nextPageButton: Button
    private lateinit var pageIndicator: TextView
    private lateinit var filterButton: Button

    private var currentPage = 1
    private var totalPages = 1
    private val limit = 10

    private var currentFilter: Int = R.id.filter_all // alapértelmezett

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewChats)
        prevPageButton = view.findViewById(R.id.prevPageButton)
        nextPageButton = view.findViewById(R.id.nextPageButton)
        pageIndicator = view.findViewById(R.id.pageIndicator)
        filterButton = view.findViewById(R.id.filterButton)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatAdapter = ChatAdapter { chat -> onChatClick(chat) }
        recyclerView.adapter = chatAdapter

        RetrofitClient.init(requireContext().applicationContext)
        apiService = RetrofitClient.apiService

        setupListeners()
        return view
    }

    override fun onResume() {
        super.onResume()
        currentPage = 1
        loadChatsPage()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val currentToken = task.result
                CoroutineScope(Dispatchers.IO).launch {
                    apiService.saveFcmToken(
                        SaveFcmTokenRequest(UserSession.getUserId()!!, currentToken)
                    )
                }
            }
        }
    }

    private fun setupListeners() {
        filterButton.setOnClickListener { v ->
            val popup = PopupMenu(requireContext(), v)
            popup.menuInflater.inflate(R.menu.chats_filter_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                currentFilter = item.itemId

                // Frissítjük a top title-t a filter alapján
                val titleTextView: TextView? = view?.findViewById(R.id.messagesTitle)
                titleTextView?.text = when(currentFilter) {
                    R.id.filter_unread -> "Olvasatlan üzeneteid"
                    else -> "Üzeneteid"
                }

                ErrorHandler.toaster(requireContext(), "Szűrés: ${item.title}")
                currentPage = 1
                loadChatsPage()
                true
            }
            popup.show()
        }

        prevPageButton.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                loadChatsPage()
            }
        }

        nextPageButton.setOnClickListener {
            if (currentPage < totalPages) {
                currentPage++
                loadChatsPage()
            }
        }
    }

    private fun onChatClick(chat: ChatItem) {
        CoroutineScope(Dispatchers.Main).launch {
            val username = fetchUsername(chat)
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("CHAT_ID", chat.chatId)
                putExtra("USERNAME", username)
                putExtra("SALE_ID", chat.saleId)
                putExtra("SELLER_ID", chat.sellerId)
            }
            startActivity(intent)
        }
    }

    private suspend fun fetchUsername(chat: ChatItem): String? {
        return withContext(Dispatchers.IO) {
            try {
                val userId = if (UserSession.getUserId() == chat.buyerId) chat.sellerId else chat.buyerId
                val response = apiService.searchUsername(SearchRequestID(userId))
                response.fullname
            } catch (e: Exception) {
                ErrorHandler.handleNetworkError(requireContext(),e)
                null
            }
        }
    }

    private fun loadChatsPage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = UserSession.getUserId()!!
                val response = when (currentFilter) {
                    R.id.filter_unread -> apiService.loadUnreadChats(userId, currentPage, limit)
                    else -> apiService.loadChats(userId, currentPage, limit)
                }

                if (response.success) {
                    val enrichedChats = response.data.mapNotNull { chat ->
                        val otherUserId = if (userId == chat.buyerId) chat.sellerId else chat.buyerId
                        val username = try {
                            apiService.searchUsername(SearchRequestID(otherUserId)).fullname
                        } catch (e: Exception) { "Ismeretlen" }

                        if (username == "Deleted User") return@mapNotNull null

                        val saleResponse = try {
                            apiService.searchSalesSID(SearchRequestID(chat.saleId))
                        } catch (e: Exception) { null }

                        if (saleResponse == null || !saleResponse.success || saleResponse.data == null) {
                            return@mapNotNull null
                        }

                        chat.copy(username = username)
                    }

                    withContext(Dispatchers.Main) {
                        totalPages = response.totalPages
                        pageIndicator.text = "$currentPage / $totalPages"
                        chatAdapter.setData(enrichedChats)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                        chatAdapter.setData(emptyList())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Hálózati hiba", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
