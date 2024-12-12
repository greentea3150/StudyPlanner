package com.example.studyplanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.R
import com.example.studyplanner.model.Category

class CategoryAdapter(
    private val categories: List<Category>,
    private val onDeleteCategory: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardViewCategory)
        val textViewName: TextView = view.findViewById(R.id.textViewCategoryName)
        val buttonDelete: Button = view.findViewById(R.id.buttonDeleteCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.textViewName.text = category.name
        holder.cardView.setCardBackgroundColor(category.color)

        // Handle delete button click
        holder.buttonDelete.setOnClickListener {
            onDeleteCategory(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}
