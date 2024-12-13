package com.example.studyplanner

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

class AddTaskFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var et_date: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var et_time_range: EditText
    private lateinit var et_until: EditText
    private lateinit var et_task_name: EditText
    private lateinit var et_objective: EditText
    private lateinit var et_materials_needed: EditText
    private lateinit var radioGroup_status: RadioGroup
    private lateinit var uploaded_image: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var cancel_button: Button
    private lateinit var submit_button: Button

    private var imageUri: Uri? = null
    private val categoryList = mutableListOf<String>()
    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_tasks, container, false)

        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        et_date = view.findViewById(R.id.et_date)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        et_time_range = view.findViewById(R.id.et_time_range)
        et_until = view.findViewById(R.id.et_until)
        et_task_name = view.findViewById(R.id.et_task_name)
        et_objective = view.findViewById(R.id.et_objective)
        et_materials_needed = view.findViewById(R.id.et_materials_needed)
        radioGroup_status = view.findViewById(R.id.radioGroup_status)
        uploaded_image = view.findViewById(R.id.uploaded_image)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        cancel_button = view.findViewById(R.id.cancel_button)
        submit_button = view.findViewById(R.id.submit_button)

        val rbNotStarted = view.findViewById<RadioButton>(R.id.rb_not_started)
        rbNotStarted.isChecked = true

        // Disable the other 2 status radio buttons
        val rbInProgress = view.findViewById<RadioButton>(R.id.rb_in_progress)
        val rbFinished = view.findViewById<RadioButton>(R.id.rb_finished)

        rbInProgress.isEnabled = false
        rbFinished.isEnabled = false

        setupButtonListeners()
        loadCategories()
        setupDateTimePickers()

        return view
    }

    private fun setupButtonListeners() {
        submit_button.setOnClickListener {
            saveTask()
        }

        selectImageButton.setOnClickListener {
            selectImage()
        }

        cancel_button.setOnClickListener {
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav.selectedItemId = R.id.nav_home
            requireActivity().supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    private fun loadCategories() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("Categories")
            .whereEqualTo("userId", userId) // Filter categories by the current user's ID
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
    }

    private fun saveTask() {
        val date = et_date.text.toString().trim()
        val category = spinnerCategory.selectedItem?.toString() ?: ""
        val timeRange = et_time_range.text.toString().trim()
        val until = et_until.text.toString().trim()
        val taskName = et_task_name.text.toString().trim()
        val objective = et_objective.text.toString().trim()
        val materialsNeeded = et_materials_needed.text.toString().trim()
        val selectedStatus = radioGroup_status.checkedRadioButtonId
        val status = view?.findViewById<RadioButton>(selectedStatus)?.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        val taskData = hashMapOf(
            "date" to date,
            "category" to category,
            "timeRange" to timeRange,
            "until" to until,
            "taskName" to taskName,
            "objective" to objective,
            "materialsNeeded" to materialsNeeded,
            "status" to status,
            "userId" to (userId ?: "")
        )

        firestore.collection("tasks")
            .add(taskData)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show()

                // Get the task ID and schedule the notification
                val taskId = documentReference.id
                scheduleTaskNotification(taskId, timeRange)

                // Navigate back to the home screen
                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                bottomNav.selectedItemId = R.id.nav_home
                requireActivity().supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun scheduleTaskNotification(taskId: String, timeRange: String) {
        // Parse the timeRange to get the hour and minute
        val timeParts = timeRange.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        // Set up the calendar object for the task start time
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)

        // Calculate the delay in milliseconds
        val delay = calendar.timeInMillis - System.currentTimeMillis()

        // If the time has already passed, schedule it for the next day
        if (delay <= 0) {
            calendar.add(Calendar.DATE, 1)
        }

        // Create input data for the worker
        val inputData = workDataOf("taskId" to taskId)

        // Create the WorkRequest
        val workRequest = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        // Enqueue the WorkRequest
        WorkManager.getInstance(requireContext()).enqueue(workRequest)
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_SELECT)
    }

    private fun setupDateTimePickers() {
        et_date.setOnClickListener {
            showDatePicker()
        }
        et_time_range.setOnClickListener {
            showTimePicker(et_time_range)
        }
        et_until.setOnClickListener {
            showTimePicker(et_until)
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
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)
        et_date.setText(formattedDate)
    }

    private fun updateTime(editText: EditText) {
        val timeFormat = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(calendar.time)
        editText.setText(formattedTime)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            uploaded_image.setImageURI(imageUri)
        }
    }

    companion object {
        private const val REQUEST_IMAGE_SELECT = 1
    }
}
