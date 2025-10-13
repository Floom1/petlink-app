package com.example.petlink.data.model

data class TestResult(
    val id: Int? = null,
    val user_id: Int? = null,
    val residence_type: String?,
    val weekday_time: String?,
    val has_children: Boolean,
    val planned_move: Boolean,
    val pet_experience: String?,
    val has_allergies: Boolean,
    val created_at: String? = null
)
