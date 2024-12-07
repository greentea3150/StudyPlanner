package com.example.studyplanner

import android.os.Bundle
import android.view.View
import android.widget.ImageView
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

        userName = intent.getStringExtra("user_name")
        userEmail = intent.getStringExtra("user_email")

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val plusIcon: ImageView = findViewById(R.id.plus_icon)

        // Set BottomNavigation listener
        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            var selectedFragment: Fragment? = null

            when (menuItem.itemId) {
                R.id.nav_home -> {
                    selectedFragment = HomeFragment()
                    showNavComponents() // Ensure BottomNav and PlusIcon reappear
                }
                R.id.nav_tasks -> {
                    selectedFragment = TasksFragment()
                    showNavComponents()
                }
                R.id.nav_add_task -> {
                    selectedFragment = CategoryFragment()
                    showNavComponents()
                }
                R.id.nav_profile -> {
                    selectedFragment = ProfileFragment()
                    val bundle = Bundle()
                    bundle.putString("user_name", userName)
                    bundle.putString("user_email", userEmail)
                    selectedFragment.arguments = bundle
                    showNavComponents()
                }
            }

            if (selectedFragment != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Set Plus Icon Listener
        plusIcon.setOnClickListener {
            val addTaskFragment = AddTaskFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, addTaskFragment)
                .commit()
            hideNavComponents() // Hide BottomNav and PlusIcon when navigating via PlusIcon
        }

        // Load the default fragment
        bottomNav.selectedItemId = R.id.nav_home
    }

    // Function to hide BottomNavigationView and PlusIcon
    private fun hideNavComponents() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val plusIcon: ImageView = findViewById(R.id.plus_icon)
        bottomNav.visibility = View.GONE
        plusIcon.visibility = View.GONE
    }

    // Function to show BottomNavigationView and PlusIcon
    private fun showNavComponents() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        val plusIcon: ImageView = findViewById(R.id.plus_icon)
        bottomNav.visibility = View.VISIBLE
        plusIcon.visibility = View.VISIBLE
    }
}

