package com.example.petlink.data.model

data class AuthResponse(
    val token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Int,
    val email: String,
    val full_name: String,
    val is_shelter: Boolean
)