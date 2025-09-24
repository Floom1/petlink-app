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

class RegActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reg)

        val back_to_auth: TextView = findViewById(R.id.link_to_auth)
        val reg: Button = findViewById(R.id.button_reg)

        val userLogin: EditText = findViewById(R.id.user_login)
        val userPass: EditText = findViewById(R.id.user_password)
        val userEmail: EditText = findViewById(R.id.user_email)
        val userPassConf: EditText = findViewById(R.id.user_password_confirm)

        back_to_auth.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        reg.setOnClickListener {
            val login = userLogin.text.toString().trim()
            val email = userEmail.text.toString().trim()
            val pass = userPass.text.toString().trim()
            val pass_conf = userPassConf.text.toString().trim()

            if (login == "" || pass == "" || pass_conf == "" || email == "")
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
//            else {
//                val db = DbHelper()
//            }
        }
    }
}