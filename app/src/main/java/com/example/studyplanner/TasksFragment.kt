package com.example.studyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.studyplanner.R
import com.example.studyplanner.adapter.TaskAdapter
import com.example.studyplanner.model.Task

class TasksFragment : Fragment() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private val tasksList = mutableListOf<Task>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
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

        return view
    }

    private fun fetchTasks() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        currentUserId?.let { userId ->
            firestore.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firestore", "Error fetching tasks", e)
                        return@addSnapshotListener
                    }

                    val fetchedTasks = snapshot?.toObjects(Task::class.java) ?: listOf()
                    tasksList.clear()
                    tasksList.addAll(fetchedTasks)
                    taskAdapter.updateTasks(tasksList)
                }
        }
    }

    private fun deleteTaskFromFirestore(taskId: String) {
        Log.d("TaskID", "Attempting to delete task with ID: $taskId")

        val taskRef = firestore.collection("tasks").document(taskId)

        taskRef.delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Task deleted successfully")

                // Find the position of the deleted task in the list
                val position = tasksList.indexOfFirst { it.id == taskId }
                if (position >= 0) {
                    tasksList.removeAt(position) // Remove the task from the list
                    taskAdapter.notifyItemRemoved(position) // Notify the adapter
                }

                // Optionally, refresh the task list
                fetchTasks() // You can refresh the list or leave it as is
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error deleting task", e)
                // Optionally, show an error message to the user
            }
    }


    private fun showTaskDetails(task: Task) {
        val taskDetailFragment = TaskDetailsFragment.newInstance(task)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, taskDetailFragment) // Replace with your container ID
            .addToBackStack(null) // Add to backstack for navigation
            .commit()
    }

    companion object {
        fun newInstance(): TasksFragment {
            return TasksFragment()
        }
    }
}