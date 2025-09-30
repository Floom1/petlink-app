package com.example.petlink

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.ui.home.HomeContentController
import com.example.petlink.ui.BottomNavHelper
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.petlink.data.model.AnimalPhotoReq
import com.example.petlink.data.model.AnimalReq
import com.example.petlink.data.model.StatusReq
import com.example.petlink.util.RetrofitClient
import okhttp3.internal.notifyAll
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response



class HomeActivity : AppCompatActivity()  {
    private val homeController = HomeContentController()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_1)
        BottomNavHelper.wire(this)
        val grid = findViewById<GridLayout>(R.id.grid_placeholder)
        if (grid != null) {
            homeController.loadAndRender(grid, this)
        }
    }

}
