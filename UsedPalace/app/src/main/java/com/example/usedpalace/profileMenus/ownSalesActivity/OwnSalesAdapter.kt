package com.example.usedpalace.profileMenus.ownSalesActivity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class OwnSalesAdapter(
    val sales: MutableList<SaleWithSid>,
    private val onOpen: (SaleWithSid) -> Unit,
    private val onModify: (SaleWithSid) -> Unit,
    private val onDelete: (SaleWithSid) -> Unit
) : RecyclerView.Adapter<OwnSalesAdapter.SaleViewHolder>() {

    inner class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.productName)
        val image: ImageView = itemView.findViewById(R.id.productThumbnail)
        val open: Button = itemView.findViewById(R.id.open)
        val modify: Button = itemView.findViewById(R.id.modify)
        val delete: Button = itemView.findViewById(R.id.delete)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_own_sales, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val sale = sales[position]
        holder.name.text = sale.Name

        // kép betöltés
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getThumbnail(GetSaleImagesRequest(sale.Sid))
                withContext(Dispatchers.Main) {
                    if (response.success && response.thumbnail.isNotEmpty()) {
                        Picasso.get()
                            .load(response.thumbnail)
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

        // gombok eseménykezelése
        holder.open.setOnClickListener { onOpen(sale) }
        holder.modify.setOnClickListener { onModify(sale) }
        holder.delete.setOnClickListener { onDelete(sale) }
    }

    override fun getItemCount(): Int = sales.size

    fun setSales(newSales: List<SaleWithSid>) {
        sales.clear()
        sales.addAll(newSales)
        notifyDataSetChanged()
    }
}
