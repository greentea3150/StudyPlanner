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

class ProfileFragment : Fragment() {

    private lateinit var profileNameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var mAuth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance()

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
            // Fallback to load data from Firebase if arguments are null
            loadUserProfileFromFirebase()
        }

        return view
    }

    private fun loadUserProfileFromFirebase() {
        val currentUser = mAuth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            val usersRef = database.getReference("users").child(userId)

            usersRef.get()
                .addOnSuccessListener { snapshot ->
                    val name = snapshot.child("name").value?.toString()
                    val email = snapshot.child("email").value?.toString()

                    if (name != null && email != null) {
                        profileNameTextView.text = name
                        emailTextView.text = email
                    } else {
                        Toast.makeText(context, "Error: User data is incomplete.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Failed to load profile: ${exception.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_LONG).show()
        }
    }
}
