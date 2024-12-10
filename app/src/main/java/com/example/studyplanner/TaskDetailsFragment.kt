package com.example.studyplanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.studyplanner.R
import com.example.studyplanner.model.Task

class TaskDetailsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_task_details, container, false)

        // Get the Task object from arguments
        val task = arguments?.getSerializable("task") as? Task

        // Bind data to views
        task?.let {
            view.findViewById<TextView>(R.id.task_title).text = it.taskName
            view.findViewById<TextView>(R.id.task_date).text = it.date
            view.findViewById<TextView>(R.id.et_date).text = it.date
            view.findViewById<TextView>(R.id.et_time_range).text = "${it.timeRange} - ${it.until}"
            view.findViewById<TextView>(R.id.et_task_name).text = it.taskName
            view.findViewById<TextView>(R.id.tvTaskType2).text = it.category
            view.findViewById<TextView>(R.id.tvTaskStatus).text = it.status
            view.findViewById<TextView>(R.id.tv_objective_details).text = it.objective
            view.findViewById<TextView>(R.id.tv_materials_details).text = it.materialsNeeded
        }

        return view
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
