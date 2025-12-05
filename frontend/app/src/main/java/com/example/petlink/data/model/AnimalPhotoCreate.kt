package com.example.petlink.data.model

data class AnimalPhotoCreate(
    val animal_id_write: Int,
    val photo_url: String,
    val is_main: Boolean? = false,
    val order: Int? = 0
)
