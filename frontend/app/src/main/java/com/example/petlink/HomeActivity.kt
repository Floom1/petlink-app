package com.example.petlink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.data.model.AuthResponse
import com.example.petlink.data.model.RegistrationRequest
import com.example.petlink.databinding.ActivityRegBinding
import com.example.petlink.util.RetrofitClient



class HomeActivity : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)
    }
}