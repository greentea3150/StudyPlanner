package com.example.studyplanner.model

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Task(
    @DocumentId
    val id: String = "",  // Firestore document ID is automatically mapped to this field
    val category: String = "",
    val date: String = "",
    val materialsNeeded: String = "",
    val objective: String = "",
    val status: String = "",
    val taskName: String = "",
    val timeRange: String = "",  // Start time in "HH:mm" format
    val until: String = "",  // End time in "HH:mm" format
    val userId: String = ""
) : Serializable {
    constructor() : this("", "", "", "", "", "", "", "", "", "")

    // Helper function to extract the hour (int) from the time string
    fun getStartHour(): Int {
        return timeRange.split(":")[0].toInt()
    }

}
