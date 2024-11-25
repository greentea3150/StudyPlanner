package com.example.studyplanner.model

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
) {
    constructor() : this("", "", "", "", "", "", "", "", "", "")
}