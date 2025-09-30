package com.example.petlink

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.util.BottomNavHelper
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.petlink.util.RetrofitClient
import com.example.petlink.data.model.UserResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        BottomNavHelper.wire(this)
        findViewById<android.widget.Button>(R.id.button_logout)?.setOnClickListener {
            logout()
        }
        loadUser()
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

    private fun logout() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        sp.edit().clear().apply()
        startActivity(android.content.Intent(this, LoginActivity::class.java))
        finish()
    }
}
