package com.example.petlink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.util.BottomNavHelper
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.util.Patterns
import android.widget.Button
import com.bumptech.glide.Glide
import com.example.petlink.util.RetrofitClient
import com.example.petlink.data.model.UploadResponse
import com.example.petlink.api.PetLinkApi
import com.example.petlink.data.model.UserResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        BottomNavHelper.wire(this)

        findViewById<Button>(R.id.button_logout)?.setOnClickListener {
            logout()
        }

        findViewById<Button>(R.id.button_access)?.setOnClickListener {
            saveProfile()
        }

        // gallery picker for new photo
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handlePickedImage(uri)
            }
        }

        findViewById<Button>(R.id.button_photo)?.setOnClickListener {
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

        // create temp file from uri
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
                        // update user profile photo_url
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
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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