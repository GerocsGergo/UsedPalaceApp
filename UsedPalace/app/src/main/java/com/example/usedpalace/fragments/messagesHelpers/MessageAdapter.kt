package com.example.usedpalace.fragments.messagesHelpers

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.requests.SearchRequestID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.ApiService


class MessageAdapter(
    private var messages: List<MessageWithEverything>,
    private val apiService: ApiService,
    private val currentUserId: Int)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {



    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }




    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> SentMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
            )
            else -> ReceivedMessageViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_messages_received, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is SentMessageViewHolder -> bindSentMessage(holder, message)
            is ReceivedMessageViewHolder -> bindReceivedMessage(holder, message)
        }


    }




    private fun bindSentMessage(holder: SentMessageViewHolder, message: MessageWithEverything) {
        holder.messageText.text = message.content
        holder.messageTime.text = ChatHelper.formatMessageTime(message.sentAt)
    }

    private fun bindReceivedMessage(holder: ReceivedMessageViewHolder, message: MessageWithEverything) {
        holder.messageText.text = message.content
        holder.messageTime.text = ChatHelper.formatMessageTime(message.sentAt)
        CoroutineScope(Dispatchers.IO).launch {
            val username = ChatHelper.getUserName(apiService, message.senderId)
            withContext(Dispatchers.Main) {
                holder.senderName.text = username ?: "Unknown"
            }
        }
    }


    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<MessageWithEverything>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_me)
        val messageTime: TextView = itemView.findViewById(R.id.date_me)
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_other)
        val messageTime: TextView = itemView.findViewById(R.id.date_other)
        val senderName: TextView = itemView.findViewById(R.id.user_other)
        // val senderName: TextView = itemView.findViewById(R.id.sender_name) // Uncomment if needed
    }

    fun addMessage(newMessage: MessageWithEverything) {
        val newList = messages.toMutableList().apply {
            add(newMessage)
        }
        messages = newList
        notifyItemInserted(messages.size - 1)
    }
}

