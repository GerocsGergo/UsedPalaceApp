package com.example.usedpalace.fragments.messagesHelpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private var messages: List<MessageWithEverything>,
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
        holder.messageTime.text = formatMessageTime(message.sentAt)
    }

    private fun bindReceivedMessage(holder: ReceivedMessageViewHolder, message: MessageWithEverything) {
        holder.messageText.text = message.content
        holder.messageTime.text = formatMessageTime(message.sentAt)
        holder.senderName.text = message.senderId.toString()  //TODO NEVET IRJA KI
        // You can add sender name here if needed
    }

    private fun formatMessageTime(date: Date?): String {
        if (date == null) return "Just now"

        return try {
            val now = Date()
            val diff = now.time - date.time

            when {
                diff < 60 * 1000 -> "Just now"
                diff < 24 * 60 * 60 * 1000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                else -> SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
            }
        } catch (e: Exception) {
            "Just now"
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

