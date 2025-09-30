package com.example.petlink

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.util.BottomNavHelper

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        BottomNavHelper.wire(this)
    }
}
