package com.example.studyplanner

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var profileNameTextView: TextView
    private lateinit var profilePasswordTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var editNameIcon: ImageView
    private lateinit var editPasswordIcon: ImageView
    private lateinit var mAuth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance() // Initialize Firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize views
        profileNameTextView = view.findViewById(R.id.profile_name)
        emailTextView = view.findViewById(R.id.profile_email)
        editNameIcon = view.findViewById(R.id.edit_name_icon)
        editPasswordIcon = view.findViewById(R.id.edit_password_icon)


        mAuth = FirebaseAuth.getInstance()

        // Check for data passed from MainActivity
        val userName = arguments?.getString("user_name")
        val userEmail = arguments?.getString("user_email")

        if (userName != null && userEmail != null) {
            // Use data passed from MainActivity
            profileNameTextView.text = userName
            emailTextView.text = userEmail
        } else {
            // Fallback to load data from Firestore if arguments are null
            loadUserProfileFromFirestore()
        }

        editNameIcon.setOnClickListener { showEditNameDialog() }
        editPasswordIcon.setOnClickListener { showEditPasswordDialog() }

        return view
    }

    private fun loadUserProfileFromFirestore() {
        val currentUser = mAuth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val userDocRef = firestore.collection("users").document(userId)

            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")

                        if (name != null && email != null) {
                            profileNameTextView.text = name
                            emailTextView.text = email
                        } else {
                            if (isAdded) { // Ensure the fragment is attached
                                Toast.makeText(requireContext(), "Error: User data is incomplete.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        if (isAdded) { // Ensure the fragment is attached
                            Toast.makeText(requireContext(), "Error: User document not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (isAdded) { // Ensure the fragment is attached
                        Toast.makeText(requireContext(), "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            if (isAdded) { // Ensure the fragment is attached
                Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditNameDialog() {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Inflate the custom layout
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_home_dialog_edit_name, null)
            val inputField = dialogView.findViewById<EditText>(R.id.edit_name_input)
            val cancelButton = dialogView.findViewById<Button>(R.id.dialog_cancel_button)
            val saveButton = dialogView.findViewById<Button>(R.id.dialog_save_button)

            // Create the AlertDialog
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            // Handle Cancel Button Click
            cancelButton.setOnClickListener { dialog.dismiss() }

            // Handle Save Button Click
            saveButton.setOnClickListener {
                val newName = inputField.text.toString().trim()
                if (newName.isNotBlank()) {
                    updateUserNameInFirestore(userId, newName)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                }
            }

            // Show the dialog
            dialog.show()
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateUserNameInFirestore(userId: String, newName: String) {
        val userDocRef = firestore.collection("users").document(userId)

        userDocRef.update("name", newName)
            .addOnSuccessListener {
                profileNameTextView.text = newName
                Toast.makeText(requireContext(), "Name updated successfully.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to update name: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showEditPasswordDialog() {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_home_dialog_edit_password, null)

        // Find views in the custom dialog
        val oldPasswordEditText = dialogView.findViewById<EditText>(R.id.edit_old_password_input)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.edit_new_password_input)
        val cancelButton = dialogView.findViewById<AppCompatButton>(R.id.dialog_cancel_button_password)
        val saveButton = dialogView.findViewById<AppCompatButton>(R.id.dialog_save_button_password)
        val toggleVisibilityButtonOld = dialogView.findViewById<ImageView>(R.id.password_toggle_visibility_old)
        val toggleVisibilityButtonNew = dialogView.findViewById<ImageView>(R.id.password_toggle_visibility_new)

        // Create the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Show the dialog
        dialog.show()

        // Set click listener for the Save button
        saveButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both old and new passwords", Toast.LENGTH_SHORT).show()
            } else {
                verifyOldPasswordAndUpdate(oldPassword, newPassword)
                dialog.dismiss()  // Close the dialog after saving
            }
        }

        // Set click listener for the Cancel button
        cancelButton.setOnClickListener {
            dialog.dismiss()  // Close the dialog if canceled
        }

        // Toggle password visibility
        toggleVisibilityButtonNew.setOnClickListener {
            if (newPasswordEditText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                newPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleVisibilityButtonNew.setImageResource(R.drawable.ic_toggle_password_visibility_on)  // Change icon to "eye" (visible)
            } else {
                newPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleVisibilityButtonNew.setImageResource(R.drawable.ic_toggle_password_visibility_off)  // Change icon to "eye" (hidden)
            }
            // Move the cursor to the end of the password
            newPasswordEditText.setSelection(newPasswordEditText.text.length)
        }

        toggleVisibilityButtonOld.setOnClickListener {
            if (oldPasswordEditText.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                oldPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleVisibilityButtonOld.setImageResource(R.drawable.ic_toggle_password_visibility_on)  // Change icon to "eye" (visible)
            } else {
                oldPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleVisibilityButtonOld.setImageResource(R.drawable.ic_toggle_password_visibility_off)  // Change icon to "eye" (hidden)
            }
            // Move the cursor to the end of the password
            oldPasswordEditText.setSelection(oldPasswordEditText.text.length)
        }
    }

    private fun verifyOldPasswordAndUpdate(oldPassword: String, newPassword: String) {
        val currentUser = mAuth.currentUser

        if (currentUser != null) {
            val email = currentUser.email
            val credential = EmailAuthProvider.getCredential(email!!, oldPassword)

            // Reauthenticate with old password
            currentUser.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Old password is correct, now update the password
                        updatePasswordInFirebase(newPassword)
                    } else {
                        Toast.makeText(requireContext(), "Old password is incorrect.", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_LONG).show()
        }
    }

    private fun updatePasswordInFirebase(newPassword: String) {
        val currentUser = mAuth.currentUser

        if (currentUser != null) {
            // Update the password in Firebase Authentication
            currentUser.updatePassword(newPassword)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                    // Update the UI (TextView) with the new password (For display purposes only)
                    profilePasswordTextView.text = newPassword
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to update password: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_LONG).show()
        }
    }



}
