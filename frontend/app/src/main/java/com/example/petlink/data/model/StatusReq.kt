package com.example.petlink.data.model

data class StatusReq(
    val id: Long,
    val name: String,
    val is_available: Boolean? = null,
)
