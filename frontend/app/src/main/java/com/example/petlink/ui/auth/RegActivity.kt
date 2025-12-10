package com.example.petlink.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.ui.MainActivity
import com.example.petlink.R
import com.example.petlink.ui.test.TestActivity
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.RegistrationRequest
import com.example.petlink.util.RetrofitClient
import com.example.petlink.util.UserSession

class RegActivity : AppCompatActivity() {

    private lateinit var userLogin: EditText
    private lateinit var userEmail: EditText
    private lateinit var userPassword: EditText
    private lateinit var userPasswordConfirm: EditText
    private lateinit var shelterCheckBox: CheckBox
    private lateinit var shelterLegalName: EditText
    private var originalLoginHint: String? = null
    private var savedUserName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg)

        userLogin = findViewById(R.id.user_login)
        userEmail = findViewById(R.id.user_email)
        userPassword = findViewById(R.id.user_password)
        userPasswordConfirm = findViewById(R.id.user_password_confirm)
        shelterCheckBox = findViewById(R.id.checkbox_is_shelter)
        shelterLegalName = findViewById(R.id.shelter_legal_name)
        val shelterLegalNameLabel: TextView = findViewById(R.id.shelter_legal_name_label)
        originalLoginHint = userLogin.hint?.toString()
        val toAuth: TextView = findViewById(R.id.link_to_auth)
        val regButton: Button = findViewById(R.id.button_reg)
        val skipButton: TextView = findViewById(R.id.button_skip_reg)

        shelterCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                savedUserName = userLogin.text.toString()
                userLogin.text.clear()
                userLogin.hint = "Полное название приюта"
                shelterLegalName.visibility = View.VISIBLE
                shelterLegalNameLabel.visibility = View.VISIBLE
            } else {
                if (!savedUserName.isNullOrEmpty()) {
                    userLogin.setText(savedUserName)
                }
                if (originalLoginHint != null) {
                    userLogin.hint = originalLoginHint
                }
                shelterLegalName.visibility = View.GONE
                shelterLegalNameLabel.visibility = View.GONE
            }
        }

        toAuth.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        regButton.setOnClickListener {
            register()
        }

        skipButton.setOnClickListener {
            UserSession.enterGuestMode(this)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun register() {
        val login = userLogin.text.toString().trim()
        val email = userEmail.text.toString().trim()
        val pass = userPassword.text.toString().trim()
        val passConf = userPasswordConfirm.text.toString().trim()
        val isShelter = shelterCheckBox.isChecked
        val shelterName = shelterLegalName.text.toString().trim()

        if (isShelter && shelterName.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите юридическое название приюта", Toast.LENGTH_LONG).show()
            return
        }

        if (login.isEmpty() || email.isEmpty() || pass.isEmpty() || passConf.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_LONG).show()
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
            is_shelter = isShelter,
            shelter_name = if (isShelter) shelterName else null
        )

        RetrofitClient.apiService.register(registrationRequest).enqueue(object : retrofit2.Callback<AuthResponse> {
            override fun onResponse(call: retrofit2.Call<AuthResponse>, response: retrofit2.Response<AuthResponse>) {
                if (response.isSuccessful) {
                    val authResponse = response.body()
                    if (authResponse?.token != null) {
                        val token = authResponse.token
                        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("auth_token", token)
                        editor.putBoolean("is_logged_in", true)
                        editor.putBoolean("is_guest_mode", false)
                        editor.apply()

                        // Получаем ID пользователя
                        RetrofitClient.apiService.me("Token $token").enqueue(object: retrofit2.Callback<com.example.petlink.data.model.UserResponse> {
                            override fun onResponse(call2: retrofit2.Call<com.example.petlink.data.model.UserResponse>, resp: retrofit2.Response<com.example.petlink.data.model.UserResponse>) {
                                if (resp.isSuccessful) {
                                    val id = resp.body()?.id ?: -1
                                    val sp = getSharedPreferences("user_session", MODE_PRIVATE)
                                    sp.edit().putInt("id", id).apply()
                                }
                                // После регистрации переходим к тесту
                                startActivity(Intent(this@RegActivity, TestActivity::class.java))
                                finish()
                            }

                            override fun onFailure(call2: retrofit2.Call<com.example.petlink.data.model.UserResponse>, t: Throwable) {
                                // Переходим к тесту даже если не удалось получить ID
                                startActivity(Intent(this@RegActivity, TestActivity::class.java))
                                finish()
                            }
                        })
                    } else {
                        // Пользователь создан, но токен не получен (возможно, это отложенная активация)
                        Toast.makeText(this@RegActivity, "Регистрация успешна. Пожалуйста, войдите в систему.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@RegActivity, LoginActivity::class.java))
                        finish()
                    }
                } else {
                    // Детализируем ошибку
                    val errorMessage = when (response.code()) {
                        400 -> "Некорректные данные регистрации"
                        409 -> "Пользователь с таким email уже существует"
                        else -> "Ошибка регистрации: ${response.code()}"
                    }
                    Toast.makeText(this@RegActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@RegActivity, "Ошибка сети: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}