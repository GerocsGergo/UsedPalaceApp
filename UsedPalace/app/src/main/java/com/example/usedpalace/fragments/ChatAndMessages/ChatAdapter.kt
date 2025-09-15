package com.example.usedpalace.fragments.ChatAndMessages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.squareup.picasso.Picasso

class ChatAdapter(
    private var chats: List<ChatItem>,
    private val onClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    fun updateData(newChats: List<ChatItem>) {
        chats = newChats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fragment_messages, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
        holder.itemView.setOnClickListener { onClick(chat) }
    }

    override fun getItemCount() = chats.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.profile_name)
        private val dateView: TextView = itemView.findViewById(R.id.last_message_date)
        private val productNameView: TextView = itemView.findViewById(R.id.product_name)
        private val imageView: ImageView = itemView.findViewById(R.id.image1)
        private val unreadDot: View = itemView.findViewById(R.id.unread_dot_product)

        fun bind(chat: ChatItem) {
            nameView.text = chat.username ?: "Ismeretlen felhasználó"
            dateView.text = ChatHelper.formatDateString(chat.lastMessageAt)
            productNameView.text = chat.productName ?: "Ismeretlen termék"

            unreadDot.visibility = if (chat.unreadCount > 0 && chat.isActive) View.VISIBLE else View.GONE

            if (chat.productImage != null) {
                Picasso.get()
                    .load(chat.productImage)
                    .placeholder(R.drawable.baseline_loading_24)
                    .error(R.drawable.baseline_error_24)
                    .into(imageView)
            } else {
                imageView.setImageResource(
                    if (chat.isActive) R.drawable.baseline_eye_40
                    else R.drawable.baseline_info_outline_40
                )
            }
        }
    }
}
