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
    val timeRange: String = "",
    val until: String = "",
    val userId: String = ""
) : Serializable {
    constructor() : this("", "", "", "", "", "", "", "", "", "")
}
