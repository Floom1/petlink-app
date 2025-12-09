package com.example.petlink

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.util.BottomNavHelper
import com.example.petlink.util.RetrofitClient
import com.example.petlink.util.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity()  {
    private val homeController = HomeContent()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false) && !token.isNullOrEmpty()
        val isGuest = UserSession.isGuestMode(this)

        if (!isLoggedIn && !isGuest) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.home_1)
        BottomNavHelper.wire(this)

        if (isGuest) {
            findViewById<Button>(R.id.btn_filters)?.visibility = View.GONE
            findViewById<Button>(R.id.btn_test_recommendations)?.visibility = View.GONE
            findViewById<android.widget.ImageView>(R.id.filter_png)?.visibility = View.GONE
        }

        val grid = findViewById<GridLayout>(R.id.grid_placeholder)
        if (grid != null) {
            homeController.loadAndRender(grid, this)
        }
    }

    override fun onResume() {
        super.onResume()
        // Проверяем при каждом возвращении на активность
        checkTestCompletion()
    }

    private fun checkTestCompletion() {
        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token != null) {
            RetrofitClient.apiService.getMyRecommendations("Token $token").enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        // Тест пройден, ничего не делаем
                    } else if (response.code() == 404) {
                        // Тест не пройден, открываем страницу теста
                        startActivity(Intent(this@MainActivity, TestActivity::class.java))
                        finish()
                    }
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    // Игнорируем ошибки сети
                }
            })
        }
    }
}