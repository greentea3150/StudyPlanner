package com.example.studyplanner.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.R
import com.example.studyplanner.TaskDetailsFragment
import com.example.studyplanner.model.Task
import com.example.studyplanner.model.TimeSlot

class TimeslotAdapter(
    private val timeSlots: List<TimeSlot>
) : RecyclerView.Adapter<TimeslotAdapter.TimeSlotViewHolder>() {

    class TimeSlotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.timeLabel)
        val tasksRecyclerView: RecyclerView = view.findViewById(R.id.recyclerView_timeSlot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        holder.timeTextView.text = timeSlot.hour

        holder.tasksRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.tasksRecyclerView.adapter = TaskAdapter(
            tasks = timeSlot.tasks,
            onItemClick = { task -> showTaskDetails(task, holder.itemView.context) },
            onDeleteClick = { taskId -> /* Handle delete */ },
            isHomePage = true
        )
    }

    private fun showTaskDetails(task: Task, context: Context) {
        val taskDetailFragment = TaskDetailsFragment.newInstance(task)

        // Check if it's a valid context (activity) before attempting to start the fragment transaction
        if (context is AppCompatActivity) {
            context.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, taskDetailFragment) // Replace with your container ID
                .addToBackStack(null) // Add to back stack for navigation
                .commit()
        }
    }

    override fun getItemCount(): Int = timeSlots.size
}
