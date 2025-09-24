package com.example.petlink

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        val toReg: TextView = findViewById(R.id.link_to_reg)
        val reg: Button = findViewById(R.id.button_auth)

        val userPass: EditText = findViewById(R.id.user_pass_auth)
        val userEmail: EditText = findViewById(R.id.user_login_auth)

        toReg.setOnClickListener {
            val intent = Intent(this, RegActivity::class.java)
            startActivity(intent)
        }

        reg.setOnClickListener {
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()

            if (pass == "" || email == "")
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
//            else {
//                val db = DbHelper()
//            }
        }
    }
}