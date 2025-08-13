package com.example.usedpalace.profilemenus.forownsalesactivity

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.squareup.picasso.Picasso

// Egyszerű Adapter a képek megjelenítéséhez RecyclerView-ban
class ImageAdapter(
    private val images: MutableList<Uri?>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = images[position]
        if (uri != null) {
            val url = uri.toString()
            if (url.startsWith("http")) {
                // Internetes kép betöltése Picasso-val
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.baseline_add_24)
                    .error(R.drawable.baseline_add_24)
                    .into(holder.imageView)
            } else {
                // Helyi fájl (pl. galéria)
                holder.imageView.setImageURI(uri)
            }
        } else {
            holder.imageView.setImageResource(R.drawable.baseline_add_24)
        }

        holder.btnRemove.setOnClickListener {
            onDeleteClicked(position)
        }
    }
}
