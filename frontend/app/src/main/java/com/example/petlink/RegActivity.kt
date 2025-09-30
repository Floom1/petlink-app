package com.example.petlink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.RegistrationRequest
import com.example.petlink.util.RetrofitClient


class RegActivity : AppCompatActivity() {

    private lateinit var userLogin: EditText
    private lateinit var userEmail: EditText
    private lateinit var userPassword: EditText
    private lateinit var userPasswordConfirm: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg)

//        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
//        if (sharedPreferences.getBoolean("is_logged_in", false)) {
//            startActivity(Intent(this, MainActivity::class.java))
//            finish()
//            return
//        }

        userLogin = findViewById(R.id.user_login)
        userEmail = findViewById(R.id.user_email)
        userPassword = findViewById(R.id.user_password)
        userPasswordConfirm = findViewById(R.id.user_password_confirm)
        val toAuth: TextView = findViewById(R.id.link_to_auth)
        val regButton: Button = findViewById(R.id.button_reg)

        toAuth.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        regButton.setOnClickListener {
            register()
        }
    }

    private fun register() {
        val login = userLogin.text.toString().trim()
        val email = userEmail.text.toString().trim()
        val pass = userPassword.text.toString().trim()
        val passConf = userPasswordConfirm.text.toString().trim()

        if (login.isEmpty() || email.isEmpty() || pass.isEmpty() || passConf.isEmpty()) {
            Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
            return
        }

        if (pass != passConf) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_LONG).show()
            return
        }

        val registrationRequest = RegistrationRequest(
            email = email,
            full_name = login,
            password = pass,
            is_shelter = false
        )

        RetrofitClient.apiService.register(registrationRequest).enqueue(object : retrofit2.Callback<AuthResponse> {
            override fun onResponse(call: retrofit2.Call<AuthResponse>, response: retrofit2.Response<AuthResponse>) {
                if (response.isSuccessful) {
                    val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("auth_token", response.body()?.token)
                    editor.putBoolean("is_logged_in", true)
                    editor.apply()

                    startActivity(Intent(this@RegActivity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@RegActivity, "Ошибка регистрации", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@RegActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}