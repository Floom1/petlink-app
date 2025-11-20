package com.example.petlink

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.petlink.adapter.AnimalPhotoAdapter
import com.example.petlink.api.PetLinkApi
import com.example.petlink.data.model.AnimalDetailResponse
import com.example.petlink.data.model.AnimalPhotoReq
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import com.example.petlink.data.model.AnimalSimpleResponse
import com.example.petlink.data.model.BreedReq
import com.example.petlink.data.model.UserResponse
import com.example.petlink.util.RetrofitClient

class AnimalDetailActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var btnFavorite: ImageButton
    private lateinit var tagsContainer: LinearLayout
    private lateinit var tvAnimalName: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvBreed: TextView
    private lateinit var tvAge: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvColor: TextView
    private lateinit var tvHabits: TextView
    private lateinit var btnApply: Button
    private lateinit var tvSellerName: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private var animalId: Long = 0
    private var isFavorite: Boolean = false
    private var api: PetLinkApi? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animal_detail)

        // Initialize views
        initViews()

        // Get animal ID from intent
        animalId = intent.getLongExtra("animal_id", 0)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)


        // Initialize Retrofit
        api = RetrofitClient.apiService
        // Load animal details
        loadAnimalDetails()

        // Set up favorite button click listener
        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Set up apply button click listener
        btnApply.setOnClickListener {
            handleApplyButtonClick()
        }
        var btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        indicatorLayout = findViewById(R.id.indicatorLayout)
        btnFavorite = findViewById(R.id.btnFavorite)
        tagsContainer = findViewById(R.id.tagsContainer)
        tvAnimalName = findViewById(R.id.tvAnimalName)
        tvPrice = findViewById(R.id.tvPrice)
        tvDescription = findViewById(R.id.tvDescription)
        tvBreed = findViewById(R.id.tvBreed)
        tvAge = findViewById(R.id.tvAge)
        tvGender = findViewById(R.id.tvGender)
        tvColor = findViewById(R.id.tvColor)
        tvHabits = findViewById(R.id.tvHabits)
        btnApply = findViewById(R.id.btnApply)
        tvSellerName = findViewById(R.id.tvSellerName)
    }

    private fun loadAnimalDetails() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val token = sharedPreferences.getString("auth_token", null)
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "Необходимо войти в аккаунт", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        // Загружаем основные данные животного
        api?.getAnimalDetail(animalId.toInt())?.enqueue(object : Callback<AnimalSimpleResponse> {
            override fun onResponse(
                call: Call<AnimalSimpleResponse>,
                response: Response<AnimalSimpleResponse>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { animal ->
                        // После получения основных данных, загружаем дополнительные данные
                        loadAdditionalData(animal)
                    }
                } else {
                    handleError("Ошибка загрузки данных животного: ${response.code()}")
                    progressBar.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) {
                handleError("Ошибка соединения: ${t.message}")
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun loadAdditionalData(animal: AnimalSimpleResponse) {
        var userLoaded = false
        var breedLoaded = false
        var photosLoaded = false

        var user: UserResponse? = null
        var breed: BreedReq? = null
        var photos: List<AnimalPhotoReq> = emptyList()

        fun tryDisplay() {
            if (userLoaded && breedLoaded && photosLoaded) {
                val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                progressBar.visibility = View.GONE

                // Создаем временный объект с полными данными для отображения
                displayFullAnimalDetails(animal, user, breed, photos)
            }
        }

        // Загружаем пользователя
        if (animal.user != null) {
            api?.getUser(animal.user.toLong())?.enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        user = response.body()
                    }
                    userLoaded = true
                    tryDisplay()
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    userLoaded = true
                    tryDisplay()
                }
            })
        } else {
            userLoaded = true
            tryDisplay()
        }

        // Загружаем породу
        if (animal.breed != null) {
            api?.getBreed(animal.breed.toLong())?.enqueue(object : Callback<BreedReq> {
                override fun onResponse(call: Call<BreedReq>, response: Response<BreedReq>) {
                    if (response.isSuccessful) {
                        breed = response.body()
                    }
                    breedLoaded = true
                    tryDisplay()
                }

                override fun onFailure(call: Call<BreedReq>, t: Throwable) {
                    breedLoaded = true
                    tryDisplay()
                }
            })
        } else {
            breedLoaded = true
            tryDisplay()
        }

        // Загружаем фотографии
        api?.getAnimalPhotos(animalId)?.enqueue(object : Callback<List<AnimalPhotoReq>> {
            override fun onResponse(call: Call<List<AnimalPhotoReq>>, response: Response<List<AnimalPhotoReq>>) {
                if (response.isSuccessful) {
                    photos = response.body() ?: emptyList()
                }
                photosLoaded = true
                tryDisplay()
            }

            override fun onFailure(call: Call<List<AnimalPhotoReq>>, t: Throwable) {
                photosLoaded = true
                tryDisplay()
            }
        })
    }


    private fun displayFullAnimalDetails(
        animal: AnimalSimpleResponse,
        user: UserResponse?,
        breed: BreedReq?,
        photos: List<AnimalPhotoReq>
    ) {
        // Set animal name
        tvAnimalName.text = animal.name ?: "Без имени"

        // Set price (if null or 0 - show "Бесплатно")
        tvPrice.text = if (animal.price == null || animal.price == 0.0) {
            "Бесплатно"
        } else {
            "${animal.price} руб."
        }

        // Set description
        tvDescription.text = animal.description ?: "Описание отсутствует"

        // Set breed
        tvBreed.text = breed?.name ?: "Не указано"

        // Set age
        if (animal.age != null) {
            tvAge.text = formatAge(animal.age)
            tvAge.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvAgeLabel).visibility = View.VISIBLE
        } else {
            tvAge.visibility = View.GONE
            findViewById<TextView>(R.id.tvAgeLabel).visibility = View.GONE
        }

        // Set gender
        tvGender.text = when (animal.gender) {
            "M" -> "Мужской"
            "F" -> "Женский"
            else -> "Не указан"
        }

        // Set color
        tvColor.text = animal.color ?: "Не указан"

        // Set habits
        if (animal.habits != null) {
            tvHabits.text = animal.habits
            tvHabits.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvHabitsLabel).visibility = View.VISIBLE
        } else {
            tvHabits.visibility = View.GONE
            findViewById<TextView>(R.id.tvHabitsLabel).visibility = View.GONE
        }

        // Set seller info
        if (user != null) {
            tvSellerName.text = if (user.is_shelter == true) {
                user.shelter_name ?: user.full_name ?: "Не указано"
            } else {
                user.full_name ?: "Не указано"
            }
        } else {
            tvSellerName.text = "Не указано"
        }

        // Setup photo ViewPager
        setupPhotoViewPager(photos)

        // Generate tags
        generateTags(animal)
    }

    private fun formatAge(age: Double): String {
        val years = age.toInt()
        return when (years % 10) {
            1 -> if (years % 100 == 11) "$years лет" else "$years год"
            2, 3, 4 -> if (years % 100 in 12..14) "$years лет" else "$years года"
            else -> "$years лет"
        }
    }

    private fun handleError(message: String) {
        Log.e("AnimalDetailActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



    private fun setupPhotoViewPager(photos: List<AnimalPhotoReq>) {
        val adapter = AnimalPhotoAdapter(photos)
        viewPager.adapter = adapter

        // Setup indicator
        setupIndicator(photos.size)

        // Update indicator when page changes
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                updateIndicator(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
    }

    private fun setupIndicator(count: Int) {
        indicatorLayout.removeAllViews()

        if (count <= 1) {
            indicatorLayout.visibility = View.GONE
            return
        }

        indicatorLayout.visibility = View.VISIBLE

        val indicators = arrayOfNulls<ImageView>(count)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 0, 8, 0)
        }

        for (i in 0 until count) {
            indicators[i] = ImageView(this)
            indicators[i]?.setImageDrawable(
                getIndicatorDrawable(if (i == 0) Color.BLACK else Color.GRAY)
            )
            indicatorLayout.addView(indicators[i], params)
        }
    }

    private fun updateIndicator(selectedPosition: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            val color = if (i == selectedPosition) Color.BLACK else Color.GRAY
            imageView.setImageDrawable(getIndicatorDrawable(color))
        }
    }

    private fun getIndicatorDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(16, 16)
        }
    }

    private fun generateTags(animal: AnimalSimpleResponse) {
        tagsContainer.removeAllViews()

        val tags = mutableListOf<String>()

        if (animal.is_sterilized == true) {
            tags.add("Стерилизован")
        }

        if (animal.has_vaccinations == true) {
            tags.add("Есть прививки")
        }

        if (animal.is_hypoallergenic == true) {
            tags.add("Гипоаллергенный")
        }

        if (animal.child_friendly == true) {
            tags.add("Дружелюбен к детям")
        }

        when (animal.space_requirements) {
            "low" -> tags.add("Низкие требования к пространству")
            "medium" -> tags.add("Средние требования к пространству")
            "high" -> tags.add("Высокие требования к пространству")
        }

        // Create tags layout (2 per row)
        val tagLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 16, 16)
        }

        var currentRow: LinearLayout? = null

        tags.forEachIndexed { index, tagText ->
            if (index % 2 == 0) {
                // Create new row
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                tagsContainer.addView(currentRow)
            }

            // Create tag view
            val tagView = TextView(this).apply {
                text = tagText
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.tag_background)
                layoutParams = tagLayoutParams
            }

            currentRow?.addView(tagView)
        }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        // TODO: Implement API call to save favorite status
        Toast.makeText(this, if (isFavorite) "Добавлено в избранное" else "Удалено из избранного", Toast.LENGTH_SHORT).show()
    }

    private fun handleApplyButtonClick() {
        // Check if compatibility test is completed
        val testCompleted = sharedPreferences.getBoolean("test_completed", false)

        if (!testCompleted) {
            // Show dialog suggesting to complete the test
            AlertDialog.Builder(this)
                .setTitle("Пройдите тест совместимости")
                .setMessage("Для подачи заявки необходимо пройти тест совместимости. Перейти к тесту?")
                .setPositiveButton("Пройти тест") { _, _ ->
                    // Navigate to test activity
                    val intent = Intent(this, TestActivity::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Подать заявку")
                .setMessage("Вы уверены, что хотите подать заявку на это животное?")
                .setPositiveButton("Да") { _, _ ->
                    // TODO: Implement API call to submit application
                    Toast.makeText(this, "Заявка отправлена!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }
}