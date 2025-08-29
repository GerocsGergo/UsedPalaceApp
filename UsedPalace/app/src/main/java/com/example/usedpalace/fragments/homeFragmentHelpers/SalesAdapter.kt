package com.example.usedpalace.fragments.homeFragmentHelpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.usedpalace.R
import com.example.usedpalace.dataClasses.SaleWithSid
import com.example.usedpalace.requests.GetSaleImagesRequest
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.RetrofitClient

class SalesAdapter(
    private val sales: MutableList<SaleWithSid>,
    private val onItemClick: (SaleWithSid) -> Unit
) : RecyclerView.Adapter<SalesAdapter.SaleViewHolder>() {

    inner class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.productLabel)
        val price: TextView = itemView.findViewById(R.id.productPrice)
        val desc: TextView = itemView.findViewById(R.id.productDescription)
        val image: ImageView = itemView.findViewById(R.id.productImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fragment_single_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val sale = sales[position]
        holder.name.text = sale.Name
        holder.price.text = "${sale.Cost} Ft"
        holder.desc.text = sale.Description

        // képek betöltése Picasso-val
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imageResponse = RetrofitClient.apiService.getSaleImages(GetSaleImagesRequest(sale.Sid))
                withContext(Dispatchers.Main) {
                    if (imageResponse.success && !imageResponse.images.isNullOrEmpty()) {
                        Picasso.get()
                            .load(imageResponse.images.first())
                            .placeholder(R.drawable.baseline_loading_24)
                            .error(R.drawable.baseline_error_24)
                            .into(holder.image)
                    } else {
                        holder.image.setImageResource(R.drawable.baseline_eye_40)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    holder.image.setImageResource(R.drawable.baseline_home_filled_24)
                }
            }
        }

        holder.itemView.setOnClickListener { onItemClick(sale) }
    }

    override fun getItemCount(): Int = sales.size

    fun setSales(newSales: List<SaleWithSid>) {
        sales.clear()
        sales.addAll(newSales)
        notifyDataSetChanged()
    }
}
