package com.example.studyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.*
import com.google.firebase.firestore.FirebaseFirestore
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddTaskFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var et_date: EditText
    private lateinit var radioGroup_category: RadioGroup
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_tasks, container, false)

        firestore = FirebaseFirestore.getInstance()

        //initialize views
        et_date = view.findViewById(R.id.et_date)
        radioGroup_category = view.findViewById(R.id.radioGroup_category)
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

        //initalize 'not started' as the current progress for new task
        val rbNotStarted = view?.findViewById<RadioButton>(R.id.rb_not_started)
        rbNotStarted?.isChecked = true
        setupButtonListeners()
        return view
    }

    private fun setupButtonListeners(){
        submit_button.setOnClickListener {
            saveTask()
        }

        selectImageButton.setOnClickListener {
            selectImage()
        }

        cancel_button.setOnClickListener{
            val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
            bottomNav.selectedItemId = R.id.nav_home
            requireActivity().supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    //open file folders to select an image
    private fun selectImage(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_SELECT)
    }

    //handle result from selecting image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_SELECT && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            // Display the selected image in frame_photo
            uploaded_image.setImageURI(imageUri)
        }
    }

    private fun saveTask(){
        val date = et_date.text.toString().trim()
        val selectedCategory = radioGroup_category.checkedRadioButtonId
        val category = view?.findViewById<RadioButton>(selectedCategory)?.text.toString()
        val timeRange = et_time_range.text.toString().trim()
        val until = et_until.text.toString().trim()
        val taskName = et_task_name.text.toString().trim()
        val objective = et_objective.text.toString().trim()
        val materialsNeeded = et_materials_needed.text.toString().trim()
        val selectedStatus = radioGroup_status.checkedRadioButtonId
        val status = view?.findViewById<RadioButton>(selectedStatus)?.text.toString()
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        //check if an image was uploaded
        if(imageUri != null){
            //upload image to firebase storage
            val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")
            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val taskData = hashMapOf<String, Any>(
                            "date" to date,
                            "category" to category,
                            "timeRange" to timeRange,
                            "until" to until,
                            "taskName" to taskName,
                            "objective" to objective,
                            "materialsNeeded" to materialsNeeded,
                            "status" to status,
                            "photoUrl" to uri.toString(),
                            "userId" to (userId ?: "")
                        )
                        saveTaskToFirestore(taskData)
                    }
                }
                .addOnFailureListener{
                    Toast.makeText(requireContext(), "Image upload failed.", Toast.LENGTH_SHORT).show()
                }
        }else{
            //save task data without photo url
            val taskData = hashMapOf<String, Any>(
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
            saveTaskToFirestore(taskData)
        }
    }

    private fun saveTaskToFirestore(taskData: HashMap<String, Any>){
        firestore.collection("tasks")
            .add(taskData)
            .addOnSuccessListener{
                Toast.makeText(requireContext(), "Task added successfully", Toast.LENGTH_SHORT).show()
                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
                bottomNav.selectedItemId = R.id.nav_home
                requireActivity().supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            .addOnFailureListener{
                Toast.makeText(requireContext(), "Failed to add task", Toast.LENGTH_SHORT).show()
            }
    }

//    override fun onResume() {
//        super.onResume()
//        clearFields() //clear the fields in the add task page each time the page is accessed
//    }


    private fun clearFields() {
        et_date.text.clear()
        radioGroup_category.clearCheck()
        et_time_range.text.clear()
        et_until.text.clear()
        et_task_name.text.clear()
        et_objective.text.clear()
        et_materials_needed.text.clear()
        radioGroup_status.clearCheck()
        uploaded_image.setImageResource(R.drawable.ic_add_image)
        imageUri = null
    }

    companion object{
        private const val REQUEST_IMAGE_SELECT = 1
    }

}