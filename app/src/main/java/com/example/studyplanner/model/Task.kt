package com.example.studyplanner.model

import java.io.Serializable

data class Task(
    val id: String = "",
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