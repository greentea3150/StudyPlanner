package com.example.studyplanner

import ColorAdapter
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.adapter.CategoryAdapter
import com.example.studyplanner.model.Category
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CategoryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()
    private var selectedColor: Int = Color.WHITE

    private val presetColors = listOf(
        Color.parseColor("#FFB3B3"), // Light Red
        Color.parseColor("#A8D0FF"), // Light Blue
        Color.parseColor("#B3E6B3"), // Light Green
        Color.parseColor("#FFFFB3"), // Light Yellow
        Color.parseColor("#A0FFFF"), // Light Cyan
        Color.parseColor("#F2A7D4"), // Light Magenta
        Color.parseColor("#D3D3D3"), // Lighter Gray
        Color.parseColor("#F0F0F0"), // Lighter Gray
        Color.parseColor("#FFD699"), // Lighter Orange
        Color.parseColor("#E0B3FF")  // Lighter Purple
    )


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        val editTextCategoryName = view.findViewById<EditText>(R.id.editTextCategoryName)
        val buttonPickColor = view.findViewById<Button>(R.id.buttonPickColor)
        val buttonSaveCategory = view.findViewById<Button>(R.id.buttonSaveCategory)
        val colorPreview = view.findViewById<View>(R.id.selectedColorPreview)
        val recyclerViewCategories = view.findViewById<RecyclerView>(R.id.recyclerViewCategories)

        firestore = FirebaseFirestore.getInstance()

        // Initialize RecyclerView
        categoryAdapter = CategoryAdapter(categories) { category ->
            deleteCategory(category)
        }
        recyclerViewCategories.layoutManager = LinearLayoutManager(context)
        recyclerViewCategories.adapter = categoryAdapter

        // Load existing categories
        loadCategories()

        // Pick a color
        buttonPickColor.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                colorPreview.setBackgroundColor(selectedColor)
            }
        }

        // Save category to Firestore
        buttonSaveCategory.setOnClickListener {
            val categoryName = editTextCategoryName.text.toString().trim()

            if (categoryName.isEmpty()) {
                Snackbar.make(view, "Category name cannot be empty!", Snackbar.LENGTH_SHORT).show()
            } else {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val userId = currentUser.uid
                    val category = hashMapOf(
                        "name" to categoryName,
                        "color" to selectedColor,
                        "userId" to userId
                    )

                    firestore.collection("Categories").add(category)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Category saved successfully!", Toast.LENGTH_SHORT).show()
                            editTextCategoryName.text.clear()
                            colorPreview.setBackgroundColor(Color.WHITE)
                            loadCategories() // Refresh list
                        }
                        .addOnFailureListener {
                            Snackbar.make(view, "Failed to save category.", Snackbar.LENGTH_SHORT).show()
                        }
                } else {
                    Snackbar.make(view, "User not authenticated!", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun showColorPickerDialog(onColorSelected: (Int) -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewColors)
        recyclerView.layoutManager = GridLayoutManager(context, 5) // 5 columns
        recyclerView.adapter = ColorAdapter(presetColors) { color ->
            onColorSelected(color)
            builder.create().dismiss()
        }

        builder.setView(dialogView)
        builder.show()
    }

    private fun loadCategories() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            firestore.collection("Categories")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { result ->
                    categories.clear()
                    for (document in result) {
                        val category = document.toObject(Category::class.java).copy(id = document.id)
                        categories.add(category)
                    }
                    categoryAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Snackbar.make(requireView(), "Failed to load categories.", Snackbar.LENGTH_SHORT).show()
                }
        } else {
            Snackbar.make(requireView(), "User not authenticated!", Snackbar.LENGTH_SHORT).show()
        }
    }


    private fun deleteCategory(category: Category) {
        firestore.collection("Categories").document(category.id)
            .delete()
            .addOnSuccessListener {
                categories.remove(category)
                categoryAdapter.notifyDataSetChanged()
                Toast.makeText(context, "Category deleted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(requireView(), "Failed to delete category.", Snackbar.LENGTH_SHORT).show()
            }
    }
}
