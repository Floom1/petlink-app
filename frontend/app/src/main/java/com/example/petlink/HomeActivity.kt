package com.example.petlink

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.petlink.data.model.AnimalPhotoReq
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.data.model.StatusReq
import com.example.petlink.util.RetrofitClient
import okhttp3.internal.notifyAll
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class HomeActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_1)
        loadAnimals()
    }

    private fun loadAnimals() {
        var animalsList = listOf<AnimalReq>()
        var photosList = listOf<AnimalPhotoReq>()
        var statusesList = listOf<StatusReq>()

        var statusesLoaded = false
        var animalsLoaded = false
        var photosLoaded = false

        val animalsCall = RetrofitClient.apiService.getAnimals()
        animalsCall.enqueue(object: Callback<List<AnimalReq>> {
            override fun onResponse(
                call: Call<List<AnimalReq>>, response: Response<List<AnimalReq>>
            ) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                animalsList = response.body().orEmpty()
                animalsLoaded = true
                if (photosLoaded && statusesLoaded) {
                    renderGridWithPhotos(animalsList, photosList, statusesList)
                }
            }

            override fun onFailure(call: Call<List<AnimalReq>>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        val photosCall = RetrofitClient.apiService.getAnimalPhotos()
        photosCall.enqueue(object: Callback<List<AnimalPhotoReq>> {
            override fun onResponse(
                call2: Call<List<AnimalPhotoReq>>, response: Response<List<AnimalPhotoReq>>
            ) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                photosList = response.body().orEmpty()
                photosLoaded = true
                if (animalsLoaded && statusesLoaded) {
                    renderGridWithPhotos(animalsList, photosList, statusesList)
                }
            }

            override fun onFailure(call: Call<List<AnimalPhotoReq>>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        val statusCall = RetrofitClient.apiService.getStatuses()
        statusCall.enqueue(object: Callback<List<StatusReq>> {
            override fun onResponse(
                call3: Call<List<StatusReq>>, response: Response<List<StatusReq>>
            ) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@HomeActivity, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                statusesList = response.body().orEmpty()
                statusesLoaded = true
                if (animalsLoaded && photosLoaded) {
                    renderGridWithPhotos(animalsList, photosList, statusesList)
                }
            }

            override fun onFailure(call: Call<List<StatusReq>>, t: Throwable) {
                Toast.makeText(this@HomeActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderGridWithPhotos(animals: List<AnimalReq>, photos: List<AnimalPhotoReq>, statuses: List<StatusReq>) {
        val grid = findViewById<GridLayout>(R.id.grid_placeholder)
        if (grid == null) return
        grid.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val photosMap = photos.associateBy { it.animal_id }
        val statusMap = statuses.associateBy { it.id }

        for (animal in animals) {
            val v = inflater.inflate(R.layout.item_ad, grid, false)
            val title = v.findViewById<TextView>(R.id.ad_title)
            val price = v.findViewById<TextView>(R.id.ad_price)
            val image = v.findViewById<ImageView>(R.id.ad_image)
            val status = v.findViewById<TextView>(R.id.ad_badge)

            title.text = animal.name ?: "Без названия"
            price.text = animal.price.toString()

            val photo = photosMap[animal.id]

            if (photo?.photo_url != null && photo.photo_url.isNotEmpty()) {
                Glide.with(this).load(photo.photo_url).centerCrop().into(image)
            } else {
                Glide.with(this).load(R.drawable.cat).centerCrop().into(image)
            }
            val status_ = statusMap[animal.status]
            if (status_ != null) {
                status.text = status_.name ?: "Неизвестно"
            } else {
                status.text = "Статус не найден"
            }
            grid.addView(v)
        }
    }
}
