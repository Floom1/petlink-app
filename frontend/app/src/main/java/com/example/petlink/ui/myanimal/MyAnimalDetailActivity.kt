package com.example.petlink.ui.myanimal

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.example.petlink.R
import com.example.petlink.adapter.AnimalPhotoAdapter
import com.example.petlink.data.model.*
import com.example.petlink.util.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyAnimalDetailActivity : AppCompatActivity() {

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
    private lateinit var tvArchivedInfo: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button

    private lateinit var sharedPreferences: SharedPreferences

    private var animalId: Int = 0
    private var statusList: List<StatusReq> = emptyList()
    private var animalDetail: AnimalSimpleResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animal_detail_my)

        initViews()

        // Всегда передаем и читаем animal_id как Int, чтобы не терять значение
        animalId = intent.getIntExtra("animal_id", 0)
        sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnEdit.setOnClickListener {
            val i = Intent(this, EditAnimalActivity::class.java)
            i.putExtra("animal_id", animalId)
            startActivity(i)
        }
        btnDelete.setOnClickListener { confirmDelete() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        if (animalId != 0) {
            loadData()
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
        tvArchivedInfo = findViewById(R.id.tvArchivedInfo)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
    }

    private fun loadData() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        RetrofitClient.apiService.getStatuses().enqueue(object: Callback<List<StatusReq>> {
            override fun onResponse(call: Call<List<StatusReq>>, response: Response<List<StatusReq>>) {
                statusList = response.body().orEmpty()
                loadAnimalDetails()
            }
            override fun onFailure(call: Call<List<StatusReq>>, t: Throwable) {
                statusList = emptyList()
                loadAnimalDetails()
            }
        })
    }

    private fun loadAnimalDetails() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val token = sharedPreferences.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            return
        }

        RetrofitClient.apiService.getAnimalDetail(null, animalId).enqueue(object: Callback<AnimalSimpleResponse> {
            override fun onResponse(
                call: Call<AnimalSimpleResponse>, response: Response<AnimalSimpleResponse>
            ) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@MyAnimalDetailActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                    progressBar.visibility = View.GONE
                    return
                }
                val animal = response.body() ?: run { progressBar.visibility = View.GONE; return }
                animalDetail = animal
                loadAdditionalData(animal)
            }
            override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@MyAnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadAdditionalData(animal: AnimalSimpleResponse) {
        var breedLoaded = false
        var photosLoaded = false

        var breed: BreedReq? = null
        var photos: List<AnimalPhotoReq> = emptyList()

        fun tryDisplay() {
            if (breedLoaded && photosLoaded) {
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                display(animal, breed, photos)
            }
        }

        if (animal.breed != null) {
            RetrofitClient.apiService.getBreed(animal.breed.toLong()).enqueue(object: Callback<BreedReq> {
                override fun onResponse(call: Call<BreedReq>, response: Response<BreedReq>) {
                    if (response.isSuccessful) breed = response.body()
                    breedLoaded = true; tryDisplay()
                }
                override fun onFailure(call: Call<BreedReq>, t: Throwable) { breedLoaded = true; tryDisplay() }
            })
        } else {
            breedLoaded = true
        }

        RetrofitClient.apiService.getAnimalPhotos(animalId.toLong()).enqueue(object: Callback<List<AnimalPhotoReq>> {
            override fun onResponse(call: Call<List<AnimalPhotoReq>>, response: Response<List<AnimalPhotoReq>>) {
                if (response.isSuccessful) photos = response.body().orEmpty()
                photosLoaded = true; tryDisplay()
            }
            override fun onFailure(call: Call<List<AnimalPhotoReq>>, t: Throwable) { photosLoaded = true; tryDisplay() }
        })
    }

    private fun display(animal: AnimalSimpleResponse, breed: BreedReq?, photos: List<AnimalPhotoReq>) {
        tvAnimalName.text = animal.name ?: "Без имени"
        tvPrice.text = if (animal.price == null || animal.price == 0.0) "Бесплатно" else "${animal.price} руб."
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
        tvGender.text = when (animal.gender) { "M" -> "Мужской"; "F" -> "Женский"; else -> "Не указан" }
        tvColor.text = animal.color ?: "Не указан"
        if (!animal.habits.isNullOrEmpty()) {
            tvHabits.text = animal.habits
            tvHabits.visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvHabitsLabel).visibility = View.VISIBLE
        } else {
            tvHabits.visibility = View.GONE
            findViewById<TextView>(R.id.tvHabitsLabel).visibility = View.GONE
        }

        setupPhotoViewPager(photos)
        generateTags(animal)

        val st = statusList.firstOrNull { it.id == animal.status }
        val isArchived = (st?.is_available == false) || (st?.name == "Уже пристроен")
        tvArchivedInfo.visibility = if (isArchived) View.VISIBLE else View.GONE
        btnEdit.visibility = if (isArchived) View.GONE else View.VISIBLE
        btnDelete.visibility = if (isArchived) View.GONE else View.VISIBLE
    }

    private fun setupPhotoViewPager(photos: List<AnimalPhotoReq>) {
        val adapter = AnimalPhotoAdapter(photos)
        viewPager.adapter = adapter
        setupIndicator(photos.size)
        viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) { updateIndicator(position) }
            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun setupIndicator(count: Int) {
        indicatorLayout.removeAllViews()
        if (count <= 1) { indicatorLayout.visibility = View.GONE; return }
        indicatorLayout.visibility = View.VISIBLE
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(8,0,8,0) }
        for (i in 0 until count) {
            val iv = ImageView(this)
            iv.setImageDrawable(getIndicatorDrawable(if (i==0) Color.BLACK else Color.GRAY))
            indicatorLayout.addView(iv, params)
        }
    }

    private fun updateIndicator(selected: Int) {
        for (i in 0 until indicatorLayout.childCount) {
            val imageView = indicatorLayout.getChildAt(i) as ImageView
            val color = if (i == selected) Color.BLACK else Color.GRAY
            imageView.setImageDrawable(getIndicatorDrawable(color))
        }
    }

    private fun getIndicatorDrawable(color: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(16, 16)
    }

    private fun generateTags(animal: AnimalSimpleResponse) {
        tagsContainer.removeAllViews()
        val tags = mutableListOf<String>()
        if (animal.is_sterilized == true) tags.add("Стерилизован")
        if (animal.has_vaccinations == true) tags.add("Есть прививки")
        if (animal.is_hypoallergenic == true) tags.add("Гипоаллергенный")
        if (animal.child_friendly == true) tags.add("Дружелюбен к детям")
        when (animal.space_requirements) {
            "low" -> tags.add("Низкие требования к пространству")
            "medium" -> tags.add("Средние требования к пространству")
            "high" -> tags.add("Высокие требования к пространству")
        }
        val tagLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0,0,16,16) }
        var currentRow: LinearLayout? = null
        tags.forEachIndexed { index, tagText ->
            if (index % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
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

    private fun formatAge(age: Double): String {
        val years = age.toInt()
        return when (years % 10) {
            1 -> if (years % 100 == 11) "$years лет" else "$years год"
            2,3,4 -> if (years % 100 in 12..14) "$years лет" else "$years года"
            else -> "$years лет"
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setMessage("Вы уверены, что хотите удалить это объявление? Все заявки на это животное будут отклонены.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ -> performDelete() }
            .show()
    }

    private fun performDelete() {
        val token = sharedPreferences.getString("auth_token", null)
        if (token.isNullOrEmpty()) { Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show(); return }
        RetrofitClient.apiService.deleteAnimal("Token $token", animalId).enqueue(object: Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MyAnimalDetailActivity, "Объявление удалено", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val msg = try {
                        val s = response.errorBody()?.string(); if (!s.isNullOrEmpty()) JSONObject(s).optString("detail") else null
                    } catch (e: Exception) { null }
                    Toast.makeText(this@MyAnimalDetailActivity, msg ?: "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MyAnimalDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
