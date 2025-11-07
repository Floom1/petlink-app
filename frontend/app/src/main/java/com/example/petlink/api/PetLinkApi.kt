package com.example.petlink.api

import com.example.petlink.data.model.AnimalPhotoReq
import com.example.petlink.data.model.UploadResponse
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.LoginRequest
import com.example.petlink.data.model.RegistrationRequest
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.data.model.StatusReq
import com.example.petlink.data.model.SpeciesReq
import com.example.petlink.data.model.BreedReq
import com.example.petlink.data.model.UserResponse
import com.example.petlink.data.model.TestResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Header
import retrofit2.http.Path


interface PetLinkApi {
    @POST("api/auth/register/")
    fun register(@Body registrationRequest: RegistrationRequest) : Call<AuthResponse>

    @POST("api-token-auth/")
    fun login(@Body loginRequest: LoginRequest): Call<AuthResponse>

    @GET("api/animals/")
    fun getAnimals(
        @Query("species") speciesId: Int? = null,
        @Query("breed") breedId: Int? = null,
        @Query("gender") gender: String? = null,
        @Query("age_min") ageMin: Double? = null,
        @Query("age_max") ageMax: Double? = null,
        @Query("price_min") priceMin: Double? = null,
        @Query("price_max") priceMax: Double? = null,
        @Query("is_hypoallergenic") isHypoallergenic: Boolean? = null,
        @Query("child_friendly") childFriendly: Boolean? = null,
        @Query("space_requirements") spaceRequirements: String? = null,
        @Query("is_sterilized") isSterilized: Boolean? = null,
        @Query("has_vaccinations") hasVaccinations: Boolean? = null
    ): Call<List<AnimalReq>>

    @GET("api/animal_photos/")
    fun getAnimalPhotos(): Call<List<AnimalPhotoReq>>

    @GET("api/statuses/")
    fun getStatuses(): Call<List<StatusReq>>

    @GET("api/species/")
    fun getSpecies(): Call<List<SpeciesReq>>

    @GET("api/breeds/")
    fun getBreeds(): Call<List<BreedReq>>

    @GET("api/auth/user/")
    fun me(@Header("Authorization") authHeader: String): Call<UserResponse>

    @PATCH("api/users/{id}/")
    fun updateUser(
        @Header("Authorization") authHeader: String,
        @Path("id") userId: Int,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Call<UserResponse>

    @Multipart
    @POST("api/upload/")
    fun uploadProfileImage(
        @Header("Authorization") authHeader: String,
        @Part image: MultipartBody.Part
    ): Call<UploadResponse>

    @POST("api/tests/")
    fun submitTest(
        @Header("Authorization") authHeader: String,
        @Body testResult: TestResult
    ): Call<TestResult>

    @GET("api/tests/my_recommendations/")
    fun getMyRecommendations(
        @Header("Authorization") authHeader: String
    ): Call<Map<String, Any>>

    @GET("api/tests/recommended_animals/")
    fun getRecommendedAnimals(
        @Header("Authorization") authHeader: String
    ): Call<List<AnimalReq>>

}