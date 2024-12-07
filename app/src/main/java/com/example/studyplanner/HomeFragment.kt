package com.example.studyplanner

import com.example.studyplanner.databinding.FragmentHomeBinding
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.adapter.TaskAdapter
import com.example.studyplanner.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
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
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Setup RecyclerView
        tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
        tasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize Adapter
        taskAdapter = TaskAdapter(
            tasksList
        ) { task -> showTaskDetails(task) }
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

    private fun showTaskDetails(task: Task) {
        // Implement task details dialog or navigation
    }

    companion object {
        fun newInstance(): TasksFragment {
            return TasksFragment()
        }
    }
}

