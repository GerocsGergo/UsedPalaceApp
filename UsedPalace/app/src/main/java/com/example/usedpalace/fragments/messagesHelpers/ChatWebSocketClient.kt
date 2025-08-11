package com.example.usedpalace.fragments.messagesHelpers

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class ChatWebSocketClient(
    private val chatId: Int,
    private val onMessageReceived: (MessageWithEverything) -> Unit,
    private val onError: (String) -> Unit
) : WebSocketListener() {

    private val client = OkHttpClient()
    private lateinit var webSocket: WebSocket

    fun connect() {
        val request = Request.Builder()
            .url("ws://yourserver:8080") // cseréld a saját websocket URL-re
            .build()
        webSocket = client.newWebSocket(request, this)

        // Csatlakozás a chathez (küldj egy join-chat üzenetet JSON-ban)
        val joinMessage = """{"type":"join-chat", "chatId":$chatId}"""
        webSocket.send(joinMessage)
    }

    fun sendMessage(senderId: Int, content: String) {
        val msgJson = """
            {
              "type": "send-message",
              "chatId": $chatId,
              "senderId": $senderId,
              "content": "${content.replace("\"", "\\\"")}"
            }
        """.trimIndent()
        webSocket.send(msgJson)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        // parse JSON üzenet a szervertől, például új üzenet érkezett
        try {
            val json = JSONObject(text)
            if (json.getString("type") == "new-message") {
                val message = MessageWithEverything(
                    messageId = json.getInt("messageId"),
                    chatId = json.getInt("chatId"),
                    senderId = json.getInt("senderId"),
                    content = json.getString("content"),
                    sentAt = json.getString("sentAt")
                )
                onMessageReceived(message)
            }
        } catch (e: Exception) {
            onError("Invalid message format: ${e.message}")
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
        onError("WebSocket failure: ${t.message}")
    }

    fun close() {
        webSocket.close(1000, "Closing")
        client.dispatcher.executorService.shutdown()
    }
}
