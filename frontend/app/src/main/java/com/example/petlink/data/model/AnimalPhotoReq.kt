package com.example.petlink.data.model

data class AnimalPhotoReq(
    val id: Long,
    val animal_id: Long,
    val photo_url: String?,
    val is_main: Boolean?,
    val order: Long,
)
