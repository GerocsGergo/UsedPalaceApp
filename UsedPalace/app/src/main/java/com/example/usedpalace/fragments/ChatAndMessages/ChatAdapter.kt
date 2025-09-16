package com.example.usedpalace.fragments.ChatAndMessages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.ErrorHandler
import com.example.usedpalace.R
import com.example.usedpalace.UserSession
import com.example.usedpalace.fragments.ChatAndMessages.ChatItem
import com.example.usedpalace.fragments.ChatAndMessages.ChatHelper
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.example.usedpalace.requests.SearchRequestID
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.RetrofitClient

class ChatAdapter(
    private val onChatClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val chatList = mutableListOf<ChatItem>()

    fun setData(newChats: List<ChatItem>) {
        chatList.clear()
        chatList.addAll(newChats)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fragment_messages, parent, false)
        return ChatViewHolder(view)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileName: TextView = itemView.findViewById(R.id.profile_name)
        private val productName: TextView = itemView.findViewById(R.id.product_name)
        private val lastMessageDate: TextView = itemView.findViewById(R.id.last_message_date)
        private val unreadDot: View = itemView.findViewById(R.id.unread_dot_product)
        private val imageView: ImageView = itemView.findViewById(R.id.image1)

        fun bind(chat: ChatItem) {
            lastMessageDate.text = ChatHelper.formatDateString(chat.lastMessageAt)
            unreadDot.visibility = if (chat.unreadCount > 0) View.VISIBLE else View.GONE

            profileName.text = chat.username?.takeIf { it != "Deleted User" } ?: "Ismeretlen"

            chat.saleId.let { saleId ->
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val saleResponse = withContext(Dispatchers.IO) {
                            RetrofitClient.apiService.searchSalesSID(SearchRequestID(saleId))
                        }

                        if (saleResponse.success && saleResponse.data != null) {
                            productName.text = saleResponse.data.Name

                            val imagesResponse = withContext(Dispatchers.IO) {
                                RetrofitClient.apiService.getSaleImages(GetSaleImagesRequest(saleId))
                            }

                            if (imagesResponse.success && imagesResponse.images.isNotEmpty()) {
                                Picasso.get()
                                    .load(imagesResponse.images.first())
                                    .placeholder(R.drawable.baseline_loading_24)
                                    .error(R.drawable.baseline_error_24)
                                    .into(imageView)
                            } else {
                                imageView.setImageResource(R.drawable.baseline_eye_40)
                            }
                        } else {
                            productName.text = "Eladott termék nem elérhető"
                            imageView.setImageResource(R.drawable.baseline_eye_40)
                        }
                    } catch (e: Exception) {
                        productName.text = "Hiba"
                        imageView.setImageResource(R.drawable.baseline_error_24)
                        ErrorHandler.logToLogcat(
                            "ChatAdapter",
                            "Hiba betöltés közben",
                            ErrorHandler.LogLevel.ERROR,
                            e
                        )
                    }
                }
            }

            itemView.setOnClickListener {
                onChatClick(chat)
            }
        }

    }
}
