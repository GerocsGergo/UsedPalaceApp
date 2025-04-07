package com.example.usedpalace.fragments

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.example.usedpalace.R
import com.example.usedpalace.Sale
import com.squareup.picasso.Picasso

class ProductPageFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_page, container, false)
        val sale = arguments?.getParcelable(ARG_SALE, Sale::class.java)

        // Populate your detail view
        sale?.let {
            view.findViewById<TextView>(R.id.productTitle).text = it.Name
            view.findViewById<TextView>(R.id.productPrice).text = "${it.Cost} Ft"
            view.findViewById<TextView>(R.id.productDescription).text = it.Description

            val imageUrl = "http://10.0.2.2:3000/${it.SaleFolder}/image1.jpg"
            val productImage = view.findViewById<ImageView>(R.id.productImage)

            Picasso.get()
                .load(imageUrl)
                .into(productImage)

//            val imageUrl2 = "http://10.0.2.2:3000/${it.SaleFolder}/image2.jpg"
//            val productImage2 = view.findViewById<ImageView>(R.id.image2)
//
//            Picasso.get()
//                .load(imageUrl2)
//                .into(productImage2)
//
//            val imageUrl3 = "http://10.0.2.2:3000/${it.SaleFolder}/image3.jpg"
//            val productImage3 = view.findViewById<ImageView>(R.id.image3)
//
//            Picasso.get()
//                .load(imageUrl3)
//                .into(productImage3)
//
//            val imageUrl4 = "http://10.0.2.2:3000/${it.SaleFolder}/image4.jpg"
//            val productImage4 = view.findViewById<ImageView>(R.id.image4)
//
//            Picasso.get()
//                .load(imageUrl4)
//                .into(productImage4)
//
//            val imageUrl5 = "http://10.0.2.2:3000/${it.SaleFolder}/image5.jpg"
//            val productImage5 = view.findViewById<ImageView>(R.id.image5)
//
//            Picasso.get()
//                .load(imageUrl5)
//                .into(productImage5)

        }

//        listOf(R.id.image2, R.id.image3, R.id.image4, R.id.image5).forEach { id ->
//            view.findViewById<ImageView>(id).apply {
//                isClickable = false
//                isFocusable = false
//            }
//        }

        return view
    }

    companion object {
        private const val ARG_SALE = "sale"

        fun newInstance(sale: Sale): ProductPageFragment {
            return ProductPageFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SALE, sale)
                }
            }
        }

}   }