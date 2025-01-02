package com.example.studyplanner

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.studyplanner.adapter.TaskAdapter
import com.example.studyplanner.model.Task

class TasksFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val tasksList = mutableListOf<Task>()
    private val allCategories = mutableListOf<String>() // To hold all unique categories
    private var currentCategory: String? = null
    private var searchQuery: String = ""  // To hold the search query

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
        tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize Adapter
        taskAdapter = TaskAdapter(
            tasksList,
            onItemClick = { task -> showTaskDetails(task) },
            onDeleteClick = { taskId -> deleteTaskFromFirestore(taskId) },
            isHomePage = false // Set this to false for other pages
        )
        tasksRecyclerView.adapter = taskAdapter

        // Fetch Tasks
        fetchTasks()

        // Find the filter button and set a click listener
        val filterButton: ImageButton = view.findViewById(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterDialog()  // Show the filter dialog when the button is clicked
        }

        // Fetch categories (if not already fetched)
        fetchCategories()

        // Setup SearchView
        val searchView: androidx.appcompat.widget.SearchView = view.findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Optional: handle submission if needed
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Log to check the query change
                Log.d("SearchQuery", "Query changed: $newText")
                searchQuery = newText ?: ""  // Update the searchQuery variable
                return true
            }
        })

        // Find the search button and set a click listener to trigger search
        val searchButton: Button = view.findViewById(R.id.searchButton)
        searchButton.setOnClickListener {
            // Get the query text entered by the user
            val query = searchView.query.toString().trim()

            if (query.isNotEmpty()) {
                searchQuery = query // Update search query
                fetchTasks() // Call fetchTasks() to fetch tasks based on the query
            }
        }

        // Find the clear search button and set a click listener to reset the search
        val clearSearchButton: Button = view.findViewById(R.id.clearSearchButton)
        clearSearchButton.setOnClickListener {
            searchQuery = "" // Reset the search query
            searchView.setQuery("", false) // Clear the text in the SearchView
            fetchTasks() // Fetch all tasks without search filter
        }

        return view
    }

    private fun fetchTasks() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        var query = firestore.collection("tasks")
            .whereEqualTo("userId", currentUserId)

        // If we have a category selected, filter tasks by category
        currentCategory?.let {
            query = query.whereEqualTo("category", it)
        }

        // If there's a search query, filter tasks by taskName (correct field)
        if (searchQuery.isNotEmpty()) {
            query = query.whereGreaterThanOrEqualTo("taskName", searchQuery)  // Use taskName field
                .whereLessThanOrEqualTo("taskName", searchQuery + "\uf8ff")  // Firebase range query for string search
        }

        // Sort tasks by date first, then by timeRange (start time)
        query = query.orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .orderBy("timeRange", com.google.firebase.firestore.Query.Direction.ASCENDING)

        // Listen to changes and update tasks
        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("TasksFragment", "Error getting tasks", e)
                return@addSnapshotListener
            }

            // Parse tasks
            val fetchedTasks = snapshot?.toObjects(Task::class.java) ?: listOf()

            // Sort tasks in memory: Finished tasks should be at the bottom
            val sortedTasks = fetchedTasks.sortedWith(compareBy<Task> { it.status == "Finished" }.thenBy { it.date }.thenBy { it.timeRange })

            // Update the list with the sorted tasks
            tasksList.clear()
            tasksList.addAll(sortedTasks)
            taskAdapter.updateTasks(tasksList)
        }
    }



    // Fetch categories from the 'Categories' collection
    private fun fetchCategories() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Ensure that the user is logged in
        if (currentUserId != null) {
            firestore.collection("Categories")
                .whereEqualTo("userId", currentUserId) // Only fetch categories for the current user
                .get()
                .addOnSuccessListener { snapshot ->
                    allCategories.clear()
                    snapshot.documents.forEach { doc ->
                        val category = doc.getString("name")
                        if (category != null && !allCategories.contains(category)) {
                            allCategories.add(category)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle error fetching categories
                    e.printStackTrace()
                }
        } else {
            // Handle case where user is not logged in
        }
    }

    // Show task details (existing function)
    private fun showTaskDetails(task: Task) {
        val taskDetailFragment = TaskDetailsFragment.newInstance(task)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, taskDetailFragment)
            .addToBackStack(null)
            .commit()
    }

    // Delete task from Firestore (existing function)
    private fun deleteTaskFromFirestore(taskId: String) {
        val taskRef = firestore.collection("tasks").document(taskId)
        taskRef.delete()
            .addOnSuccessListener {
                tasksList.removeAll { task -> task.id == taskId }
                taskAdapter.notifyDataSetChanged()
            }
    }

    // Show filter dialog with category options
    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_tasks_dialog_filter, null)

        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Create the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("APPLY") { _, _ ->
                currentCategory = spinner.selectedItem as String
                fetchTasks() // Fetch tasks with the selected category
            }
            .setNegativeButton("CLEAR") { _, _ ->
                currentCategory = null
                fetchTasks() // Fetch all tasks (clear filter)
            }
            .create()

        dialog.show()
    }

}