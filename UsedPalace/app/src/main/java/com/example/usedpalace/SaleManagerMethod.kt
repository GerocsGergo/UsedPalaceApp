package com.example.usedpalace.dataClasses

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.usedpalace.R
import network.ApiService

class SaleManagerMethod(private val context: Context, private val apiService: ApiService) {

    //region Category Management
    fun setupCategorySpinners(
        bigCategorySpinner: Spinner,
        smallCategorySpinner: Spinner,
        onCategorySelected: (position: Int) -> Unit
    ) {
        ArrayAdapter.createFromResource(
            context,
            R.array.big_categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            bigCategorySpinner.adapter = adapter
        }

        bigCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position > 0) updateSubcategories(position, smallCategorySpinner)
                onCategorySelected(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    fun updateSubcategories(categoryPosition: Int, spinner: Spinner) {
        val subcategoryArray = when (categoryPosition) {
            1 -> R.array.book_subcategories
            2 -> R.array.dvd_subcategories
            3 -> R.array.bluray_subcategories
            4 -> R.array.cd_subcategories
            5 -> R.array.console_subcategories
            6 -> R.array.games_subcategories
            else -> null
        }

        subcategoryArray?.let {
            ArrayAdapter.createFromResource(
                context,
                it,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
        } ?: run {
            spinner.adapter = null
        }
    }

    fun getSelectedCategories(bigCategorySpinner: Spinner, smallCategorySpinner: Spinner): Pair<String?, String?> {
        return Pair(
            bigCategorySpinner.takeIf { it.selectedItemPosition > 0 }?.selectedItem?.toString(),
            smallCategorySpinner.takeIf { it.adapter != null && it.selectedItemPosition >= 0 }?.selectedItem?.toString()
        )
    }

    fun setSpinnerCategory(
        spinner: Spinner,
        category: String?,
        defaultPosition: Int = 0
    ): Boolean {
        if (category == null) {
            spinner.setSelection(defaultPosition)
            return false
        }

        val adapter = spinner.adapter as? ArrayAdapter<*>
        if (adapter == null) {
            spinner.setSelection(defaultPosition)
            return false
        }

        for (i in 0 until adapter.count) {
            if (adapter.getItem(i).toString() == category) {
                spinner.setSelection(i)
                return true
            }
        }

        spinner.setSelection(defaultPosition)
        return false
    }

}
