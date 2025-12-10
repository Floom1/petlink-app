package com.example.petlink.ui.profile

import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.R
import com.example.petlink.util.BottomNavHelper
import com.example.petlink.util.NotificationPreferences
import com.example.petlink.util.NotificationsScheduler

class NotificationSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)
        BottomNavHelper.wire(this)

        findViewById<Button>(R.id.btnBackScreen)?.setOnClickListener { finish() }

        val switch = findViewById<Switch>(R.id.switch_notifications)
        val enabled = NotificationPreferences.isEnabled(this)
        switch?.isChecked = enabled

        switch?.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            NotificationPreferences.setEnabled(this, isChecked)
            if (isChecked) {
                NotificationsScheduler.schedule(this)
            } else {
                NotificationsScheduler.cancel(this)
            }
        }
    }
}
