package com.example.studyplanner

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.ImageButton
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var loginRedirect: TextView
    private lateinit var mAuth: FirebaseAuth

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nameEditText = findViewById(R.id.register_name)
        emailEditText = findViewById(R.id.register_email)
        passwordEditText = findViewById(R.id.register_password)
        confirmPasswordEditText = findViewById(R.id.register_confirm_password)
        registerButton = findViewById(R.id.register_button)
        loginRedirect = findViewById(R.id.login_redirect)
        backButton = findViewById(R.id.back_button)

        setUpPasswordToggle(passwordEditText, isPasswordVisible)
        setUpPasswordToggle(confirmPasswordEditText, isConfirmPasswordVisible)

        mAuth = FirebaseAuth.getInstance()

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = mAuth.currentUser?.uid
                            if (userId != null) {
                                val firestore = FirebaseFirestore.getInstance()
                                val userMap = mapOf(
                                    "name" to name,
                                    "email" to email
                                )
                                firestore.collection("users").document(userId)
                                    .set(userMap)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                                            startActivity(Intent(this, MainActivity::class.java))
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Failed to save user data: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(this, "User ID is null. Try again.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPasswordToggle(editText: EditText, isVisible: Boolean) {
        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (editText.right - editText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility(editText)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(editText: EditText) {
        if (editText.transformationMethod is PasswordTransformationMethod) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_toggle_password_visibility_off), null
            )
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_toggle_password_visibility_on), null
            )
        }
        editText.setSelection(editText.text.length)
    }
}
