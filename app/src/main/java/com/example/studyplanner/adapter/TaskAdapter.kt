package com.example.studyplanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.R
import com.example.studyplanner.model.Task

class TaskAdapter(
    private var tasks: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onDeleteClick: (String) -> Unit, // Add this parameter to handle delete action
    private val isHomePage: Boolean // Add a flag to determine if this is for the homepage
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Define the view types
    companion object {
        const val VIEW_TYPE_HOME = 0
        const val VIEW_TYPE_DEFAULT = 1
    }

    override fun getItemViewType(position: Int): Int {
        // Check if this item is for the homepage
        return if (isHomePage) VIEW_TYPE_HOME else VIEW_TYPE_DEFAULT
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskNameTextView: TextView = itemView.findViewById(R.id.taskNameTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val dateTimeTextView: TextView = itemView.findViewById(R.id.dateTimeTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val deleteButton: Button? = itemView.findViewById(R.id.deleteButton)

        init {
            // Check if the edit and delete buttons are present
            if (deleteButton == null) {
                // If not, set click listeners to null or handle this case gracefully
                itemView.findViewById<Button>(R.id.deleteButton)?.setOnClickListener(null)
            }
        }

        fun bind(task: Task) {
            taskNameTextView.text = task.taskName
            categoryTextView.text = task.category
            dateTimeTextView.text = "${task.date} | ${task.timeRange} - ${task.until}"
            statusTextView.text = task.status

            // Set click listener for the task item
            itemView.setOnClickListener { onItemClick(task) }

            // Check if the deleteButton exists and set a click listener
            deleteButton?.setOnClickListener {
                // Pass the task's ID to the onDeleteClick function to delete it
                onDeleteClick(task.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View
        if (viewType == VIEW_TYPE_HOME) {
            view = layoutInflater.inflate(R.layout.item_task_home, parent, false)
        } else {
            view = layoutInflater.inflate(R.layout.item_task, parent, false)
        }
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    // Helper function to find the correct time slot index based on task's time range
    fun getTimeSlotIndex(task: Task): Int {
        // Use the hour of start time (HH:mm) to determine the correct time slot
        val startHour = task.getStartHour()
        return startHour // Assuming your time slots are based on hour intervals (0:00 to 23:00)
    }
}
