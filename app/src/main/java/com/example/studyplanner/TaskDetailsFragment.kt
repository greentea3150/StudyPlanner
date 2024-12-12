package com.example.studyplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailsFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var task: Task
    private val calendar = Calendar.getInstance()

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
        val dateEditText = view.findViewById<EditText>(R.id.et_date)
        val timeRangeEditText = view.findViewById<EditText>(R.id.et_time_range)
        val taskNameEditText = view.findViewById<EditText>(R.id.et_task_name)

        view.findViewById<TextView>(R.id.task_title).text = task.taskName
        view.findViewById<TextView>(R.id.task_date).text = task.date
        dateEditText.setText(task.date)
        timeRangeEditText.setText("${task.timeRange} - ${task.until}")
        taskNameEditText.setText(task.taskName)
        view.findViewById<TextView>(R.id.tvTaskType2).text = task.category
        view.findViewById<TextView>(R.id.tvTaskStatus).text = task.status
        view.findViewById<TextView>(R.id.tv_objective_details).text = task.objective
        view.findViewById<TextView>(R.id.tv_materials_details).text = task.materialsNeeded

        // Set Date Picker
        dateEditText.setOnClickListener {
            val existingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(task.date)
            if (existingDate != null) {
                calendar.time = existingDate
            }
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    updateDate(dateEditText)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Set Time Picker for time range
        timeRangeEditText.setOnClickListener {
            pickTimeRange(timeRangeEditText)
        }

        // Set click listener for the update button
        val updateButton = view.findViewById<View>(R.id.updatetask_button)
        updateButton.setOnClickListener {
            updateTask()
        }

        return view
    }

    private fun updateDate(editText: EditText) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        editText.setText(dateFormat.format(calendar.time))
    }

    private fun pickTimeRange(editText: EditText) {
        val taskTimeRange = task.timeRange.split(" - ")
        val startTime = Calendar.getInstance()
        val endTime = Calendar.getInstance()

        if (taskTimeRange.size == 2) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            try {
                startTime.time = timeFormat.parse(taskTimeRange[0]) ?: startTime.time
                endTime.time = timeFormat.parse(taskTimeRange[1]) ?: endTime.time
            } catch (e: Exception) {
                // Fallback to current time if parsing fails
            }
        }

        // First, pick the start time
        TimePickerDialog(
            requireContext(),
            { _, startHour, startMinute ->
                startTime.set(Calendar.HOUR_OF_DAY, startHour)
                startTime.set(Calendar.MINUTE, startMinute)

                // Then, pick the end time
                TimePickerDialog(
                    requireContext(),
                    { _, endHour, endMinute ->
                        endTime.set(Calendar.HOUR_OF_DAY, endHour)
                        endTime.set(Calendar.MINUTE, endMinute)

                        // Update the EditText with the selected time range
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val startFormatted = timeFormat.format(startTime.time)
                        val endFormatted = timeFormat.format(endTime.time)
                        editText.setText("$startFormatted - $endFormatted")
                    },
                    endTime.get(Calendar.HOUR_OF_DAY),
                    endTime.get(Calendar.MINUTE),
                    true
                ).show()
            },
            startTime.get(Calendar.HOUR_OF_DAY),
            startTime.get(Calendar.MINUTE),
            true
        ).show()
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
            "status" to task.status,
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
