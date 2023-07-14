package com.analytics.device

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.analytics.device.adapters.TopAppsAdapter
import com.analytics.device.constants.StatsTypeMap
import com.analytics.device.helpers.DateTimeHelper
import com.analytics.device.models.AppDetails
import com.analytics.device.models.AppStats
import com.analytics.device.utils.GeneralUtils
import com.analytics.device.utils.UsageUtils
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Collections


class MainActivity : AppCompatActivity() {

    var enableBtn: Button? = null
    var permissionDescriptionTv: TextView? = null
    var screenTimeTv: TextView? = null
    private var totalAppUsageUsageCountTv: TextView? = null
    var runningAppsTv: TextView? = null
    var deviceUptimeTv: TextView? = null
    var memoryUsageTv: TextView? = null
    var usageCountCardLayout: LinearLayout? = null
    var screenTimeCardLayout: LinearLayout? = null
    var viewAllTv: TextView? = null
    var mainActivityLayout: LinearLayout? = null
    var noPermissionLayout: LinearLayout? = null
    var btnScrollRight: ImageButton? = null
    var btnScrollLeft: ImageButton? = null
    var horizontalScrollview: HorizontalScrollView? = null
    var appRecyclerView: RecyclerView? = null
    var compiledAppList: ArrayList<AppDetails> = ArrayList()
    private val TOP_APP_DISPLAY_LIMIT: Int = 5
    var appPackageStats: HashMap<String, AppStats> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableBtn = findViewById(R.id.enable_btn)
        permissionDescriptionTv = findViewById(R.id.permission_description_tv)
        mainActivityLayout = findViewById(R.id.main_activity_layout)
        noPermissionLayout = findViewById(R.id.no_permission_layout)
        screenTimeTv = findViewById(R.id.screen_time_text)
        deviceUptimeTv = findViewById(R.id.device_uptime_tv)
        usageCountCardLayout = findViewById(R.id.usage_count_layout)
        screenTimeCardLayout = findViewById(R.id.screen_time_layout)
        horizontalScrollview = findViewById(R.id.horizontal_scrollview)
        viewAllTv = findViewById(R.id.view_all_tv)
        totalAppUsageUsageCountTv = findViewById(R.id.total_app_usage_count_text)
        // getting the recyclerview by its id
        appRecyclerView = findViewById(R.id.top_apps_list)
        runningAppsTv = findViewById(R.id.utilized_apps_tv)
        memoryUsageTv = findViewById(R.id.memory_usage_tv)
        btnScrollLeft = findViewById(R.id.btn_scroll_left)
        btnScrollRight = findViewById(R.id.btn_scroll_right)

