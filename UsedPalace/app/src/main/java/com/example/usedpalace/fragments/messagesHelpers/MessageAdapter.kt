package com.example.usedpalace.fragments.messagesHelpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private var messages: List<MessageWithEverything>,
    private val currentUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    // ViewHolder for sent messages
    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_me)
        val messageTime: TextView = itemView.findViewById(R.id.date_me)
        //val messageStatus: TextView = itemView.findViewById(R.id.sent_message_status)
    }

    // ViewHolder for received messages
    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_other)
        val messageTime: TextView = itemView.findViewById(R.id.date_other)
        val senderName: TextView = itemView.findViewById(R.id.user_other)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_messages_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is SentMessageViewHolder -> {
                holder.messageText.text = message.messageText
                holder.messageTime.text = formatTimeOnly(message.sentAt)
            }
            is ReceivedMessageViewHolder -> {
                holder.messageText.text = message.messageText
                holder.messageTime.text = formatDateAndTime(message.sentAt)
            }
        }
    }

    private fun formatTimeOnly(date: Date?): String {
        return date?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: "Just now"
    }

    private fun formatDateAndTime(date: Date?): String {
        return date?.let {
            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            "${dateFormat.format(it)} at ${timeFormat.format(it)}"
        } ?: "Just now"
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isSentByMe(currentUserId)) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    fun updateMessages(newMessages: List<MessageWithEverything>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}