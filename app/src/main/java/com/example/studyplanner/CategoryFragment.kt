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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

        // Find views related to the category list
        val recyclerViewCategories = view.findViewById<RecyclerView>(R.id.recyclerViewCategories)
        val fabAddCategory = view.findViewById<FloatingActionButton>(R.id.fabAddCategory)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize RecyclerView
        categoryAdapter = CategoryAdapter(categories) { category ->
            deleteCategory(category)
        }
        recyclerViewCategories.layoutManager = LinearLayoutManager(context)
        recyclerViewCategories.adapter = categoryAdapter

        // Load existing categories
        loadCategories()

        // Handle FAB click
        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        return view
    }

    private fun showColorPickerDialog(onColorSelected: (Int) -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewColors)
        recyclerView.layoutManager = GridLayoutManager(context, 5) // 5 columns

        val dialog = builder.setView(dialogView).create() // Create the dialog instance here

        recyclerView.adapter = ColorAdapter(presetColors) { color ->
            onColorSelected(color)
            dialog.dismiss() // Dismiss the actual dialog instance
        }

        dialog.show() // Show the dialog after setting up everything
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

    private fun showAddCategoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.fragment_category_dialog_add_category, null)

        val editTextCategoryName = dialogView.findViewById<EditText>(R.id.editTextCategoryName)
        val buttonPickColor = dialogView.findViewById<Button>(R.id.buttonPickColor)
        val colorPreview = dialogView.findViewById<View>(R.id.selectedColorPreview)

        // Reset selected color to default
        selectedColor = Color.WHITE
        colorPreview.setBackgroundColor(selectedColor)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Add New Category")
            .setPositiveButton("Save") { _, _ ->
                val categoryName = editTextCategoryName.text.toString().trim()
                if (categoryName.isEmpty()) {
                    Toast.makeText(context, "Category name cannot be empty!", Toast.LENGTH_SHORT).show()
                } else {
                    saveCategory(categoryName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Pick a color
        buttonPickColor.setOnClickListener {
            showColorPickerDialog { color ->
                selectedColor = color
                colorPreview.setBackgroundColor(selectedColor)
            }
        }

        dialog.show()
    }

    private fun saveCategory(categoryName: String) {
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
                    loadCategories()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to save category.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show()
        }
    }


}
