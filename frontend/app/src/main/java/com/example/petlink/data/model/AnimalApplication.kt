package com.example.petlink.data.model

data class AnimalApplicationCreate(
    val animal: Int,
    val message: String?
)

data class AnimalApplication(
    val id: Int,
    val animal: Int,
    val user: Int,
    val message: String?,
    val status: String,
    val risk_info: String?,
    val animal_name: String?,
    val buyer_name: String?,
    val created_at: String?,
    val updated_at: String?
)
