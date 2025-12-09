package com.example.petlink.ui.applications

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.R
import com.example.petlink.data.model.AnimalSimpleResponse
import com.example.petlink.data.model.AnimalApplication
import com.example.petlink.data.model.UserResponse
import com.example.petlink.util.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApplicationDetailActivity : AppCompatActivity() {
    private var applicationId: Int = -1
    private lateinit var progress: ProgressBar
    private lateinit var tvAnimal: TextView
    private lateinit var tvBuyer: TextView
    private lateinit var tvBuyerPhone: TextView
    private lateinit var tvBuyerAddress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvRisk: TextView
    private lateinit var tvMessage: TextView
    private lateinit var btnApprove: Button
    private lateinit var btnReject: Button
    private lateinit var tvCounterpartyLabel: TextView

    private var app: AnimalApplication? = null
    private var role: String = "seller"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_application_detail)

        applicationId = intent.getIntExtra("application_id", -1)
        if (applicationId == -1) { finish(); return }
        role = intent.getStringExtra("role") ?: "seller"

        progress = findViewById(R.id.progress)
        tvAnimal = findViewById(R.id.tvAnimal)
        tvBuyer = findViewById(R.id.tvBuyer)
        tvBuyerPhone = findViewById(R.id.tvBuyerPhone)
        tvBuyerAddress = findViewById(R.id.tvBuyerAddress)
        tvStatus = findViewById(R.id.tvStatus)
        tvRisk = findViewById(R.id.tvRisk)
        tvMessage = findViewById(R.id.tvMessage)
        btnApprove = findViewById(R.id.btnApprove)
        btnReject = findViewById(R.id.btnReject)
        tvCounterpartyLabel = findViewById(R.id.tvCounterpartyLabel)

        findViewById<Button>(R.id.btnBackScreen)?.setOnClickListener { finish() }

        btnApprove.setOnClickListener { updateStatus("approved") }
        btnReject.setOnClickListener { updateStatus("rejected") }

        load()
    }

    private fun load() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null) ?: run { finish(); return }
        progress.visibility = View.VISIBLE

        RetrofitClient.apiService.getAnimalApplication("Token $token", applicationId)
            .enqueue(object : Callback<AnimalApplication> {
                override fun onResponse(
                    call: Call<AnimalApplication>, response: Response<AnimalApplication>
                ) {
                    progress.visibility = View.GONE
                    if (!response.isSuccessful) {
                        Toast.makeText(this@ApplicationDetailActivity, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
                        finish(); return
                    }
                    app = response.body()
                    app?.let { render(it, token) }
                }

                override fun onFailure(call: Call<AnimalApplication>, t: Throwable) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@ApplicationDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun render(a: AnimalApplication, token: String) {
        tvAnimal.text = a.animal_name ?: "Животное: ${a.animal}"
        tvStatus.text = when (a.status) {
            "submitted" -> "Статус: Новая"
            "approved" -> "Статус: Одобрена"
            "rejected" -> "Статус: Отклонена"
            else -> "Статус: ${a.status}"
        }
        // Показываем риски только продавцу
        if (role == "seller" && !a.risk_info.isNullOrEmpty()) {
            tvRisk.visibility = View.VISIBLE
            tvRisk.text = "Потенциальные риски:\n" + a.risk_info
        } else {
            tvRisk.visibility = View.GONE
        }
        tvMessage.text = a.message ?: ""

        if (role == "seller") {
            tvCounterpartyLabel.text = "Покупатель"
            // Контакты покупателя
            RetrofitClient.apiService.getUser(a.user.toLong()).enqueue(object: Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful) {
                        val u = response.body()
                        tvBuyer.text = (u?.shelter_name ?: u?.full_name) ?: "Покупатель"
                        tvBuyerPhone.text = "Телефон: ${u?.phone ?: "Не указан"}"
                        tvBuyerAddress.text = "Адрес: ${u?.address ?: "Не указан"}"
                    }
                }
                override fun onFailure(call: Call<UserResponse>, t: Throwable) { }
            })
        } else {
            tvCounterpartyLabel.text = "Продавец"
            // Контакты продавца: сначала деталь животного user id user
            RetrofitClient.apiService.getAnimalDetail("Token $token", a.animal).enqueue(object: Callback<AnimalSimpleResponse> {
                override fun onResponse(call: Call<AnimalSimpleResponse>, response: Response<AnimalSimpleResponse>) {
                    if (response.isSuccessful) {
                        val animal = response.body()
                        val sellerId = animal?.user
                        if (sellerId != null) {
                            RetrofitClient.apiService.getUser(sellerId.toLong()).enqueue(object: Callback<UserResponse> {
                                override fun onResponse(call2: Call<UserResponse>, resp2: Response<UserResponse>) {
                                    if (resp2.isSuccessful) {
                                        val u = resp2.body()
                                        tvBuyer.text = (u?.shelter_name ?: u?.full_name) ?: "Продавец"
                                        tvBuyerPhone.text = "Телефон: ${u?.phone ?: "Не указан"}"
                                        tvBuyerAddress.text = "Адрес: ${u?.address ?: "Не указан"}"
                                    }
                                }
                                override fun onFailure(call2: Call<UserResponse>, t: Throwable) {}
                            })
                        }
                    }
                }
                override fun onFailure(call: Call<AnimalSimpleResponse>, t: Throwable) {}
            })
        }

        // Кнопки доступны только продавцу и только пока заявка новая
        if (role == "seller") {
            val enableActions = a.status == "submitted"
            btnApprove.visibility = View.VISIBLE
            btnReject.visibility = View.VISIBLE
            btnApprove.isEnabled = enableActions
            btnReject.isEnabled = enableActions
        } else {
            btnApprove.visibility = View.GONE
            btnReject.visibility = View.GONE
        }
    }

    private fun updateStatus(newStatus: String) {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null) ?: return
        val id = app?.id ?: return
        RetrofitClient.apiService.updateAnimalApplicationStatus(
            "Token $token", id, mapOf("status" to newStatus)
        ).enqueue(object: Callback<AnimalApplication> {
            override fun onResponse(call: Call<AnimalApplication>, response: Response<AnimalApplication>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ApplicationDetailActivity, if (newStatus == "approved") "Заявка одобрена" else "Заявка отклонена", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ApplicationDetailActivity, "Не удалось обновить статус", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AnimalApplication>, t: Throwable) {
                Toast.makeText(this@ApplicationDetailActivity, "Сеть недоступна: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
