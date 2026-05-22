package com.example.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll

class WidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("WidgetRefreshWorker", "Standard Widget refresh triggered via WorkManager timer")
            
            // Re-render and publish changes to widgets
            MomentGlanceWidget("SMALL").updateAll(applicationContext)
            MomentGlanceWidget("MEDIUM").updateAll(applicationContext)
            MomentGlanceWidget("LARGE").updateAll(applicationContext)
            
            Result.success()
        } catch (e: Exception) {
            Log.e("WidgetRefreshWorker", "Failed fully updating home widgets: ${e.message}", e)
            Result.retry()
        }
    }
}
