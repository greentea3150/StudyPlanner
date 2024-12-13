package com.example.studyplanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.studyplanner.model.Task
import com.google.firebase.firestore.FirebaseFirestore

class TaskNotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val taskId = inputData.getString("taskId") ?: return Result.failure()
        Log.d("TaskNotification", "Worker triggered for taskId: $taskId")

        val firestore = FirebaseFirestore.getInstance()
        val taskRef = firestore.collection("tasks").document(taskId)

        taskRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val task = document.toObject(Task::class.java)
                task?.let {
                    // Trigger the notification
                    Log.d("TaskNotification", "Sending notification for task: ${it.taskName}")
                    sendNotification(applicationContext, it)
                }
            }
        }
        return Result.success()
    }

    private fun sendNotification(context: Context, task: Task) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "studyplanner_channel",
                "Study Planner Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "studyplanner_channel")
            .setSmallIcon(R.drawable.ic_task)
            .setContentTitle("Task Reminder")
            .setContentText("It's time to start: ${task.taskName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(task.id.hashCode(), notification)
        Log.d("TaskNotification", "Notification sent for task: ${task.taskName}")
    }
}

