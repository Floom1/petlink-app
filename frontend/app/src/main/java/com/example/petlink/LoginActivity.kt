package com.example.petlink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.LoginRequest
import com.example.petlink.util.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {
    private lateinit var userEmail: EditText
    private lateinit var userPass: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        if (sharedPreferences.getBoolean("is_logged_in", false)) {
            checkTestCompletion()
            return
        }

        userEmail = findViewById(R.id.user_login_auth)
        userPass = findViewById(R.id.user_pass_auth)
        val toReg: TextView = findViewById(R.id.link_to_reg)
        val loginButton: Button = findViewById(R.id.button_auth)

        toReg.setOnClickListener {
            startActivity(Intent(this, RegActivity::class.java))
        }

        loginButton.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val email = userEmail.text.toString().trim()
        val pass = userPass.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
            return
        }

        val loginRequest = LoginRequest(username = email, password = pass)

        RetrofitClient.apiService.login(loginRequest).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful && response.body()?.token != null) {
                    val token = response.body()?.token
                    val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("auth_token", token)
                    editor.putBoolean("is_logged_in", true)
                    editor.apply()

                    // Получаем ID пользователя и затем проверяем тест
                    if (!token.isNullOrEmpty()) {
                        RetrofitClient.apiService.me("Token $token").enqueue(object: Callback<com.example.petlink.data.model.UserResponse> {
                            override fun onResponse(call2: Call<com.example.petlink.data.model.UserResponse>, resp: Response<com.example.petlink.data.model.UserResponse>) {
                                if (resp.isSuccessful) {
                                    val id = resp.body()?.id ?: -1
                                    val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                                    sp.edit().putInt("id", id).apply()
                                }
                                checkTestCompletion()
                            }

                            override fun onFailure(call2: Call<com.example.petlink.data.model.UserResponse>, t: Throwable) {
                                checkTestCompletion()
                            }
                        })
                    } else {
                        checkTestCompletion()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Неверный email или пароль", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Ошибка соединения: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun checkTestCompletion() {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            RetrofitClient.apiService.getMyRecommendations("Token $token").enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        // Тест пройден, открываем главную страницу
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else if (response.code() == 404) {
                        // Тест не пройден, открываем страницу теста
                        startActivity(Intent(this@LoginActivity, TestActivity::class.java))
                        finish()
                    } else {
                        // Другая ошибка, открываем главную страницу
                        Toast.makeText(this@LoginActivity, "Ошибка проверки теста", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    // Ошибка сети, открываем главную страницу
                    Toast.makeText(this@LoginActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            })
        } else {
            Toast.makeText(this@LoginActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
        }
    }
}