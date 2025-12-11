package com.example.petlink.ui.home

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.petlink.adapter.AnimalPhotoAdapter
import com.example.petlink.api.PetLinkApi
import com.example.petlink.data.model.AnimalPhotoReq
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
import com.example.petlink.data.model.AnimalApplication
import com.example.petlink.data.model.AnimalApplicationCreate
import android.text.InputFilter
import android.os.Handler
import android.os.Looper
import com.example.petlink.ui.auth.LoginActivity
import com.example.petlink.R
import com.example.petlink.ui.test.TestActivity
import org.json.JSONObject

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

    private var animalId: Int = 0
    private var isFavorite: Boolean = false
    private var api: PetLinkApi? = null
    private var sellerUser: UserResponse? = null
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animal_detail)

        initViews()

        animalId = intent.getIntExtra("animal_id", 0)

        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        val isGuest = com.example.petlink.util.UserSession.isGuestMode(this)
        if (isGuest) {
            btnApply.visibility = View.GONE
            btnFavorite.visibility = View.GONE
        }

        api = RetrofitClient.apiService
        fetchCurrentUser()
        loadAnimalDetails()

        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

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
        val isGuest = com.example.petlink.util.UserSession.isGuestMode(this)

        if (token.isNullOrEmpty() && !isGuest) {
            Toast.makeText(this, "Необходимо войти в аккаунт", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        val authHeader = if (token.isNullOrEmpty()) null else "Token $token"

        api?.getAnimalDetail(authHeader, animalId)?.enqueue(object : Callback<AnimalSimpleResponse> {
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

    private fun fetchCurrentUser() {
        val token = sharedPreferences.getString("auth_token", null) ?: return
        RetrofitClient.apiService.me("Token $token").enqueue(object: Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful) {
                    currentUser = response.body()
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) { }
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
                        sellerUser = user
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
        api?.getAnimalPhotos(animalId.toLong())?.enqueue(object : Callback<List<AnimalPhotoReq>> {
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
        tvAnimalName.text = animal.name ?: "Без имени"

        tvPrice.text = if (animal.price == null || animal.price == 0.0) {
            "Бесплатно"
        } else {
            "${animal.price} руб."
        }

        tvDescription.text = animal.description ?: "Описание отсутствует"

        tvBreed.text = breed?.name ?: "Не указано"

        if (animal.age != null) {
            tvAge.text = formatAge(animal.age)
            tvAge.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvAgeLabel).visibility = View.VISIBLE
        } else {
            tvAge.visibility = View.GONE
            findViewById<TextView>(R.id.tvAgeLabel).visibility = View.GONE
        }

        tvGender.text = when (animal.gender) {
            "M" -> "Мужской"
            "F" -> "Женский"
            else -> "Не указан"
        }

        tvColor.text = animal.color ?: "Не указан"

        if (animal.habits != null) {
            tvHabits.text = animal.habits
            tvHabits.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvHabitsLabel).visibility = View.VISIBLE
        } else {
            tvHabits.visibility = View.GONE
            findViewById<TextView>(R.id.tvHabitsLabel).visibility = View.GONE
        }

        if (user != null) {
            tvSellerName.text = if (user.is_shelter == true) {
                user.shelter_name ?: user.full_name ?: "Не указано"
            } else {
                user.full_name ?: "Не указано"
            }
        } else {
            tvSellerName.text = "Не указано"
        }

        setupPhotoViewPager(photos)

        generateTags(animal)

        isFavorite = animal.is_favorite == true
        updateFavoriteIcon()
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

        setupIndicator(photos.size)

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

        val tagLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 16, 16)
        }

        var currentRow: LinearLayout? = null

        tags.forEachIndexed { index, tagText ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                tagsContainer.addView(currentRow)
            }

            val tagView = TextView(this).apply {
                text = tagText
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.tag_background)
                layoutParams = tagLayoutParams
            }

            currentRow?.addView(tagView)
        }
    }

    private fun updateFavoriteIcon() {
        btnFavorite.setImageResource(
            if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
    }

    private fun animateFavorite() {
        try {
            btnFavorite.animate().cancel()
            btnFavorite.scaleX = 1f
            btnFavorite.scaleY = 1f
            btnFavorite.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(120)
                .withEndAction {
                    btnFavorite.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                .start()
        } catch (_: Exception) { }
    }

    private fun toggleFavorite() {
        val token = sharedPreferences.getString("auth_token", null)
        val isGuest = com.example.petlink.util.UserSession.isGuestMode(this)
        if (isGuest || token.isNullOrEmpty()) {
            redirectGuestToLoginWithDelay()
            return
        }

        val previous = isFavorite
        isFavorite = !isFavorite
        updateFavoriteIcon()
        animateFavorite()

        val authHeader = "Token $token"

        if (isFavorite) {
            RetrofitClient.apiService.addFavorite(authHeader, animalId)
                .enqueue(object : Callback<AnimalSimpleResponse> {
                    override fun onResponse(
                        call: Call<AnimalSimpleResponse>,
                        response: Response<AnimalSimpleResponse>
                    ) {
                        if (!response.isSuccessful) {
                            isFavorite = previous
                            updateFavoriteIcon()
                            Toast.makeText(this@AnimalDetailActivity, "Ошибка добавления в избранное", Toast.LENGTH_SHORT).show()
                            return
                        }
                        val body = response.body()
                        if (body != null) {
                            isFavorite = body.is_favorite == true
                            updateFavoriteIcon()
                        }
                        Toast.makeText(this@AnimalDetailActivity, "Объявление добавлено в избранное", Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) {
                        isFavorite = previous
                        updateFavoriteIcon()
                        Toast.makeText(this@AnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            RetrofitClient.apiService.removeFavorite(authHeader, animalId)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            showUndoFavoriteDialog()
                        } else {
                            isFavorite = previous
                            updateFavoriteIcon()
                            Toast.makeText(this@AnimalDetailActivity, "Ошибка удаления из избранного", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        isFavorite = previous
                        updateFavoriteIcon()
                        Toast.makeText(this@AnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun showUndoFavoriteDialog() {
        AlertDialog.Builder(this)
            .setMessage("Объявление удалено из избранного")
            .setPositiveButton("Отменить") { _, _ ->
                val token = sharedPreferences.getString("auth_token", null) ?: return@setPositiveButton
                val authHeader = "Token $token"
                val before = isFavorite
                isFavorite = true
                updateFavoriteIcon()
                animateFavorite()
                RetrofitClient.apiService.addFavorite(authHeader, animalId)
                    .enqueue(object : Callback<AnimalSimpleResponse> {
                        override fun onResponse(
                            call: Call<AnimalSimpleResponse>,
                            response: Response<AnimalSimpleResponse>
                        ) {
                            if (!response.isSuccessful) {
                                isFavorite = before
                                updateFavoriteIcon()
                                Toast.makeText(this@AnimalDetailActivity, "Ошибка возврата в избранное", Toast.LENGTH_SHORT).show()
                                return
                            }
                            val body = response.body()
                            if (body != null) {
                                isFavorite = body.is_favorite == true
                                updateFavoriteIcon()
                            }
                        }

                        override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) {
                            isFavorite = before
                            updateFavoriteIcon()
                            Toast.makeText(this@AnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Ок", null)
            .show()
    }

    private fun handleApplyButtonClick() {
        val token = sharedPreferences.getString("auth_token", null)
        val isGuest = com.example.petlink.util.UserSession.isGuestMode(this)
        if (isGuest || token.isNullOrEmpty()) {
            redirectGuestToLoginWithDelay()
            return
        }

        RetrofitClient.apiService.getMyRecommendations("Token $token").enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    // Предварительная проверка: есть ли уже активная заявка от текущего пользователя на это животное
                    RetrofitClient.apiService.getAnimalApplications(
                        "Token $token", role = "buyer", status = "submitted", animal = animalId
                    ).enqueue(object: Callback<List<AnimalApplication>> {
                        override fun onResponse(callCheck: Call<List<AnimalApplication>>, respCheck: Response<List<AnimalApplication>>) {
                            if (respCheck.isSuccessful) {
                                val exists = (respCheck.body() ?: emptyList()).isNotEmpty()
                                if (exists) {
                                    Toast.makeText(this@AnimalDetailActivity, "У вас уже есть активная заявка на это животное", Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }

                            val dialogView = layoutInflater.inflate(R.layout.dialog_application_confirm, null)
                            val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
                            val tvAnimalNameConfirm = dialogView.findViewById<TextView>(R.id.tvAnimalNameConfirm)
                            val tvSellerInfoConfirm = dialogView.findViewById<TextView>(R.id.tvSellerInfoConfirm)
                            val tvBuyerNameConfirm = dialogView.findViewById<TextView>(R.id.tvBuyerNameConfirm)
                            val tvBuyerPhoneConfirm = dialogView.findViewById<TextView>(R.id.tvBuyerPhoneConfirm)
                            val tvBuyerAddressConfirm = dialogView.findViewById<TextView>(R.id.tvBuyerAddressConfirm)
                            val etMessage = dialogView.findViewById<EditText>(R.id.etMessageConfirm)
                            etMessage.filters = arrayOf(InputFilter.LengthFilter(500))

                            tvTitle.text = "Подтвердите заявку на ${tvAnimalName.text}"
                            tvAnimalNameConfirm.text = "Животное: ${tvAnimalName.text}"
                            val seller = sellerUser
                            tvSellerInfoConfirm.text = if (seller?.is_shelter == true) {
                                "Продавец: Приют — ${seller.shelter_name ?: seller.full_name ?: "Не указано"}"
                            } else {
                                "Продавец: ${seller?.full_name ?: "Не указано"}"
                            }
                            currentUser?.let { me ->
                                tvBuyerNameConfirm.text = "Покупатель: ${me.full_name}"
                                tvBuyerPhoneConfirm.text = "Телефон: ${me.phone ?: "Не указан"}"
                                tvBuyerAddressConfirm.text = "Адрес: ${me.address ?: "Не указан"}"
                            }

                            AlertDialog.Builder(this@AnimalDetailActivity)
                                .setView(dialogView)
                                .setPositiveButton("Подать заявку") { _, _ ->
                                    val msg = etMessage.text?.toString()?.take(500)
                                    RetrofitClient.apiService.createAnimalApplication(
                                        "Token $token",
                                        AnimalApplicationCreate(animalId, msg)
                                    ).enqueue(object : Callback<AnimalApplication> {
                                        override fun onResponse(call2: Call<AnimalApplication>, resp: Response<AnimalApplication>) {
                                            if (resp.isSuccessful) {
                                                Toast.makeText(this@AnimalDetailActivity, "Заявка отправлена!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val err = try {
                                                    val s = resp.errorBody()?.string()
                                                    if (!s.isNullOrEmpty()) {
                                                        val j = JSONObject(s)
                                                        when {
                                                            j.has("non_field_errors") -> j.getJSONArray("non_field_errors").optString(0)
                                                            j.has("detail") -> j.optString("detail")
                                                            j.has("status") -> j.optString("status")
                                                            else -> null
                                                        }
                                                    } else null
                                                } catch (e: Exception) { null }
                                                val msg = err ?: "Ошибка отправки заявки. Возможно вы уже подали заявку."
                                                Toast.makeText(this@AnimalDetailActivity, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onFailure(call2: Call<AnimalApplication>, t: Throwable) {
                                            Toast.makeText(this@AnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }
                                .setNegativeButton("Отмена", null)
                                .show()
                        }

                        override fun onFailure(callCheck: Call<List<AnimalApplication>>, t: Throwable) {
                            val dialogView = layoutInflater.inflate(R.layout.dialog_application_confirm, null)
                            val etMessage = dialogView.findViewById<EditText>(R.id.etMessageConfirm)
                            etMessage.filters = arrayOf(InputFilter.LengthFilter(500))
                            AlertDialog.Builder(this@AnimalDetailActivity)
                                .setView(dialogView)
                                .setPositiveButton("Подать заявку") { _, _ ->
                                    val msg = etMessage.text?.toString()?.take(500)
                                    RetrofitClient.apiService.createAnimalApplication(
                                        "Token $token",
                                        AnimalApplicationCreate(animalId.toInt(), msg)
                                    ).enqueue(object : Callback<AnimalApplication> {
                                        override fun onResponse(call2: Call<AnimalApplication>, resp: Response<AnimalApplication>) {
                                            if (resp.isSuccessful) {
                                                Toast.makeText(this@AnimalDetailActivity, "Заявка отправлена!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(this@AnimalDetailActivity, "Ошибка отправки заявки", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        override fun onFailure(call2: Call<AnimalApplication>, t: Throwable) {
                                            Toast.makeText(this@AnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }
                                .setNegativeButton("Отмена", null)
                                .show()
                        }
                    })
                } else if (response.code() == 404) {
                    AlertDialog.Builder(this@AnimalDetailActivity)
                        .setTitle("Пройдите тест совместимости")
                        .setMessage("Для подачи заявки необходимо пройти тест совместимости. Перейти к тесту?")
                        .setPositiveButton("Пройти тест") { _, _ ->
                            val intent = Intent(this@AnimalDetailActivity, TestActivity::class.java)
                            startActivity(intent)
                        }
                        .setNegativeButton("Отмена", null)
                        .show()
                } else {
                    Toast.makeText(this@AnimalDetailActivity, "Ошибка проверки теста", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@AnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun redirectGuestToLoginWithDelay() {
        Toast.makeText(
            this,
            "Войдите в аккаунт, чтобы использовать эту функцию",
            Toast.LENGTH_SHORT
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2000)
    }
}