package com.example.usedpalace.fragments.messagesHelpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class ChatWebSocketClientNOTUSED(
    private val context: Context,
    private val chatId: Int,
    private val onMessageReceived: (MessageWithEverything) -> Unit
) : WebSocketClient(URI("ws://10.0.2.2:3000/ws/chat/$chatId")) {

    override fun onOpen(handshakedata: ServerHandshake?) {
        Log.d("WebSocket", "Connection opened")
    }

    override fun onMessage(message: String) {
        try {
            val newMessage = Gson().fromJson(message, MessageWithEverything::class.java)
            Handler(Looper.getMainLooper()).post {
                onMessageReceived(newMessage)
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Error parsing message", e)
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        Log.d("WebSocket", "Connection closed: $reason")
    }

    override fun onError(ex: Exception) {
        Log.e("WebSocket", "Error", ex)
    }
}