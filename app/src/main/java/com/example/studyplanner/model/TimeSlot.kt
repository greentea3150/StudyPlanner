package com.example.studyplanner.model

data class TimeSlot(
    val hour: String, // e.g., "00:00", "01:00"
    val tasks: List<Task> // Tasks for this time slot
)
