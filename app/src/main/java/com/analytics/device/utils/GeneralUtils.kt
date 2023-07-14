package com.analytics.device.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.analytics.device.models.AppStats


class GeneralUtils {
    inner class Utils {
        /**
         * check if the application info is still existing in the device / otherwise it's not possible to show app detail
         * @return true if application info is available
         */
        fun isAppInformationAvailable(context: Context, packageName: String): Boolean {
            val ai: ApplicationInfo = getAppInformation(context, packageName)
            println("AIIIII  ".plus(ai))
            return ai.packageName != null
        }

        fun getAppInformation (context: Context, packageName: String): ApplicationInfo {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    context.packageManager
                        .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                return ApplicationInfo()
            }
        }

        fun getNonSystemAppsList(context: Context) : Map<String,String> {
            lateinit var appInfos: MutableList<ApplicationInfo>
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appInfos = context.packageManager.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                appInfos = context.packageManager
                    .getInstalledApplications(PackageManager.GET_META_DATA)
            }

            val appInfoMap = HashMap<String,String>()

            for ( appInfo in appInfos ) {
                if ( appInfo.flags != ApplicationInfo.FLAG_SYSTEM ) {
                    appInfoMap[ appInfo.packageName ]= context.packageManager.getApplicationLabel( appInfo ).toString()
                }
            }
            return appInfoMap
        }
    }
}