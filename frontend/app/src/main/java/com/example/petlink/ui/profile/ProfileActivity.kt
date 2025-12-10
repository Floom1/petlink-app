package com.example.petlink.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.util.Patterns
import android.os.Environment
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.example.petlink.util.BottomNavHelper
import com.bumptech.glide.Glide
import com.example.petlink.ui.applications.ApplicationsActivity
import com.example.petlink.R
import com.example.petlink.ui.test.TestActivity
import com.example.petlink.util.RetrofitClient
import com.example.petlink.data.model.UploadResponse
import com.example.petlink.data.model.UserResponse
import com.example.petlink.ui.ads.MyAdsActivity
import com.example.petlink.ui.auth.LoginActivity
import com.example.petlink.util.UserSession
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class ProfileActivity : AppCompatActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>
    private var currentUser: UserResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        BottomNavHelper.wire(this)

        findViewById<ImageView>(R.id.ivSettings)?.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                startActivity(Intent(this@ProfileActivity, NotificationSettingsActivity::class.java))
            }
        }

        val scrollView = findViewById<ScrollView>(R.id.profile_scroll)
        val guestStub = findViewById<LinearLayout>(R.id.guest_profile_stub)
        val guestLoginButton = findViewById<Button>(R.id.btn_guest_login_profile)

        val isGuest = UserSession.isGuestMode(this)
        if (isGuest) {
            scrollView?.visibility = View.GONE
            guestStub?.visibility = View.VISIBLE

            guestLoginButton?.setOnClickListener {
                UserSession.setGuestMode(this, false)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            return
        }

        findViewById<Button>(R.id.button_logout)?.setOnClickListener {
            logout()
        }

        findViewById<Button>(R.id.button_access)?.setOnClickListener {
            saveProfile()
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handlePickedImage(uri)
            }
        }

        findViewById<ImageView>(R.id.imageView)?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.button_test)?.setOnClickListener {
            startActivity(android.content.Intent(this, TestActivity::class.java))
        }

        findViewById<Button>(R.id.button_my_apps)?.setOnClickListener {
            val intent = Intent(this, ApplicationsActivity::class.java)
            intent.putExtra("role", "buyer")
            startActivity(intent)
        }

        findViewById<Button>(R.id.button_my_ads)?.setOnClickListener {
            startActivity(Intent(this, MyAdsActivity::class.java))
        }

        findViewById<Button>(R.id.button_stats)?.visibility = View.GONE

        loadUser()
    }

    private fun handlePickedImage(uri: Uri) {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        val userId = sp.getInt("id", -1)
        if (token.isNullOrEmpty() || userId == -1) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            return
        }

        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView?.let { Glide.with(this).load(uri).centerCrop().into(it) }

        val tempFile = uriToTempFile(uri) ?: run {
            Toast.makeText(this, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("image", tempFile.name, requestBody)

        RetrofitClient.apiService.uploadProfileImage("Token $token", part)
            .enqueue(object: Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    if (!response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                        return
                    }
                    val url = response.body()?.url
                    if (!url.isNullOrEmpty()) {
                        val fields = mapOf<String, Any?>("photo_url" to url)
                        RetrofitClient.apiService.updateUser("Token $token", userId, fields)
                            .enqueue(object: Callback<UserResponse> {
                                override fun onResponse(call: Call<UserResponse>, resp: Response<UserResponse>) {
                                    if (resp.isSuccessful) {
                                        Toast.makeText(this@ProfileActivity, "Фото обновлено", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@ProfileActivity, "Не удалось сохранить url", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                                    Toast.makeText(this@ProfileActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                }

                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun uriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
            FileOutputStream(tempFile).use { out ->
                inputStream.use { inp ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (inp.read(buffer).also { read = it } != -1) {
                        out.write(buffer, 0, read)
                    }
                    out.flush()
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun loadUser() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.apiService.me("Token $token").enqueue(object: Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (!response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Ошибка загрузки профиля", Toast.LENGTH_SHORT).show()
                    return
                }
                val user = response.body() ?: return
                val emailView = findViewById<EditText>(R.id.profile_email)
                val nameView = findViewById<EditText>(R.id.profile_name)
                val phoneView = findViewById<EditText>(R.id.profile_phone)
                val addressView = findViewById<EditText>(R.id.profile_address)
                val imageView = findViewById<ImageView>(R.id.imageView)

                emailView?.setText(user.email)
                nameView?.setText(user.full_name)
                phoneView?.setText(user.phone ?: "")
                addressView?.setText(user.address ?: "")

                if (imageView != null) {
                    if (!user.photo_url.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity).load(user.photo_url).centerCrop().into(imageView)
                    } else {
                        Glide.with(this@ProfileActivity).load(R.drawable.cat).centerCrop().into(imageView)
                    }
                }

                currentUser = user

                val statsButton = findViewById<Button>(R.id.button_stats)
                if (user.is_shelter) {
                    statsButton?.visibility = View.VISIBLE
                    statsButton?.setOnClickListener {
                        showStatsTypeDialog()
                    }
                } else {
                    statsButton?.visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showStatsTypeDialog() {
        val user = currentUser
        if (user == null || !user.is_shelter) {
            Toast.makeText(this, "Функция доступна только для приютов", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("Проданные животные", "Купленные животные")
        AlertDialog.Builder(this)
            .setTitle("Выберите тип отчёта")
            .setItems(options) { _, which ->
                val reportType = if (which == 0) "sold" else "bought"
                showPeriodTypeDialog(reportType)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPeriodTypeDialog(reportType: String) {
        val options = arrayOf("За год", "За месяц")
        AlertDialog.Builder(this)
            .setTitle("Выберите период")
            .setItems(options) { _, which ->
                val periodType = if (which == 0) "year" else "month"
                showYearDialog(reportType, periodType)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showYearDialog(reportType: String, periodType: String) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val years = arrayOf(currentYear.toString(), (currentYear - 1).toString())

        AlertDialog.Builder(this)
            .setTitle("Выберите год")
            .setItems(years) { _, which ->
                val selectedYear = years[which].toInt()
                if (periodType == "year") {
                    startDownloadStats(reportType, periodType, selectedYear, null)
                } else {
                    showMonthDialog(reportType, selectedYear)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showMonthDialog(reportType: String, year: Int) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val maxMonth = if (year == currentYear) currentMonth else 12

        val monthValues = (1..maxMonth).toList()
        val monthLabels = monthValues.map { m -> if (m < 10) "0$m" else m.toString() }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Выберите месяц")
            .setItems(monthLabels) { _, which ->
                val month = monthValues[which]
                startDownloadStats(reportType, "month", year, month)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun startDownloadStats(reportType: String, periodType: String, year: Int, month: Int?) {
        val user = currentUser
        if (user == null || !user.is_shelter) {
            Toast.makeText(this, "Функция доступна только для приютов", Toast.LENGTH_SHORT).show()
            return
        }

        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = findViewById<android.widget.ProgressBar>(R.id.stats_progress)
        progress?.visibility = View.VISIBLE

        val call: Call<ResponseBody> = if (reportType == "sold") {
            RetrofitClient.apiService.getShelterSoldStats("Token $token", periodType, year, month)
        } else {
            RetrofitClient.apiService.getShelterBoughtStats("Token $token", periodType, year, month)
        }

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                progress?.visibility = View.GONE

                if (!response.isSuccessful) {
                    when (response.code()) {
                        404 -> Toast.makeText(this@ProfileActivity, "Нет данных за выбранный период", Toast.LENGTH_SHORT).show()
                        400 -> Toast.makeText(this@ProfileActivity, "Ошибка параметров запроса", Toast.LENGTH_SHORT).show()
                        403 -> Toast.makeText(this@ProfileActivity, "Доступно только для приютов", Toast.LENGTH_SHORT).show()
                        else -> Toast.makeText(this@ProfileActivity, "Ошибка генерации отчёта", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                saveReportToFile(response.body(), reportType, periodType, year, month)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                progress?.visibility = View.GONE
                Toast.makeText(this@ProfileActivity, "Ошибка соединения: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveReportToFile(body: ResponseBody?, reportType: String, periodType: String, year: Int, month: Int?) {
        if (body == null) {
            Toast.makeText(this, "Пустой ответ сервера", Toast.LENGTH_SHORT).show()
            return
        }

        val user = currentUser
        val shelterTitle = user?.shelter_name ?: user?.full_name ?: "Shelter"
        val safeTitle = shelterTitle.replace(" ", "_")
        val prefix = if (reportType == "sold") "Проданные" else "Купленные"
        val fileName = if (periodType == "month" && month != null) {
            String.format("%s_%02d_%d_%s.csv", prefix, month, year, safeTitle)
        } else {
            String.format("%s_%d_%s.csv", prefix, year, safeTitle)
        }

        try {
            val bytes = body.bytes()
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (dir == null) {
                Toast.makeText(this, "Не удалось получить директорию загрузок", Toast.LENGTH_SHORT).show()
                return
            }
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                out.write(bytes)
                out.flush()
            }
            Toast.makeText(this, "Отчёт сохранён в Загрузки: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        val userId = sp.getInt("id", -1)

        if (token.isNullOrEmpty() || userId == -1) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            return
        }

        val emailView = findViewById<EditText>(R.id.profile_email)
        val nameView = findViewById<EditText>(R.id.profile_name)
        val phoneView = findViewById<EditText>(R.id.profile_phone)
        val addressView = findViewById<EditText>(R.id.profile_address)

        val email = emailView.text?.toString()?.trim() ?: ""
        val name = nameView.text?.toString()?.trim() ?: ""
        val phone = phoneView.text?.toString()?.trim() ?: ""
        val address = addressView.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            nameView.error = "Введите имя"
            nameView.requestFocus()
            return
        }
        if (email.isEmpty()) {
            emailView.error = "Введите почту"
            emailView.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailView.error = "Неверный формат почты"
            emailView.requestFocus()
            return
        }
        if (phone.isNotEmpty() && !phone.all { it.isDigit() }) {
            phoneView.error = "Телефон должен содержать только цифры"
            phoneView.requestFocus()
            return
        }

        val body = mapOf(
            "email" to email,
            "full_name" to name,
            "phone" to phone,
            "address" to address
        )

        RetrofitClient.apiService.updateUser("Token $token", userId, body)
            .enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Изменения сохранены", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Toast.makeText(this@ProfileActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun logout() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        sp.edit().clear().apply()
        startActivity(android.content.Intent(this, LoginActivity::class.java))
        finish()
    }
}