package com.example.usedpalace.fragments.messagesHelpers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService

    private var chatId: Int = -1

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var enterMessage: EditText
    private lateinit var buttonSend: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getIntentData()
        setupRetrofit()
        initializeViews()
        loadMessages()

    }



    private fun initializeViews() {
        mainLayout = findViewById(R.id.main)
        messagesRecyclerView = findViewById(R.id.messages_list_recycler_view)
        enterMessage = findViewById(R.id.enter_message)
        buttonSend = findViewById(R.id.button_send)

        // Initialize adapter with current user ID (you need to get this from your auth system)
        val currentUserId = UserSession.getUserId()!!
        messageAdapter = MessageAdapter(emptyList(), currentUserId)

        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true // Makes the list start from the bottom
            }
            adapter = messageAdapter
        }
    }

    private fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getChatMessages(SearchRequestID(chatId))
                if (response.success) {
                    val messages = response.data
                    withContext(Dispatchers.Main) {
                        messageAdapter.updateMessages(messages)
                        messagesRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showErrorMessage("Failed to load messages: ${response.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorMessage("Network error: ${e.message}")
                }
            }
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()

        finish()
    }

    private fun setupRetrofit() {
        apiService = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun getIntentData() {
        chatId = intent.getIntExtra("CHAT_ID", -1)
        if (chatId == -1) {
            showErrorMessage("Could not get chatID")
            finish()
        }
    }


    private fun showErrorMessage(message: String) {
        mainLayout.removeAllViews()
        val errorView = layoutInflater.inflate(
            R.layout.show_error_message,
            mainLayout,
            false
        )

        errorView.findViewById<TextView>(R.id.messageText).text = message
        mainLayout.addView(errorView)
    }
}