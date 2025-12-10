package com.example.petlink.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationsScheduler {
    private const val WORK_NAME = "notifications_worker"
    private const val INTERVAL_MINUTES = 1L

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<NotificationsWorker>()
            .setInitialDelay(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
