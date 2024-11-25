package com.example.studyplanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private var userName: String? = null
    private var userEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        // Retrieve user data passed from LoginActivity
        userName = intent.getStringExtra("user_name")
        userEmail = intent.getStringExtra("user_email")

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            var selectedFragment: Fragment? = null

            when (menuItem.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_tasks -> selectedFragment = TasksFragment()
                R.id.nav_add_task -> selectedFragment = AddTaskFragment()
                R.id.nav_profile -> {
                    selectedFragment = ProfileFragment()
                    // Pass user data to ProfileFragment
                    val bundle = Bundle()
                    bundle.putString("user_name", userName)
                    bundle.putString("user_email", userEmail)
                    selectedFragment.arguments = bundle
                }
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Load the default fragment
        bottomNav.selectedItemId = R.id.nav_home
    }
}
