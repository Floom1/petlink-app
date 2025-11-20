package com.example.petlink.data.model

data class AnimalSimpleResponse(
    val id: Long,
    val name: String?,
    val price: Double?,
    val imageUrl: String?,
    val status: Long,
    val breed: Int?,
    val age: Double?,
    val gender: String?,
    val color: String?,
    val description: String?,
    val is_sterilized: Boolean?,
    val has_vaccinations: Boolean?,
    val habits: String?,
    val is_hypoallergenic: Boolean?,
    val child_friendly: Boolean?,
    val space_requirements: String?,
    val user: Int?
)