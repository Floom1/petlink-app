package com.example.petlink.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.petlink.data.model.NotificationItem
import retrofit2.Call
import retrofit2.Response

class NotificationsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        try {
            if (!NotificationPreferences.isEnabled(applicationContext)) {
                return Result.success()
            }

            val sp = applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val token = sp.getString("auth_token", null)
            val isLoggedIn = sp.getBoolean("is_logged_in", false)
            val isGuest = sp.getBoolean("is_guest_mode", false)

            if (token.isNullOrEmpty() || !isLoggedIn || isGuest) {
                return Result.success()
            }

            val call: Call<List<NotificationItem>> =
                RetrofitClient.apiService.getNotifications("Token $token", true)
            val response: Response<List<NotificationItem>> = call.execute()
            if (!response.isSuccessful) {
                return Result.success()
            }

            val items = response.body() ?: emptyList()
            if (items.isEmpty()) {
                return Result.success()
            }

            if (com.example.petlink.ui.applications.ApplicationsActivity.isForeground) {
                return Result.success()
            }

            for (n in items) {
                NotificationHelper.showNewApplicationNotification(applicationContext, n)
            }

            val ids = items.map { it.id }
            RetrofitClient.apiService.markNotificationsAsRead(
                "Token $token",
                mapOf("ids" to ids)
            ).execute()

            return Result.success()
        } catch (e: Exception) {
            return Result.success()
        } finally {
            val sp = applicationContext.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            val stillLoggedIn = sp.getBoolean("is_logged_in", false)
            val isGuestNow = sp.getBoolean("is_guest_mode", false)

            if (NotificationPreferences.isEnabled(applicationContext) && stillLoggedIn && !isGuestNow) {
                NotificationsScheduler.schedule(applicationContext)
            }
        }
    }
}
