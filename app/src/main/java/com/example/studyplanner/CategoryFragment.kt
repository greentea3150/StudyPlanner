package com.example.studyplanner

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class CategoryFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private var selectedColor: Int = Color.WHITE

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        val editTextCategoryName = view.findViewById<EditText>(R.id.editTextCategoryName)
        val buttonPickColor = view.findViewById<Button>(R.id.buttonPickColor)
        val buttonSaveCategory = view.findViewById<Button>(R.id.buttonSaveCategory)
        val colorPreview = view.findViewById<View>(R.id.selectedColorPreview)

        firestore = FirebaseFirestore.getInstance()

        // Pick a color
        buttonPickColor.setOnClickListener {
            selectedColor = Color.rgb(
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            )
            colorPreview.setBackgroundColor(selectedColor)
        }

        // Save category to Firestore
        buttonSaveCategory.setOnClickListener {
            val categoryName = editTextCategoryName.text.toString().trim()

            if (categoryName.isEmpty()) {
                Snackbar.make(view, "Category name cannot be empty!", Snackbar.LENGTH_SHORT).show()
            } else {
                val category = hashMapOf(
                    "name" to categoryName,
                    "color" to selectedColor
                )

                firestore.collection("Categories").add(category)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Category saved successfully!", Toast.LENGTH_SHORT).show()
                        editTextCategoryName.text.clear()
                        colorPreview.setBackgroundColor(Color.WHITE)
                    }
                    .addOnFailureListener {
                        Snackbar.make(view, "Failed to save category.", Snackbar.LENGTH_SHORT).show()
                    }
            }
        }

        return view
    }
}
