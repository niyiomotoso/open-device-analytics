package com.analytics.device.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.analytics.device.helpers.DateTimeHelper
import com.analytics.device.migrations.AppUsageDatabase
import com.analytics.device.models.AppDetails
import com.analytics.device.models.AppStats
import com.analytics.device.models.AppUsage
import com.analytics.device.models.CustomActivityMap
import java.time.ZonedDateTime

class UsageUtils (var context: Context) {
     val appUsageDatabase by lazy { AppUsageDatabase.getDatabase(context).appUsageDao() }
    inner class Usage {
    suspend fun syncAppUsageInDB(appDetails: AppDetails) {
        val currentSystemTimeNow = DateTimeHelper.getSystemCurrentTimeNow()
        val newUsageInstance = AppUsage(
            appName = appDetails.appName,
            packageName = appDetails.packageName,
            frequency = 0,
            lastOpenTime = appDetails.lastTimeUsed,
            createdAt = currentSystemTimeNow
        )

        val savedAppUsage = appUsageDatabase.getMostRecentAppUsage(appDetails.packageName)
        if (savedAppUsage == null) {
            appUsageDatabase.addAppUsage(newUsageInstance)
        } else {
            // has an existing entry
            val savedLastTimeOpen = savedAppUsage.lastOpenTime
            val currentLastTimeOpen = appDetails.lastTimeUsed
            val usageCreatedAt = savedAppUsage.createdAt
            // check if within the last hour
            if (DateTimeHelper.isWithInLastMinutes(usageCreatedAt, currentSystemTimeNow, 60)) {
                // time changed
                if (savedLastTimeOpen != currentLastTimeOpen && !DateTimeHelper.isWithInLastMinutes(savedLastTimeOpen, currentSystemTimeNow, 1)) {
                    savedAppUsage.frequency = savedAppUsage.frequency + 1
                    savedAppUsage.lastOpenTime = currentLastTimeOpen
                    appUsageDatabase.updateAppUsage(savedAppUsage)
                }
            } else {
                // last update was not within the last 1 hour, add a new entry
                appUsageDatabase.addAppUsage(newUsageInstance)
            }
        }
    }

        suspend fun cleanUpAppUsageInDB() {
            val timeString = DateTimeHelper.getDateTimeStringDaysFromNow(7)
            appUsageDatabase.deleteOldAppUsages(timeString)
        }

        /**
         * load the usage stats within a time range
         */
        fun loadGeneralAppEvents(timeInMinutes: Int): HashMap<String, AppStats> {
            // get all non-system apps
            val nonSystemApps = GeneralUtils().Utils().getNonSystemAppsList(context)

            val usm = context.getSystemService(AppCompatActivity.USAGE_STATS_SERVICE) as UsageStatsManager
            val milliSecondMultiplier: Long = 60000
            val start: Long = (ZonedDateTime.now().toInstant().toEpochMilli() - (milliSecondMultiplier * timeInMinutes))
            val usageEvents = usm.queryEvents(start, System.currentTimeMillis())
            // reset appPackage Map
            val appPackageMap: HashMap<String, ArrayList<UsageEvents.Event>> = HashMap()

            while ( usageEvents.hasNextEvent() ) {
                val usageEvent = UsageEvents.Event()
                usageEvents.getNextEvent( usageEvent )

                if ((usageEvent.eventType == 1 || usageEvent.eventType == 2 || usageEvent.eventType == 23 || usageEvent.eventType == 24)) {
                    // check to skip all system apps
                    if (nonSystemApps[usageEvent.packageName] == null) {
                        continue
                    }

                    val currentList = appPackageMap[usageEvent.packageName]
                    var newAppList = ArrayList<UsageEvents.Event>()

                    if (currentList != null) {
                        newAppList = currentList
                        newAppList.add(usageEvent)
                    } else {
                        newAppList.add(usageEvent)
                    }

                    appPackageMap[usageEvent.packageName] = newAppList

                }
            }
            // send the appPackage Map into a sorting algorithm to extract the stats
            return sortEachPackageEvent(appPackageMap)
        }

        private fun sortEachPackageEvent(appPackageMap: HashMap<String, ArrayList<UsageEvents.Event>>): HashMap<String, AppStats> {
            val appPackageStats: HashMap<String, AppStats> = HashMap()

            for ((packageName, appEventList) in appPackageMap) {

                val classActivityEventMap = HashMap<String, CustomActivityMap>()
                for(appEvent in appEventList) {
                    Log.e("APPL", "class name is ".plus(appEvent.className).plus("  event type is ").plus(appEvent.eventType).plus(" in time ").plus(DateTimeHelper.getDateTimeBreakdown(appEvent.timeStamp)))
                    val className = appEvent.className
                    if (appEvent.eventType == 1) {
                        // ACTIVITY_RESUMED, reset the current event map for this class
                        classActivityEventMap[className] = CustomActivityMap(appEvent.timeStamp, appEvent.eventType)
                        if (appPackageStats[packageName] == null) {
                            appPackageStats[packageName] = AppStats(packageName, 0, appEvent.timeStamp, 0)
                        }
                    } else if (appEvent.eventType == 2 || appEvent.eventType == 23 || appEvent.eventType == 24) {
                        // ACTIVITY_PAUSED or ACTIVITY_STOPPED or DESTROYED
                        if (classActivityEventMap[className] != null) {
                            // compute the time difference between ACTIVITY_RESUMED/STARTED and this event
                            val existingActivityMap = classActivityEventMap[className]
                            if (existingActivityMap?.eventType == 1) {
                                val timeDiff = appEvent.timeStamp - existingActivityMap.timeStamp
                                if (timeDiff > 0) {
                                    // add it to the total time spent on the app
                                    if (appPackageStats[packageName] != null) {
                                        val currentStat = appPackageStats[packageName]
                                        currentStat?.totalTimeSpent  = timeDiff + currentStat?.totalTimeSpent!!
                                        currentStat.lastTimeUsed = appEvent.timeStamp
                                        currentStat.usageCount++
                                        appPackageStats[packageName] = currentStat
                                    } else {
                                        appPackageStats[packageName] = AppStats(packageName, timeDiff, appEvent.timeStamp, 0)
                                    }
                                }
                            }

                            // reset the current event map for the class
                            classActivityEventMap[className] = CustomActivityMap(appEvent.timeStamp, appEvent.eventType)
                        }
                    }
                }
            }

            return appPackageStats
        }
    }
}