package com.example.petlink.data.model


data class RegistrationRequest(
    val email: String,
    val full_name: String,
    val password: String,
    val is_shelter: Boolean
)