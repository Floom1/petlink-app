package com.example.petlink.api

import com.example.petlink.data.model.AnimalPhotoReq
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.LoginRequest
import com.example.petlink.data.model.RegistrationRequest
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.data.model.StatusReq
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface PetLinkApi {
    @POST("api/auth/register/")
    fun register(@Body registrationRequest: RegistrationRequest) : Call<AuthResponse>

    @POST("api-token-auth/")
    fun login(@Body loginRequest: LoginRequest): Call<AuthResponse>

    @GET("api/animals/")
    fun getAnimals(): Call<List<AnimalReq>>

    @GET("api/animal_photos/")
    fun getAnimalPhotos(): Call<List<AnimalPhotoReq>>

    @GET("api/statuses/")
    fun getStatuses(): Call<List<StatusReq>>



}