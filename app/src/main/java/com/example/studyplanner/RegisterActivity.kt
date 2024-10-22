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

        val text = "Already have an account? Log in"
        val spannableString = SpannableString(text)

        val greenColor = ForegroundColorSpan(ContextCompat.getColor(this, R.color.main_green))
        val underlineSpan = UnderlineSpan()

        // Applying spans to the "Log in" part
        spannableString.setSpan(greenColor, 25, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(underlineSpan, 25, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Making "Log in" clickable
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
            }
        }
        spannableString.setSpan(clickableSpan, 25, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the spannable text to your TextView
        loginRedirect.text = spannableString
        loginRedirect.movementMethod = LinkMovementMethod.getInstance()

        mAuth = FirebaseAuth.getInstance()

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
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
