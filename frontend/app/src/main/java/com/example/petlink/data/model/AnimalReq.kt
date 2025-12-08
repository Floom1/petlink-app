package com.example.petlink.data.model

data class AnimalReq(
    val id: Long,
    val name: String?,
    val price: Double?,
    val imageUrl: String?,
    val status: Long,
    val is_favorite: Boolean? = null,
)
