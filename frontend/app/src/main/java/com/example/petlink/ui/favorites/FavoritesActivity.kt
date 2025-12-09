package com.example.petlink.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.petlink.R
import com.example.petlink.data.model.AnimalPhotoReq
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.data.model.StatusReq
import com.example.petlink.ui.auth.LoginActivity
import com.example.petlink.ui.home.AnimalDetailActivity
import com.example.petlink.util.BottomNavHelper
import com.example.petlink.util.RetrofitClient
import com.example.petlink.util.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FavoritesActivity : AppCompatActivity() {

    private lateinit var grid: GridLayout
    private lateinit var progress: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var guestHint: TextView
    private lateinit var guestLoginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        BottomNavHelper.wire(this)

        grid = findViewById(R.id.grid_placeholder)
        progress = findViewById(R.id.progress)
        emptyView = findViewById(R.id.emptyView)
        guestHint = findViewById(R.id.guest_hint_favorites)
        guestLoginButton = findViewById(R.id.btn_guest_login_favorites)

        loadFavorites()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        val isGuest = UserSession.isGuestMode(this)

        if (token.isNullOrEmpty()) {
            grid.removeAllViews()
            progress.visibility = View.GONE
            emptyView.visibility = View.VISIBLE

            if (isGuest) {
                emptyView.text = "Авторизуйтесь для доступа к избранному"
                val container = findViewById<android.widget.LinearLayout>(R.id.favorites_container)
                container?.gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.CENTER_VERTICAL
                guestHint.visibility = View.VISIBLE
                guestLoginButton.visibility = View.VISIBLE
                guestLoginButton.setOnClickListener {
                    UserSession.setGuestMode(this, false)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
//                Toast.makeText(this, "Войдите в аккаунт, чтобы использовать избранное", Toast.LENGTH_SHORT).show()
            } else {
                emptyView.text = "Войдите, чтобы увидеть избранное"
                guestHint.visibility = View.GONE
                guestLoginButton.visibility = View.GONE
                Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            }
            return
        }

        progress.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        grid.removeAllViews()

        var animalsList = listOf<AnimalReq>()
        var photosList = listOf<AnimalPhotoReq>()
        var statusesList = listOf<StatusReq>()

        var animalsLoaded = false
        var photosLoaded = false
        var statusesLoaded = false

        fun tryRender() {
            if (animalsLoaded && photosLoaded && statusesLoaded) {
                progress.visibility = View.GONE
                if (animalsList.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Список избранного пуст"
                } else {
                    emptyView.visibility = View.GONE
                    renderGridWithPhotos(grid, animalsList, photosList, statusesList)
                }
            }
        }

        RetrofitClient.apiService.getFavoriteAnimals("Token $token").enqueue(object : Callback<List<AnimalReq>> {
            override fun onResponse(call: Call<List<AnimalReq>>, response: Response<List<AnimalReq>>) {
                if (!response.isSuccessful) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@FavoritesActivity, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                animalsList = response.body().orEmpty()
                animalsLoaded = true
                tryRender()
            }

            override fun onFailure(call: Call<List<AnimalReq>>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(this@FavoritesActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        RetrofitClient.apiService.getAnimalPhotos().enqueue(object : Callback<List<AnimalPhotoReq>> {
            override fun onResponse(call: Call<List<AnimalPhotoReq>>, response: Response<List<AnimalPhotoReq>>) {
                if (!response.isSuccessful) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@FavoritesActivity, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                photosList = response.body().orEmpty()
                photosLoaded = true
                tryRender()
            }

            override fun onFailure(call: Call<List<AnimalPhotoReq>>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(this@FavoritesActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

        RetrofitClient.apiService.getStatuses().enqueue(object : Callback<List<StatusReq>> {
            override fun onResponse(call: Call<List<StatusReq>>, response: Response<List<StatusReq>>) {
                if (!response.isSuccessful) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@FavoritesActivity, "Ошибка загрузки: ${response.code()}", Toast.LENGTH_SHORT).show()
                    return
                }
                statusesList = response.body().orEmpty()
                statusesLoaded = true
                tryRender()
            }

            override fun onFailure(call: Call<List<StatusReq>>, t: Throwable) {
                progress.visibility = View.GONE
                Toast.makeText(this@FavoritesActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun renderGridWithPhotos(
        grid: GridLayout,
        animals: List<AnimalReq>,
        photos: List<AnimalPhotoReq>,
        statuses: List<StatusReq>
    ) {
        grid.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val photosByAnimal = photos.groupBy { it.animal_id }
        val statusMap = statuses.associateBy { it.id }

        for (animal in animals) {
            val v = inflater.inflate(R.layout.item_ad, grid, false)
            val title = v.findViewById<TextView>(R.id.ad_title)
            val price = v.findViewById<TextView>(R.id.ad_price)
            val image = v.findViewById<ImageView>(R.id.ad_image)
            val status = v.findViewById<TextView>(R.id.ad_badge)

            v.setOnClickListener {
                val intent = android.content.Intent(this, AnimalDetailActivity::class.java)
                intent.putExtra("animal_id", animal.id.toInt())
                startActivity(intent)
            }

            title.text = animal.name ?: "Без названия"
            price.text = animal.price?.toString() ?: ""

            val list = photosByAnimal[animal.id] ?: emptyList()
            val mainPhoto = list.firstOrNull { it.is_main == true }
                ?: list.minByOrNull { it.order }
                ?: list.firstOrNull()

            if (mainPhoto?.photo_url != null && mainPhoto.photo_url.isNotEmpty()) {
                Glide.with(this).load(mainPhoto.photo_url).centerCrop().into(image)
            } else {
                Glide.with(this).load(R.drawable.cat).centerCrop().into(image)
            }
            val statusObj = statusMap[animal.status]
            if (statusObj != null) {
                status.text = statusObj.name
            } else {
                status.text = "Статус не найден"
            }

            grid.addView(v)
        }
    }
}
