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
import com.example.petlink.data.model.TestResult
import com.example.petlink.data.model.AnimalSimpleResponse
import com.example.petlink.data.model.UserResponse
import com.example.petlink.data.model.AnimalApplication
import com.example.petlink.data.model.AnimalApplicationCreate
import com.example.petlink.data.model.NotificationItem

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
import retrofit2.http.DELETE


interface PetLinkApi {
    @POST("api/auth/register/")
    fun register(@Body registrationRequest: RegistrationRequest) : Call<AuthResponse>

    @POST("api-token-auth/")
    fun login(@Body loginRequest: LoginRequest): Call<AuthResponse>

    @GET("api/animals/")
    fun getAnimals(
        @Header("Authorization") authHeader: String? = null,
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
        @Query("has_vaccinations") hasVaccinations: Boolean? = null,
        @Query("mine") mine: Boolean? = null,
        @Query("status_name") statusName: String? = null,
        @Query("is_available") isAvailable: Boolean? = null
    ): Call<List<AnimalReq>>

//    @GET("api/animal_photos/")
//    fun getAnimalPhotos(): Call<List<AnimalPhotoReq>>

    @GET("api/animal_photos/")
    fun getAnimalPhotos(@Query("animal_id") animalId: Long? = null): Call<List<AnimalPhotoReq>>

    @POST("api/animal_photos/")
    fun createAnimalPhoto(
        @Header("Authorization") authHeader: String,
        @Body body: com.example.petlink.data.model.AnimalPhotoCreate
    ): Call<AnimalPhotoReq>

    @DELETE("api/animal_photos/{id}/")
    fun deleteAnimalPhoto(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Long
    ): Call<Void>

    @PATCH("api/animal_photos/{id}/")
    fun updateAnimalPhoto(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Long,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Call<AnimalPhotoReq>

    @GET("api/users/{id}/")
    fun getUser(@Path("id") id: Long): Call<UserResponse>

    @GET("api/statuses/")
    fun getStatuses(): Call<List<StatusReq>>

    @GET("api/species/")
    fun getSpecies(): Call<List<SpeciesReq>>

    @GET("api/breeds/")
    fun getBreeds(): Call<List<BreedReq>>

    @GET("api/breeds/{id}/")
    fun getBreed(@Path("id") id: Long): Call<BreedReq>


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

    @GET("api/animals/{id}/")
    fun getAnimalDetail(
        @Header("Authorization") authHeader: String? = null,
        @Path("id") id: Int
    ): Call<AnimalSimpleResponse>

    @POST("api/animals/")
    fun createAnimal(
        @Header("Authorization") authHeader: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Call<AnimalSimpleResponse>

    @PATCH("api/animals/{id}/")
    fun updateAnimal(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Int,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Call<AnimalSimpleResponse>

    @DELETE("api/animals/{id}/")
    fun deleteAnimal(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Int
    ): Call<Void>

    // Applications (animal)
    @POST("api/animal_apps/")
    fun createAnimalApplication(
        @Header("Authorization") authHeader: String,
        @Body body: AnimalApplicationCreate
    ): Call<AnimalApplication>

    @GET("api/animal_apps/")
    fun getAnimalApplications(
        @Header("Authorization") authHeader: String,
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("animal") animal: Int? = null
    ): Call<List<AnimalApplication>>

    @PATCH("api/animal_apps/{id}/")
    fun updateAnimalApplicationStatus(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Int,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Call<AnimalApplication>

    @GET("api/animal_apps/{id}/")
    fun getAnimalApplication(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Int
    ): Call<AnimalApplication>

    @GET("api/notifications/")
    fun getNotifications(
        @Header("Authorization") authHeader: String,
        @Query("only_unread") onlyUnread: Boolean? = true
    ): Call<List<NotificationItem>>

    @POST("api/notifications/mark-as-read/")
    fun markNotificationsAsRead(
        @Header("Authorization") authHeader: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Call<Map<String, Any>>

    // Favorites
    @GET("api/animals/favorites/")
    fun getFavoriteAnimals(
        @Header("Authorization") authHeader: String
    ): Call<List<AnimalReq>>

    @POST("api/animals/{id}/favorite/")
    fun addFavorite(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Int
    ): Call<AnimalSimpleResponse>

    @DELETE("api/animals/{id}/favorite/")
    fun removeFavorite(
        @Header("Authorization") authHeader: String,
        @Path("id") id: Int
    ): Call<Void>

}