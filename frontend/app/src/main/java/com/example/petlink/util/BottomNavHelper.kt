package com.example.petlink.util

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import android.widget.Toast
import com.example.petlink.MainActivity
import com.example.petlink.ProfileActivity
import com.example.petlink.R

object BottomNavHelper {
    fun wire(activity: Activity) {
        val home = activity.findViewById<LinearLayout>(R.id.nav_home)
        val search = activity.findViewById<LinearLayout>(R.id.nav_search)
        val favorites = activity.findViewById<LinearLayout>(R.id.nav_favorites)
        val profile = activity.findViewById<LinearLayout>(R.id.nav_profile)

        home?.setOnClickListener {
            if (activity !is MainActivity) {
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }
        }
        search?.setOnClickListener {
            Toast.makeText(activity, "Поиск: скоро", Toast.LENGTH_SHORT).show()
        }
        favorites?.setOnClickListener {
            Toast.makeText(activity, "Избранное: скоро", Toast.LENGTH_SHORT).show()
        }
        profile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                activity.startActivity(Intent(activity, ProfileActivity::class.java))
            }
        }
    }
}
