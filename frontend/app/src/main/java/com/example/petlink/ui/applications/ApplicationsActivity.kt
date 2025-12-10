package com.example.petlink.ui.applications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.petlink.R
import com.example.petlink.adapter.ApplicationAdapter
import com.example.petlink.data.model.AnimalApplication
import com.example.petlink.ui.auth.LoginActivity
import com.example.petlink.util.RetrofitClient
import com.example.petlink.util.BottomNavHelper
import com.example.petlink.util.UserSession
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApplicationsActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var btnAll: Button
    private lateinit var btnSubmitted: Button
    private lateinit var btnApproved: Button
    private lateinit var btnRejected: Button
    private lateinit var guestHint: TextView
    private lateinit var guestLoginButton: Button

    private var role: String = "seller" // seller | buyer
    private var currentStatus: String? = null
    private lateinit var adapter: ApplicationAdapter

    companion object {
        @Volatile
        var isForeground: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_applications)
        BottomNavHelper.wire(this)

        role = intent.getStringExtra("role") ?: "seller"
        val title = findViewById<TextView>(R.id.tvTitle)
        title?.text = if (role == "buyer") "Мои заявки" else "Входящие заявки"
        findViewById<Button>(R.id.btnBackScreen)?.setOnClickListener { finish() }

        recycler = findViewById(R.id.recycler)
        progress = findViewById(R.id.progress)
        emptyView = findViewById(R.id.emptyView)
        guestHint = findViewById(R.id.guest_hint_applications)
        guestLoginButton = findViewById(R.id.btn_guest_login_applications)
        btnAll = findViewById(R.id.btnAll)
        btnSubmitted = findViewById(R.id.btnSubmitted)
        btnApproved = findViewById(R.id.btnApproved)
        btnRejected = findViewById(R.id.btnRejected)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ApplicationAdapter(role) { app ->
            val intent = android.content.Intent(this, ApplicationDetailActivity::class.java)
            intent.putExtra("application_id", app.id)
            intent.putExtra("role", role)
            startActivity(intent)
        }
        recycler.adapter = adapter

        btnAll.setOnClickListener { currentStatus = null; loadData() }
        btnSubmitted.setOnClickListener { currentStatus = "submitted"; loadData() }
        btnApproved.setOnClickListener { currentStatus = "approved"; loadData() }
        btnRejected.setOnClickListener { currentStatus = "rejected"; loadData() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        loadData()
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
    }

    private fun loadData() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)
        val isGuest = UserSession.isGuestMode(this)
        if (token.isNullOrEmpty()) {
            progress.visibility = View.GONE
            recycler.visibility = View.GONE
            emptyView.visibility = View.VISIBLE

            if (isGuest) {
                emptyView.text = "Авторизуйтесь для доступа к заявкам"
                findViewById<Button>(R.id.btnBackScreen)?.visibility = View.GONE
                btnAll.visibility = View.GONE
                btnSubmitted.visibility = View.GONE
                btnApproved.visibility = View.GONE
                btnRejected.visibility = View.GONE
                guestHint.visibility = View.VISIBLE
                guestLoginButton.visibility = View.VISIBLE
                guestLoginButton.setOnClickListener {
                    UserSession.setGuestMode(this, false)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
//                Toast.makeText(this, "Войдите в аккаунт, чтобы просматривать заявки", Toast.LENGTH_SHORT).show()
            } else {
                emptyView.text = "Войдите, чтобы просматривать заявки"
                guestHint.visibility = View.GONE
                guestLoginButton.visibility = View.GONE
                Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            }
            return
        }
        progress.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recycler.visibility = View.GONE

        RetrofitClient.apiService.getAnimalApplications(
            "Token $token",
            role,
            currentStatus
        ).enqueue(object : Callback<List<AnimalApplication>> {
            override fun onResponse(
                call: Call<List<AnimalApplication>>,
                response: Response<List<AnimalApplication>>
            ) {
                progress.visibility = View.GONE
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val sorted = list.sortedWith(
                        compareBy<AnimalApplication> { if (it.status == "submitted") 0 else 1 }
                            .thenByDescending { it.created_at ?: "" }
                    )
                    adapter.submitList(sorted)
                    updateFilterUi()
                    recycler.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
                    emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    emptyView.visibility = View.VISIBLE
                    emptyView.text = "Ошибка загрузки: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<AnimalApplication>>, t: Throwable) {
                progress.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = "Сеть недоступна: ${t.message}"
            }
        })
    }

    private fun updateFilterUi() {
        fun style(btn: Button, selected: Boolean) {
            if (selected) {
                btn.setBackgroundColor(0xFF4CAF50.toInt()) // green
                btn.setTextColor(0xFFFFFFFF.toInt())
            } else {
                btn.setBackgroundColor(0xFFE0E0E0.toInt()) // light gray
                btn.setTextColor(0xFF000000.toInt())
            }
        }
        style(btnAll, currentStatus == null)
        style(btnSubmitted, currentStatus == "submitted")
        style(btnApproved, currentStatus == "approved")
        style(btnRejected, currentStatus == "rejected")
    }
}
