package com.example.studyplanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        val bottomNav: BottomNavigationView = findViewById(R.id.bottomNavigationView)

        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            var selectedFragment: Fragment? = null

            when (menuItem.itemId) {
                R.id.nav_home -> selectedFragment = HomeFragment()
                R.id.nav_tasks -> selectedFragment = TasksFragment()
                R.id.nav_add_task -> selectedFragment = AddTaskFragment()
                R.id.nav_profile -> selectedFragment = ProfileFragment()
            }

            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, selectedFragment!!).commit()
            true
        }

        // Load the default fragment
        bottomNav.selectedItemId = R.id.nav_home
    }
}
