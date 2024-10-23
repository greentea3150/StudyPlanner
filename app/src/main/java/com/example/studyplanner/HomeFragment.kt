package com.example.studyplanner

import com.example.studyplanner.databinding.FragmentHomeBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access each task's included layout and set its title and time
        setTask(binding.taskList.getChildAt(0), "Electrical Machines 2", "07:00am - 08:00am")
        setTask(binding.taskList.getChildAt(1), "Basic Programming", "14:00pm - 16:00pm")
        setTask(binding.taskList.getChildAt(2), "Computer Security", "18:00pm - 20:00pm")
        setTask(binding.taskList.getChildAt(3), "Robotics", "21:00pm - 22:00pm")
        setTask(binding.taskList.getChildAt(4), "Math Review", "08:00am - 09:00am")
        setTask(binding.taskList.getChildAt(5), "Project Discussion", "10:00am - 11:30am")
        setTask(binding.taskList.getChildAt(6), "Data Science Seminar", "13:00pm - 15:00pm")
        setTask(binding.taskList.getChildAt(7), "Gym Break", "17:00pm - 18:00pm")
    }

    // Helper function to set task title and time
    private fun setTask(taskView: View, title: String, time: String) {
        val taskTitle = taskView.findViewById<TextView>(R.id.taskTitle)
        val taskTime = taskView.findViewById<TextView>(R.id.taskTime)

        taskTitle.text = title
        taskTime.text = time
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

