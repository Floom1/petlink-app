package com.example.petlink.util

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import com.example.petlink.ui.MainActivity
import com.example.petlink.ui.profile.ProfileActivity
import com.example.petlink.R
import com.example.petlink.ui.favorites.FavoritesActivity
import android.widget.ImageView
import android.graphics.PorterDuff
import android.graphics.Color
import com.example.petlink.ui.applications.ApplicationsActivity
import com.example.petlink.data.model.AnimalApplication
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object BottomNavHelper {
    fun wire(activity: Activity) {
        val home = activity.findViewById<LinearLayout>(R.id.nav_home)
        val search = activity.findViewById<LinearLayout>(R.id.nav_search)
        val favorites = activity.findViewById<LinearLayout>(R.id.nav_favorites)
        val profile = activity.findViewById<LinearLayout>(R.id.nav_profile)
        val notifIcon = activity.findViewById<ImageView>(R.id.nav_notifications_icon)

        home?.setOnClickListener {
            if (activity !is MainActivity) {
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }
        }
        search?.setOnClickListener {
            if (activity !is FavoritesActivity) {
                activity.startActivity(Intent(activity, FavoritesActivity::class.java))
            }
        }
        favorites?.setOnClickListener {
            // Открываем список входящих заявок (seller scope)
            if (activity !is ApplicationsActivity) {
                val intent = Intent(activity, ApplicationsActivity::class.java)
                intent.putExtra("role", "seller")
                activity.startActivity(intent)
            }
        }
        profile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                activity.startActivity(Intent(activity, ProfileActivity::class.java))
            }
        }

        // Обновляем бейдж иконки уведомлений: есть ли новые заявки (submitted) на мои животные
        try {
            val sp = activity.getSharedPreferences("user_session", Activity.MODE_PRIVATE)
            val token = sp.getString("auth_token", null)
            if (!token.isNullOrEmpty() && notifIcon != null) {
                RetrofitClient.apiService.getAnimalApplications(
                    "Token $token",
                    role = "seller",
                    status = "submitted"
                ).enqueue(object: Callback<List<AnimalApplication>> {
                    override fun onResponse(
                        call: Call<List<AnimalApplication>>, response: Response<List<AnimalApplication>>
                    ) {
                        if (response.isSuccessful) {
                            val list = response.body() ?: emptyList()
                            if (list.isNotEmpty()) {
                                notifIcon.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                            } else {
                                notifIcon.clearColorFilter()
                            }
                        }
                    }
                    override fun onFailure(call: Call<List<AnimalApplication>>, t: Throwable) { /* ignore */ }
                })
            }
        } catch (_: Exception) { }
    }
}
