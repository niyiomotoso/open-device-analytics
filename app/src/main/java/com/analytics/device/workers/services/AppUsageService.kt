package com.analytics.device.workers.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.analytics.device.R
import com.analytics.device.constants.StatsTypeMap
import com.analytics.device.helpers.DateTimeHelper
import com.analytics.device.models.AppDetails
import com.analytics.device.utils.GeneralUtils
import com.analytics.device.utils.UsageUtils
import kotlinx.coroutines.*
import java.util.Collections
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors


class AppUsageService: Service() {
    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    /**
     * load the usage stats for last 1 hour
     */
    private fun loadLastUsedStats() {
        val usm = baseContext.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        var appList = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            System.currentTimeMillis() - 1000 * 3600 * 1,
            System.currentTimeMillis()
        )
        appList =
            appList.stream().filter { app: UsageStats -> app.lastTimeUsed > 0 }.collect(
                Collectors.toList()
            )

        // Group the usageStats by application and sort them by last time used
        if (appList.size > 0) {
            val mySortedMap: MutableMap<String, UsageStats> = TreeMap()
            for (usageStats in appList) {
                mySortedMap[usageStats.packageName] = usageStats
            }
            handleAppsLastUsage(mySortedMap)
        }
    }


    private fun handleAppsLastUsage(mySortedMap: Map<String, UsageStats>) {
        val appsList: ArrayList<AppDetails> = ArrayList()
        val usageStatsList: List<UsageStats> = ArrayList(mySortedMap.values)

        // sort the applications by time spent in foreground
        Collections.sort(
            usageStatsList
        ) { z1: UsageStats, z2: UsageStats ->
            z1.lastTimeUsed.compareTo(z2.lastTimeUsed)
        }

        // get total time of apps usage to calculate the usagePercentage for each app
        val totalTime = usageStatsList.stream().map { obj: UsageStats -> obj.lastTimeUsed }
            .mapToLong { obj: Long -> obj }.sum()

        //fill the appsList
        for (usageStats in usageStatsList) {
            try {
                val lastUsedAppTime: Long = 0
                if (usageStats.lastTimeUsed > lastUsedAppTime) {
                    val packageName = usageStats.packageName
                    var icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_launcher_background)
                    val packageNames = packageName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    var appName = packageNames[packageNames.size - 1].trim { it <= ' ' }
                    if (GeneralUtils().Utils().isAppInformationAvailable(applicationContext, usageStats.packageName)) {
                        val ai: ApplicationInfo = applicationContext.packageManager
                            .getApplicationInfo(packageName, 0)
                        icon = applicationContext.packageManager.getApplicationIcon(ai)
                        appName = applicationContext.packageManager.getApplicationLabel(ai)
                            .toString()
                    }

                    if (appName.trim() != "") {
                        val lastTimeUsed = DateTimeHelper.getDateTimeBreakdown(usageStats.lastTimeUsed)
                        val usagePercentage = (usageStats.lastTimeUsed * 100 / totalTime).toInt()

                        val usageStatDTO = AppDetails(packageName.hashCode().toLong(), icon, appName, packageName, usagePercentage, lastTimeUsed, "", StatsTypeMap().LAST_TIME_OPENED, 0)
                        // Add the new note at the top of the list
                        appsList.add(usageStatDTO)
                        // work operation
                        runBlocking {
                            UsageUtils(applicationContext).Usage().syncAppUsageInDB(usageStatDTO)
                        }
                    }


                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private val delayTime: Long = TimeUnit.SECONDS.toMillis(5)
    var usageCountHandler: Handler = Handler()
    var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                //Sync data to and fro every 5 seconds
                println("USAGE SERVICE RUNNING")
                loadLastUsedStats()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                usageCountHandler.postDelayed(this, delayTime)
            }
        }
    }

    private val cleanupDelayTime: Long = TimeUnit.HOURS.toMillis(24)
    var usageCountCleanupHandler: Handler = Handler()
    var cleanupTimerRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                //Sync data to and fro every day
                cleanupLastUsedStats()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                usageCountCleanupHandler.postDelayed(this, cleanupDelayTime)
            }
        }
    }

    private fun cleanupLastUsedStats() {
        runBlocking {
            UsageUtils(applicationContext).Usage().cleanUpAppUsageInDB()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(62318, builtNotification())
        usageCountHandler.postDelayed(timerRunnable, 0)
        usageCountCleanupHandler.postDelayed(cleanupTimerRunnable, 0)
    }

    fun builtNotification(): Notification {
        val notificationManager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)

        var builder: NotificationCompat.Builder? = null

        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel = NotificationChannel("ID", "Name", importance)

            notificationManager.createNotificationChannel(notificationChannel)
            NotificationCompat.Builder(this, notificationChannel.id)
        } else {
            NotificationCompat.Builder(this)
        }
        builder.setDefaults(Notification.FLAG_ONLY_ALERT_ONCE)

        val message = "Analysing your app usages"
        builder.setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
            .setColor(Color.parseColor("#0f9595"))
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

        val contentIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(contentIntent)

        val notification = builder.build()
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
        return notification
    }

    override fun onDestroy() {
        super.onDestroy()
        ForegroundServiceLauncher().Foreground().getInstance()?.startService(this)
    }
}