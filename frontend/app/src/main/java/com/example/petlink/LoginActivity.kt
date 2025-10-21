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
import com.example.petlink.databinding.ActivityAuthBinding
import com.example.petlink.util.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var userEmail: EditText
    private lateinit var userPass: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        if (sharedPreferences.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        userEmail = findViewById(R.id.user_login_auth)
        userPass = findViewById(R.id.user_pass_auth)
        val toReg: TextView = findViewById(R.id.link_to_reg)
        val reg: Button = findViewById(R.id.button_auth)

        toReg.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        reg.setOnClickListener {
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

                if (response.isSuccessful) {
                    val token = response.body()?.token
                    val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("auth_token", token)
                    editor.putBoolean("is_logged_in", true)
                    editor.apply()

                    if (!token.isNullOrEmpty()) {
                        RetrofitClient.apiService.me("Token $token").enqueue(object: Callback<com.example.petlink.data.model.UserResponse> {
                            override fun onResponse(call2: Call<com.example.petlink.data.model.UserResponse>, resp: Response<com.example.petlink.data.model.UserResponse>) {
                                if (resp.isSuccessful) {
                                    val id = resp.body()?.id ?: -1
                                    val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                                    sp.edit().putInt("id", id).apply()
                                }
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }

                            override fun onFailure(call2: Call<com.example.petlink.data.model.UserResponse>, t: Throwable) {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        })
                    } else {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
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
}