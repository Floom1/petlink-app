package com.example.petlink.ui.ads

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.R
import com.example.petlink.util.BottomNavHelper

class MyAdsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ads)
        BottomNavHelper.wire(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, MyAdsFragment())
                .commit()
        }
    }
}
