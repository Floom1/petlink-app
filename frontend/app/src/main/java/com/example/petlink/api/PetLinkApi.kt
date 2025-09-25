package com.example.petlink.api

import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.LoginRequest
import com.example.petlink.data.model.RegistrationRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface PetLinkApi {
    @POST("api/auth/register/")
    fun register(@Body registrationRequest: RegistrationRequest) : Call<AuthResponse>

    @POST("api-token-auth/")
    fun login(@Body loginRequest: LoginRequest): Call<AuthResponse>

}