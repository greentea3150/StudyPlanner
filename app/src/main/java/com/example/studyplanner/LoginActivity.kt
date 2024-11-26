package com.example.studyplanner

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.HideReturnsTransformationMethod
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerRedirect: TextView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var backButton: ImageButton

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.login_email)
        passwordEditText = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.login_button)
        registerRedirect = findViewById(R.id.register_redirect)
        backButton = findViewById(R.id.back_button)

        setUpPasswordToggle(passwordEditText)

        val text = "Don't have an account? Register"
        val spannableString = SpannableString(text)

        val greenColor = ForegroundColorSpan(ContextCompat.getColor(this, R.color.main_green))
        val underlineSpan = UnderlineSpan()

        spannableString.setSpan(greenColor, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(underlineSpan, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }
        spannableString.setSpan(clickableSpan, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        registerRedirect.text = spannableString
        registerRedirect.movementMethod = LinkMovementMethod.getInstance()

        mAuth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Start MainActivity directly after login success
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish() // Optionally close the LoginActivity
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun fetchUserData(userId: String) {
        val usersRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        usersRef.get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value?.toString()
                val email = snapshot.child("email").value?.toString()

                if (name != null && email != null) {
                    // Pass the data to MainActivity via Intent
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("user_name", name)
                    intent.putExtra("user_email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to fetch user details.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching data: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPasswordToggle(editText: EditText) {
        editText.setOnTouchListener { _, event ->
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