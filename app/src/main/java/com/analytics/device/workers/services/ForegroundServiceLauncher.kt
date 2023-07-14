package com.analytics.device.workers.services

import android.app.ActivityManager
import android.content.Context
import android.content.Intent


class ForegroundServiceLauncher {
    @Volatile
    private var foregroundServiceLauncher: ForegroundServiceLauncher? = null
    inner class Foreground {
        fun getInstance(): ForegroundServiceLauncher? {
            if (foregroundServiceLauncher == null) {
                synchronized(ForegroundServiceLauncher::class.java) {
                    if (foregroundServiceLauncher == null) {
                        foregroundServiceLauncher = ForegroundServiceLauncher()
                    }
                }
            }
            return foregroundServiceLauncher
        }
    }

    init {
        //Prevent form the reflection api.
        if (foregroundServiceLauncher != null) {
            throw RuntimeException("Use getInstance() method to get the single instance of this class.")
        }
    }

    @Synchronized
    fun startService(context: Context) {
        if (!isServiceRunning(context)) {
                context.startForegroundService(Intent(context, AppUsageService::class.java))
        }
    }

    @Synchronized
    fun stopService (context: Context) {
        context.stopService(Intent(context, AppUsageService::class.java))
    }

    private fun isServiceRunning(ctx: Context): Boolean {
        val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (AppUsageService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}