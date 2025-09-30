package com.example.petlink

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.util.BottomNavHelper
import android.widget.GridLayout


class MainActivity : AppCompatActivity()  {
    private val homeController = HomeContent()
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
