package com.example.studyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var profileNameTextView: TextView
    private lateinit var emailTextView: TextView
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
}
