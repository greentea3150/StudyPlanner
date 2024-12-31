package com.example.studyplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.studyplanner.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TaskDetailsFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var task: Task
    private val calendar = Calendar.getInstance()

    private lateinit var etDate: EditText
    private lateinit var etTimeRange: EditText
    private lateinit var etUntil: EditText
    private lateinit var etTaskName: EditText
    private lateinit var etObjective: EditText
    private lateinit var etMaterialsNeeded: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var radioGroupStatus: RadioGroup

    private val categoryList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_details, container, false)

        firestore = FirebaseFirestore.getInstance()

        // Bind views
        etDate = view.findViewById(R.id.et_date)
        etTimeRange = view.findViewById(R.id.et_time_range)
        etUntil = view.findViewById(R.id.et_until)
        etTaskName = view.findViewById(R.id.et_task_name)
        etObjective = view.findViewById(R.id.et_objective)
        etMaterialsNeeded = view.findViewById(R.id.et_materials_needed)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        radioGroupStatus = view.findViewById(R.id.radioGroup_status)

        val updateButton = view.findViewById<Button>(R.id.updatetask_button)
        val deleteButton = view.findViewById<Button>(R.id.deletetask_button)

        // Get the task data from arguments
        task = arguments?.getSerializable("task") as? Task ?: return null

        // Populate fields with existing task data
        populateTaskDetails()

        // Load categories for the spinner
        loadCategories()

        // Setup date and time pickers
        setupDateTimePickers()

        // Handle update button click
        updateButton.setOnClickListener {
            updateTaskInFirestore()
        }

        deleteButton.setOnClickListener {
            deleteTaskInFirestore()
        }

        return view
    }

    private fun populateTaskDetails() {
        etDate.setText(task.date)
        etTimeRange.setText(task.timeRange)
        etUntil.setText(task.until)
        etTaskName.setText(task.taskName)
        etObjective.setText(task.objective)
        etMaterialsNeeded.setText(task.materialsNeeded)

        // Set category spinner to current task category
        spinnerCategory.setSelection(categoryList.indexOf(task.category))

        // Set status radio button to current task status
        when (task.status) {
            getString(R.string.not_started) -> radioGroupStatus.check(R.id.rb_not_started)
            getString(R.string.in_progress) -> radioGroupStatus.check(R.id.rb_in_progress)
            getString(R.string.finished) -> radioGroupStatus.check(R.id.rb_finished)
        }
    }

    private fun loadCategories() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("Categories")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                categoryList.clear()
                for (document in querySnapshot) {
                    val category = document.getString("name")
                    if (category != null) {
                        categoryList.add(category)
                    }
                }
                setupCategorySpinner()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load categories.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Set spinner to current category after data is loaded
        spinnerCategory.setSelection(categoryList.indexOf(task.category))
    }

    private fun setupDateTimePickers() {
        etDate.setOnClickListener {
            showDatePicker()
        }

        etTimeRange.setOnClickListener {
            showTimePicker(etTimeRange)
        }

        etUntil.setOnClickListener {
            showTimePicker(etUntil)
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDate()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(editText: EditText) {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            updateTime(editText)
        }

        TimePickerDialog(
            requireContext(),
            timeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(dateFormat.format(calendar.time))
    }

    private fun updateTime(editText: EditText) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        editText.setText(timeFormat.format(calendar.time))
    }

    private fun updateTaskInFirestore() {
        val updatedTask = mapOf(
            "taskName" to etTaskName.text.toString().trim(),
            "category" to (spinnerCategory.selectedItem?.toString() ?: task.category.toString()),
            "date" to etDate.text.toString().trim(),
            "timeRange" to etTimeRange.text.toString().trim(),
            "until" to etUntil.text.toString().trim(),
            "objective" to etObjective.text.toString().trim(),
            "materialsNeeded" to etMaterialsNeeded.text.toString().trim(),
            "status" to getSelectedStatus()
        )

        firestore.collection("tasks")
            .document(task.id) // Assuming task.id is the document ID
            .update(updatedTask)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Task updated successfully!", Toast.LENGTH_SHORT).show()
                scheduleTaskNotification(task.id, etTimeRange.text.toString().trim(), etDate.text.toString().trim())
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update task.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteTaskInFirestore() {
        firestore.collection("tasks")
            .document(task.id) // Menggunakan task.id sebagai ID dokumen
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Task deleted successfully!", Toast.LENGTH_SHORT).show()
                // Kembali ke layar sebelumnya atau perbarui UI
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to delete task: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleTaskNotification(taskId: String, timeRange: String, date: String) {
        val dateParts = date.split("-")
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Calendar month is 0-based
        val day = dateParts[2].toInt()

        val timeParts = timeRange.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val taskCalendar = Calendar.getInstance()
        taskCalendar.set(Calendar.YEAR, year)
        taskCalendar.set(Calendar.MONTH, month)
        taskCalendar.set(Calendar.DAY_OF_MONTH, day)
        taskCalendar.set(Calendar.HOUR_OF_DAY, hour)
        taskCalendar.set(Calendar.MINUTE, minute)
        taskCalendar.set(Calendar.SECOND, 0)

        val currentTimeMillis = System.currentTimeMillis()
        val taskTimeMillis = taskCalendar.timeInMillis

        if (taskTimeMillis <= currentTimeMillis) {
            Log.w("TaskNotification", "Scheduled time is in the past. Notification not scheduled.")
            return
        }

        val delay = taskTimeMillis - currentTimeMillis
        val inputData = workDataOf("taskId" to taskId)

        val workRequest = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(taskId)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(workRequest)
        Log.d("TaskNotification", "Notification scheduled for $date $timeRange (delay: $delay ms)")
    }

    private fun getSelectedStatus(): String {
        val selectedStatusId = radioGroupStatus.checkedRadioButtonId
        return when (selectedStatusId) {
            R.id.rb_not_started -> getString(R.string.not_started)
            R.id.rb_in_progress -> getString(R.string.in_progress)
            R.id.rb_finished -> getString(R.string.finished)
            else -> task.status
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


