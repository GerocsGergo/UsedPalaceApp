package com.example.usedpalace.fragments.homefragmentHelpers
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.squareup.picasso.Picasso

class ImageSliderAdapter(
    private val context: Context,
    private val imageUrls: MutableList<String> // fontos, hogy MutableList legyen
) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.sliderImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_slider_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val url = imageUrls[position]

        Picasso.get()
            .load(url)
            .placeholder(R.drawable.baseline_loading_24)
            .into(holder.imageView, object : com.squareup.picasso.Callback {
                override fun onSuccess() {
                    holder.imageView.visibility = View.VISIBLE
                }

                override fun onError(e: java.lang.Exception?) {
                    // Ha hiba van, távolítsuk el a listából, és értesítsük az adaptert
                    val adapterPosition = holder.adapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        // Fontos: UI thread-en hívjuk a notify-t
                        (holder.itemView.context as? android.app.Activity)?.runOnUiThread {
                            imageUrls.removeAt(adapterPosition)
                            notifyItemRemoved(adapterPosition)
                        }
                    }
                }
            })
    }

    override fun getItemCount(): Int = imageUrls.size
}
