
    package com.example.usedpalace.fragments.homefragmentHelpers

    import android.content.Context
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.usedpalace.R
    import com.squareup.picasso.Picasso

    class FullscreenGalleryAdapter(
        private val context: Context,
        private val imageUrls: List<String>
    ) : RecyclerView.Adapter<FullscreenGalleryAdapter.ImageViewHolder>() {

        class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.fullScreenImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_fullscreen_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val url = imageUrls[position]

            Picasso.get()
                .load(url)
                .placeholder(R.drawable.baseline_loading_24)
                .error(R.drawable.baseline_error_24)
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = imageUrls.size
    }

