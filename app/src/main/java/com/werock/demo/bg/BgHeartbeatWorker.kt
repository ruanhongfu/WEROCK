package com.werock.demo.bg

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class BgHeartbeatWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        BgStatusStore.setLastUpdated(applicationContext, System.currentTimeMillis())
        enqueueNext(applicationContext, delaySeconds = 5)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "werock_bg_heartbeat"

        fun start(context: Context) {
            enqueueNext(context, delaySeconds = 0)
        }

        private fun enqueueNext(context: Context, delaySeconds: Long) {
            val request = OneTimeWorkRequestBuilder<BgHeartbeatWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
}

