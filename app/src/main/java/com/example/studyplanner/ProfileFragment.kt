package com.example.studyplanner

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import android.Manifest

class ProfileFragment : Fragment() {

    private lateinit var profileNameTextView: TextView
    private lateinit var profilePasswordTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var editNameIcon: ImageView
    private lateinit var editPasswordIcon: ImageView
    private lateinit var editProfilePictureIcon: ImageButton
    private lateinit var profileImageView: ImageView
    private lateinit var logoutButton: Button
    private lateinit var mAuth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance() // Initialize Firestore
    private val storage = FirebaseStorage.getInstance()

    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST_CODE = 2
    private val CAMERA_PERMISSION_CODE = 100

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
        editProfilePictureIcon = view.findViewById(R.id.camera_button)
        profileImageView = view.findViewById(R.id.imageView8)
        logoutButton = view.findViewById(R.id.logout_button)

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
        editProfilePictureIcon.setOnClickListener { showPictureOptionsDialog() }

        logoutButton.setOnClickListener { logoutUser() }
        loadProfilePictureFromFirestore()
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
                            Toast.makeText(requireContext(), "Error: User data is incomplete.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Error: User document not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadProfilePictureFromFirestore() {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userDocRef = firestore.collection("users").document(userId)

            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val profilePictureUrl = document.getString("profilePicture")
                        if (!profilePictureUrl.isNullOrEmpty()) {
                            Glide.with(this).load(profilePictureUrl).into(profileImageView)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to load profile picture: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun showPictureOptionsDialog() {
        val options = arrayOf("Take a Photo", "Choose from Gallery")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkCameraPermission() // Ubah ini
                    1 -> selectImageFromGallery()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission belum diberikan, minta permission
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            // Permission sudah ada, buka kamera
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission diberikan
                    openCamera()
                } else {
                    // Permission ditolak
                    Toast.makeText(
                        requireContext(),
                        "Camera permission is required to take photos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
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

    private fun logoutUser() {
        mAuth.signOut()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        Toast.makeText(requireContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val imageUri = data.data
                    uploadProfilePicture(imageUri)
                }
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data.extras?.get("data") as Bitmap
                    val imageUri = saveBitmapToUri(imageBitmap)
                    uploadProfilePicture(imageUri)
                }
            }
        }
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "profile_picture.jpg")
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
    }



    private fun uploadProfilePicture(imageUri: Uri?) {
        if (imageUri != null) {
            val currentUser = mAuth.currentUser
            if (currentUser != null) {
                val userId = currentUser.uid
                val storageRef = storage.reference.child("profile_pictures/$userId.jpg")

                val uploadTask = storageRef.putFile(imageUri)
                uploadTask.addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        updateUserProfilePictureInFirestore(uri.toString())
                        Glide.with(this).load(uri).into(profileImageView)
                        Toast.makeText(requireContext(), "Profile picture updated successfully.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to upload picture: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "No image selected.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateUserProfilePictureInFirestore(downloadUrl: String) {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userDocRef = firestore.collection("users").document(userId)

            userDocRef.update("profilePicture", downloadUrl)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile picture URL updated in Firestore.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to update Firestore: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
