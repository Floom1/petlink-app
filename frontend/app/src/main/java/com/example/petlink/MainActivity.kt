package com.example.petlink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.LoginRequest
import com.example.petlink.databinding.ActivityAuthBinding
import com.example.petlink.util.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var userEmail: EditText
    private lateinit var userPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

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
                    // Сохраняем токен в SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("auth_token", response.body()?.token)
                    editor.putBoolean("is_logged_in", true)
                    editor.apply()

                    // Переходим к главному экрану
                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Обработка ошибки авторизации
                    Toast.makeText(this@MainActivity, "Неверный email или пароль", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Ошибка соединения: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}