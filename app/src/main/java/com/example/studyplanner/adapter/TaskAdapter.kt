package com.example.studyplanner.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.studyplanner.R
import com.example.studyplanner.model.Task
import com.google.firebase.firestore.FirebaseFirestore

class TaskAdapter(
    private var tasks: List<Task>,
    private val onItemClick: (Task) -> Unit,
    private val onDeleteClick: (String) -> Unit, // Add this parameter to handle delete action
    private val isHomePage: Boolean // Add a flag to determine if this is for the homepage
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

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
        private val cardView: CardView = itemView.findViewById(R.id.cardView) // CardView reference

        init {
            deleteButton?.setOnClickListener {
                tasks[adapterPosition].id?.let { onDeleteClick(it) }
            }
        }

        fun bind(task: Task) {
            taskNameTextView.text = task.taskName
            categoryTextView.text = task.category
            dateTimeTextView.text = "${task.date} | ${task.timeRange} - ${task.until}"
            statusTextView.text = task.status

            // Set click listener for the task item
            itemView.setOnClickListener { onItemClick(task) }

            // Fetch the category color and set the card color
            fetchCategoryColor(task.category, cardView)
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

    // Helper function to fetch the category color and set it on the card view
    private fun fetchCategoryColor(categoryName: String, cardView: CardView) {
        firestore.collection("Categories")
            .whereEqualTo("name", categoryName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val categoryDoc = documents.documents[0]
                    val color = categoryDoc.getLong("color")?.toInt() ?: -1
                    cardView.setCardBackgroundColor(color)
                } else {
                    cardView.setCardBackgroundColor(Color.GRAY) // Default color if not found
                }
            }
            .addOnFailureListener { e ->
                Log.e("TaskAdapter", "Error fetching category color", e)
                cardView.setCardBackgroundColor(Color.GRAY) // Default color on failure
            }
    }
}

