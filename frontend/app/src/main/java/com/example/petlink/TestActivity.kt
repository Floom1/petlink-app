package com.example.petlink

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.petlink.data.model.TestResult
import com.example.petlink.util.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        findViewById<Button>(R.id.button_submit_test)?.setOnClickListener {
            submitTest()
        }
    }

    private fun submitTest() {
        val sp = getSharedPreferences("user_session", MODE_PRIVATE)
        val token = sp.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Требуется вход", Toast.LENGTH_SHORT).show()
            return
        }

        val residenceType = getSelectedValue(R.id.rg_residence)
        val weekdayTime = getSelectedValue(R.id.rg_weekday_time)
        val hasChildren = findViewById<RadioButton>(R.id.rb_kids_yes)?.isChecked ?: false
        val plannedMove = findViewById<RadioButton>(R.id.rb_move_yes)?.isChecked ?: false
        val petExperience = getSelectedValue(R.id.rg_experience)
        val hasAllergies = findViewById<RadioButton>(R.id.rb_allergy_yes)?.isChecked ?: false

        if (residenceType == null || weekdayTime == null || petExperience == null) {
            Toast.makeText(this, "Пожалуйста, ответьте на все вопросы", Toast.LENGTH_SHORT).show()
            return
        }

        val testResult = TestResult(
            residence_type = residenceType,
            weekday_time = weekdayTime,
            has_children = hasChildren,
            planned_move = plannedMove,
            pet_experience = petExperience,
            has_allergies = hasAllergies
        )

        RetrofitClient.apiService.submitTest("Token $token", testResult)
            .enqueue(object : Callback<TestResult> {
                override fun onResponse(call: Call<TestResult>, response: Response<TestResult>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@TestActivity, "Тест успешно сохранён!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@TestActivity, "Ошибка сохранения теста", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<TestResult>, t: Throwable) {
                    Toast.makeText(this@TestActivity, "Ошибка соединения: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getSelectedValue(radioGroupId: Int): String? {
        val radioGroup = findViewById<RadioGroup>(radioGroupId)
        val selectedId = radioGroup.checkedRadioButtonId

        return when (selectedId) {
            R.id.rb_res_apartment -> "apartment"
            R.id.rb_res_private -> "private"
            R.id.rb_time_lt4 -> "lt4"
            R.id.rb_time_4_8 -> "4_8"
            R.id.rb_time_gt8 -> "gt8"
            R.id.rb_exp_none -> "none"
            R.id.rb_exp_had_before -> "had_before"
            R.id.rb_exp_now -> "now"
            else -> null
        }
    }
}
