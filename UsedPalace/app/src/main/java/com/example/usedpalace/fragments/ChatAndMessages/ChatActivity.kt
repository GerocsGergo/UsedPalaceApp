package com.example.usedpalace.fragments.ChatAndMessages

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.RetrofitClient
import com.example.usedpalace.UserSession
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService

class ChatActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var prefs: SharedPreferences

    private var chatId: Int = -1
    private var saleId: Int = -1
    private var sellerId: Int = -1
    private var toolbarUsername: String? = "null"

    private lateinit var mainLayout: ConstraintLayout
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var enterMessage: EditText
    private lateinit var saleItemTextView: TextView
    private lateinit var buttonSend: ImageButton

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private lateinit var webSocketClient: ChatWebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getIntentData()
        setupWebSocket()
        setupRetrofit()
        initializeViews()
        setupToolbar(toolbarUsername)
        initializeMessages()
        setupSendButton()
        getSaleItemText()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sale -> {
                intent = Intent(this, OpenSaleChatActivity::class.java).apply {
                    putExtra("SALE_ID", saleId) //Give the saleId to the activity
                    putExtra("SELLER_ID", sellerId)
                }
                startActivity(intent)
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getSaleItemText() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchSalesSID(SearchRequestID(saleId))

                val text = if (response.success && response.data != null) {
                    response.data.Name
                } else {
                    "Nincs termék"
                }

                withContext(Dispatchers.Main) {
                    saleItemTextView.text = text
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    saleItemTextView.text = "Hiba történt"
                }
            }
        }
    }


    private fun setupRetrofit() {
        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        RetrofitClient.init(applicationContext)
        apiService = RetrofitClient.apiService
    }

    private fun initializeViews() {
        mainLayout = findViewById(R.id.main)
        messagesRecyclerView = findViewById(R.id.messages_list_recycler_view)
        enterMessage = findViewById(R.id.enter_message)
        buttonSend = findViewById(R.id.button_send)
        saleItemTextView = findViewById(R.id.sale_item_text)

        val currentUserId = UserSession.getUserId() ?: -1
        Log.d("ChatActivity", "ITT NÉZD Current user ID: $currentUserId")
        Log.d("ChatActivity", "ITT NÉZD Current chat ID: $chatId")
        messageAdapter = MessageAdapter(emptyList(), apiService, currentUserId)

        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun getIntentData() {
        chatId = intent.getIntExtra("CHAT_ID", -1)
        if (chatId == -1) {
            showErrorMessage("Could not get chatID")
            finish()
        }
        saleId = intent.getIntExtra("SALE_ID", -1)
        if (saleId == -1) {
            showErrorMessage("Could not get saleID")
            finish()
        }
        sellerId = intent.getIntExtra("SELLER_ID", -1)
        if (sellerId == -1) {
            showErrorMessage("Could not get sellerID")
            finish()
        }

        toolbarUsername = intent.getStringExtra("USERNAME")
        if (toolbarUsername.isNullOrEmpty()) {
            showErrorMessage("Could not get username for enemy")
            finish()
        }
    }

    private fun setupToolbar(toolbarUsername: String?) {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = toolbarUsername

        toolbar.setNavigationOnClickListener {
            finish()
        }

        if (toolbarUsername == "Deleted User") {
            val message = "You can't send message to deleted user"
            enterMessage.hint = message
            enterMessage.isEnabled = false
            buttonSend.isEnabled = false
        }
    }

    private fun initializeMessages() {
        webSocketClient.requestMessages()
    }


    private fun setupWebSocket() {
        webSocketClient = ChatWebSocketClient(chatId,
            onMessageReceived = { message ->
                runOnUiThread {
                    messageAdapter.addMessage(message)
                    messagesRecyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                }
            },
            onMessagesReceived = { messages ->
                runOnUiThread {
                    messageAdapter.updateMessages(messages)
                    messagesRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        )
        webSocketClient.connect()
    }

    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            if (toolbarUsername != "Deleted User") {
                val messageText = enterMessage.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    sendMessage(messageText)
                    enterMessage.text.clear()
                }
            } else {
                enterMessage.text.clear()
                Toast.makeText(this, "You can't send message to deleted user.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(content: String) {
        val currentUserId = UserSession.getUserId() ?: return
        webSocketClient.sendMessage(currentUserId, content)
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
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