        // this creates a vertical layout Manager
        appRecyclerView?.layoutManager = LinearLayoutManager(this)
        loadGeneralAppStats()
        getMemory()
        getDeviceUpTime()
        usageCountMoreTvListener()
        rightScrollBtnListener()
        leftScrollBtnListener()
        viewAllTvListener()
        screenTimeMoreTvListener()
        attachPrivacyPolicy()
    }

    private fun attachPrivacyPolicy() {
        val privacyPolicy = findViewById<TextView>(R.id.privacy_policy)
        privacyPolicy.movementMethod = LinkMovementMethod.getInstance()
        val privacyPolicyOnPermission = findViewById<TextView>(R.id.privacy_policy_2)
        privacyPolicyOnPermission.movementMethod = LinkMovementMethod.getInstance()

        val terms = findViewById<TextView>(R.id.terms_of_service)
        terms.movementMethod = LinkMovementMethod.getInstance()
        val termsOnPermission = findViewById<TextView>(R.id.terms_of_service_2)
        termsOnPermission.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun usageCountMoreTvListener() {
        usageCountCardLayout?.setOnClickListener {
            val intent = Intent(this, UsageActivity::class.java)
            intent.putExtra("selectedStatType", StatsTypeMap().USAGE)
            startActivity(intent)
        }
    }

    private fun viewAllTvListener() {
        viewAllTv?.setOnClickListener {
            val intent = Intent(this, UsageActivity::class.java)
            intent.putExtra("selectedStatType", StatsTypeMap().TOTAL_TIME_USED)
            startActivity(intent)
        }
    }

    private fun rightScrollBtnListener() {
        btnScrollRight?.setOnClickListener {
            horizontalScrollview?.scrollTo(
                horizontalScrollview?.scrollX as Int + 60,
                horizontalScrollview?.scrollY as Int
            )
        }
    }

    private fun leftScrollBtnListener() {
        btnScrollLeft?.setOnClickListener {
            horizontalScrollview?.scrollTo(
                horizontalScrollview?.scrollX as Int - 60,
                horizontalScrollview?.scrollY as Int
            )
        }
    }

    private fun screenTimeMoreTvListener() {
        screenTimeCardLayout?.setOnClickListener {
            val intent = Intent(this, UsageActivity::class.java)
            intent.putExtra("selectedStatType", StatsTypeMap().TOTAL_TIME_USED)
            startActivity(intent)
        }
    }

    private fun getDeviceUpTime()
    {
        val deviceUptimeInMills = SystemClock.uptimeMillis()
        deviceUptimeTv?.text = DateTimeHelper.getTimeInHoursAndMinuteBreakdown(deviceUptimeInMills, true)
    }

    override fun onResume() {
        loadGeneralAppStats()
        super.onResume()
    }


    /**
     * load the usage stats for last 24h
     */
    private fun loadGeneralAppStats() {
        appPackageStats = HashMap()
        appPackageStats = UsageUtils(baseContext).Usage().loadGeneralAppEvents(DateTimeHelper.getHourFromBeginningOfDay())
        computeTopAppUsages(appPackageStats)
        getTodayAppUsageCount(appPackageStats)

    }

    private fun computeTopAppUsages(mySortedMap: Map<String, AppStats>) {
        clearOutCurrentList()
        val usageStatsList: List<AppStats> = ArrayList(mySortedMap.values)
        var totalTimeRunning: Long = 0
        var utilizedAppsTotal: Int = 0

        // sort the applications by time spent in foreground
        Collections.sort(usageStatsList) { z1: AppStats, z2: AppStats -> z1.totalTimeSpent.compareTo(z2.totalTimeSpent) }

        // get total time of apps usage to calculate the usagePercentage for each app
        val totalTime = usageStatsList.stream().map { obj: AppStats -> obj.totalTimeSpent }
            .mapToLong { obj: Long -> obj }.sum()

        // fill the appsList
        for (usageStats in usageStatsList) {
            try {
                val lastUsedAppTime: Long = 0
                if (usageStats.totalTimeSpent > lastUsedAppTime) {
                    totalTimeRunning += usageStats.totalTimeSpent
                    val packageName = usageStats.packageName
                    if (DateTimeHelper.isDateTimeMillisWithinToday(usageStats.lastTimeUsed)) {
                        utilizedAppsTotal++
                    }
                    var icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_launcher_background)
                    val packageNames = packageName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    var appName = packageNames[packageNames.size - 1].trim { it <= ' ' }
                    if (GeneralUtils().Utils().isAppInformationAvailable(applicationContext, usageStats.packageName)) {
                        val ai: ApplicationInfo = GeneralUtils().Utils().getAppInformation(applicationContext, usageStats.packageName)
                        icon = applicationContext.packageManager.getApplicationIcon(ai)
                        appName = applicationContext.packageManager.getApplicationLabel(ai)
                            .toString()
                    }
                    if (appName.trim() != "") {
                        val timeInForeground = DateTimeHelper.getRunningDurationBreakdown(usageStats.totalTimeSpent, 24)
                        val usagePercentage = (usageStats.totalTimeSpent * 100 / totalTime).toInt()
                        val usageStatDTO = AppDetails(packageName.hashCode().toLong(), icon, appName, packageName, usagePercentage, "", timeInForeground, StatsTypeMap().TOTAL_TIME_USED, 0)
                        compiledAppList.add(usageStatDTO)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        // update total app screen time
        screenTimeTv?.text = DateTimeHelper.getTimeInHoursAndMinuteBreakdown(totalTimeRunning, false)
        //update number of app utilized today
        runningAppsTv?.text = utilizedAppsTotal.toString()
        bindCompiledListToUi(compiledAppList)
    }

    private fun clearOutCurrentList()
    {
        compiledAppList.clear()
        bindCompiledListToUi(compiledAppList)
    }

    // each time the application gets in foreground -> getGrantStatus and render the corresponding buttons
    override fun onStart() {
        super.onStart()
        if (!getGrantStatus()) {
            showHideNoPermission()
            enableBtn?.setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                )
            }
        } else {
            showHideItemsWhenShowApps()
        }
    }

    private fun getTodayAppUsageCount(sortedMap: HashMap<String, AppStats>)
    {
        val usageList: List<AppStats> = ArrayList(sortedMap.values)
        var totalFreq = 0
        // sort the applications by time spent in foreground
        Collections.sort(usageList) { z1: AppStats, z2: AppStats -> z1.usageCount.compareTo(z2.usageCount) }

        for (appStat in usageList) {
            var appName = ""
            val packageName = appStat.packageName
            if (GeneralUtils().Utils().isAppInformationAvailable(applicationContext, packageName)) {
                val ai: ApplicationInfo = GeneralUtils().Utils().getAppInformation(applicationContext, packageName)
                appName = applicationContext.packageManager.getApplicationLabel(ai)
                    .toString()
            }
            if (appName.trim() != "") {
                totalFreq++
            }
        }
        totalAppUsageUsageCountTv?.text  = totalFreq.toString().plus(" interactions")
    }

    private fun getMemory()
    {
        // Declaring and Initializing the ActivityManager
        val actManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        // Declaring MemoryInfo object
        val memInfo = ActivityManager.MemoryInfo()
        // Fetching the data from the ActivityManager
        actManager.getMemoryInfo(memInfo)
        // Fetching the available and total memory and converting into Giga Bytes
        val availMemory = memInfo.availMem.toDouble()/(1024*1024*1024)
        val totalMemory= memInfo.totalMem.toDouble()/(1024*1024*1024)
        val df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.DOWN
        val availMemoryStr = df.format(availMemory)
        val totalMemoryStr = df.format(totalMemory)
        memoryUsageTv?.text = availMemoryStr.plus("GB/").plus(totalMemoryStr).plus("GB")
    }

    private fun bindCompiledListToUi (compiledAppList: ArrayList<AppDetails>)
    {
        // reverse the list to get most usage first
        compiledAppList.reverse()
        var topAppsList: List<AppDetails>
        topAppsList = compiledAppList
        if (compiledAppList.size > TOP_APP_DISPLAY_LIMIT)
        {
            topAppsList = topAppsList.subList(0, 5)

        }
        // This will pass the ArrayList to our Adapter
        val adapter = TopAppsAdapter(topAppsList)
        // Setting the Adapter with the recyclerview
        appRecyclerView?.adapter = adapter
        // showHideItemsWhenShowApps()
    }

    /**
     * check if PACKAGE_USAGE_STATS permission is allowed for this application
     * @return true if permission granted
     */
    private fun getGrantStatus(): Boolean {
        val appOps = applicationContext
            .getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            OPSTR_GET_USAGE_STATS,
            Process.myUid(), applicationContext.packageName
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            applicationContext.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) === PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /**
     * helper method used to show/hide items in the view when  PACKAGE_USAGE_STATS permission is not allowed
     */
    private fun showHideNoPermission() {
        noPermissionLayout?.visibility = View.VISIBLE
        mainActivityLayout?.visibility = View.GONE
    }

    /**
     * helper method used to show/hide items in the view when showing the apps list
     */
    private fun showHideItemsWhenShowApps() {
        mainActivityLayout?.visibility = View.VISIBLE
        noPermissionLayout?.visibility = View.GONE
    }
}