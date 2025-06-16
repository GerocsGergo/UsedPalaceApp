package com.example.usedpalace.fragments.messagesHelpers

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.messagesHelpers.Requests.SendMessageRequest
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    //private lateinit var webSocketClient: ChatWebSocketClient

    private var chatId: Int = -1
    private var toolbarUsername: String? = "null"

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var enterMessage: EditText
    private lateinit var buttonSend: ImageButton

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 3000L // 3 seconds

    private var lastMessageId: Int? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            loadMessages()
            handler.postDelayed(this, updateInterval)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRetrofit()
        initializeViews()
        getIntentData()
        setupToolbar(toolbarUsername)
        setupSendButton()
        initializeMessages()

    }

    private fun setupToolbar(toolbarUsername: String?){
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        supportActionBar?.title = toolbarUsername


        if (toolbarUsername.equals("Deleted User")){
            val message = "You cant send message to deleted user"
            enterMessage.hint = message
            enterMessage.isEnabled = false
            buttonSend.isEnabled = false

        }

    }

    private fun initializeViews() {
        mainLayout = findViewById(R.id.main)
        messagesRecyclerView = findViewById(R.id.messages_list_recycler_view)
        enterMessage = findViewById(R.id.enter_message)
        buttonSend = findViewById(R.id.button_send)

        val currentUserId = UserSession.getUserId()!!
        messageAdapter = MessageAdapter(emptyList(),apiService, currentUserId)

        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            if (!toolbarUsername.equals("Deleted User")){
                val messageText = enterMessage.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    sendMessage(messageText)
                    enterMessage.text.clear()
                }
            } else {
                enterMessage.text.clear()
                Toast.makeText(this, "You cant send message to deleted user.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }


    private fun sendMessage(content: String) {
        val currentUserId = UserSession.getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create request object
                val request = SendMessageRequest(
                    chatId = chatId,
                    senderId = currentUserId,
                    content = content
                )
                Log.d("before api call ","success: " + request.chatId +
                        " chat id: " + request.senderId
                        + "content: " + request.content)
                // Send to server
                val response = apiService.sendMessage(request)
                Log.d("after api call ","success: " + response.success + " message id: " + response.messageId)
                withContext(Dispatchers.Main) {
                    if (response.success) {
                        // Add message to local list immediately for fast UI response
                        val newMessage = MessageWithEverything(
                            messageId = response.messageId,
                            chatId = chatId,
                            senderId = currentUserId,
                            content = content,
                            sentAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME), // Current time
                        )

                        messageAdapter.addMessage(newMessage)
                        messagesRecyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                    } else {
                        val error = response.message
                        showErrorMessage("Failed to send message: $error")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorMessage("Network error: ${e.message}")
                }
            }
        }
    }

    private fun initializeMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getChatMessages(SearchRequestID(chatId))
                if (response.success) {
                    val messages = response.data
                    lastMessageId = messages.maxByOrNull { it.messageId }?.messageId
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

    private fun loadMessages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getChatMessages(SearchRequestID(chatId))
                if (response.success) {
                    val messages = response.data

                    val newestMessageId = messages.maxByOrNull { it.messageId }?.messageId

                    if (newestMessageId != null && newestMessageId != lastMessageId) {
                        lastMessageId = newestMessageId
                        withContext(Dispatchers.Main) {
                            messageAdapter.updateMessages(messages)
                            messagesRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                        }
                    } else {
                        // No new messages, do nothing or log if you want
                        Log.d("ChatActivity", "No new messages to update.")
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

        toolbarUsername = intent.getStringExtra("USERNAME")
        if (toolbarUsername.isNullOrEmpty()){
            showErrorMessage("Could not get username for enemy")
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
