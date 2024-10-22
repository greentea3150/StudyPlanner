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

        // Applying spans to the "Register" part
        spannableString.setSpan(greenColor, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(underlineSpan, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Making "Log in" clickable
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            }
        }
        spannableString.setSpan(clickableSpan, 23, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the spannable text to your TextView
        registerRedirect.text = spannableString
        registerRedirect.movementMethod = LinkMovementMethod.getInstance()

        mAuth = FirebaseAuth.getInstance()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPasswordToggle(editText: EditText) {
        editText.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if the touch is on the drawableEnd (right side)
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
            // Show password
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_toggle_password_visibility_off), null
            )
        } else {
            // Hide password
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                null, null, ContextCompat.getDrawable(this, R.drawable.ic_toggle_password_visibility_on), null
            )
        }
        // Move the cursor to the end
        editText.setSelection(editText.text.length)
    }
}

