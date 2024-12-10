package com.example.studyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.studyplanner.model.Task
import com.google.firebase.firestore.FirebaseFirestore

class TaskDetailsFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var task: Task

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_details, container, false)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Get the Task object from arguments
        task = arguments?.getSerializable("task") as? Task ?: return null

        // Bind data to views
        view.findViewById<TextView>(R.id.task_title).text = task.taskName
        view.findViewById<TextView>(R.id.task_date).text = task.date
        view.findViewById<EditText>(R.id.et_date).setText(task.date)
        view.findViewById<EditText>(R.id.et_time_range).setText("${task.timeRange} - ${task.until}")
        view.findViewById<EditText>(R.id.et_task_name).setText(task.taskName)
        view.findViewById<TextView>(R.id.tvTaskType2).text = task.category
        view.findViewById<TextView>(R.id.tvTaskStatus).text = task.status
        view.findViewById<TextView>(R.id.tv_objective_details).text = task.objective
        view.findViewById<TextView>(R.id.tv_materials_details).text = task.materialsNeeded

        // Set click listener for the update button
        val updateButton = view.findViewById<View>(R.id.updatetask_button)
        updateButton.setOnClickListener {
            updateTask()
        }

        return view
    }

    private fun updateTask() {
        // Get updated values from the EditText fields
        val updatedTaskName = view?.findViewById<EditText>(R.id.et_task_name)?.text.toString()
        val updatedDate = view?.findViewById<EditText>(R.id.et_date)?.text.toString()
        val updatedTimeRange = view?.findViewById<EditText>(R.id.et_time_range)?.text.toString()

        // Validation (optional but recommended)
        if (updatedTaskName.isEmpty() || updatedDate.isEmpty() || updatedTimeRange.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare the updated task data
        val updatedTask = mapOf(
            "taskName" to updatedTaskName,
            "date" to updatedDate,
            "timeRange" to updatedTimeRange,
            "status" to task.status, // You might want to update the status as well if needed
            "category" to task.category,
            "objective" to task.objective,
            "materialsNeeded" to task.materialsNeeded
        )

        // Update the task document in Firestore
        firestore.collection("tasks")
            .document(task.id) // Assuming task.id is the document ID in Firestore
            .update(updatedTask)
            .addOnSuccessListener {
                // Show success message
                Toast.makeText(requireContext(), "Task updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Show error message
                Toast.makeText(requireContext(), "Failed to update task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    companion object {
        fun newInstance(task: Task): TaskDetailsFragment {
            val fragment = TaskDetailsFragment()
            val args = Bundle()
            args.putSerializable("task", task)
            fragment.arguments = args
            return fragment
        }
    }
}
