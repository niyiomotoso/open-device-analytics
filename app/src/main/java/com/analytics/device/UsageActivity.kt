package com.analytics.device

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.analytics.device.adapters.LastTimeOpenAdapter
import com.analytics.device.adapters.TotalTimeUsedAdapter
import com.analytics.device.adapters.UsageCountAdapter
import com.analytics.device.constants.StatsTypeMap
import com.analytics.device.constants.TimeRangeMap
import com.analytics.device.helpers.DateTimeHelper
import com.analytics.device.models.AppDetails
import com.analytics.device.models.AppStats
import com.analytics.device.utils.GeneralUtils
import com.analytics.device.utils.UsageUtils
import java.util.Collections
import kotlin.math.roundToInt

open class UsageActivity: AppCompatActivity()  {
    var pageTitleTv:TextView? = null
    var filters: LinearLayout? = null
    var appRecyclerView: RecyclerView? = null
    var currentlySelectedStatType: String? = null
    var currentlySelectedTimeRange: Int? = null
    var currentlySelectedTimeRangeString: String? = null
    var arrowBackIv: ImageView? = null
    var compiledAppList: ArrayList<AppDetails> = ArrayList()
    var appPackageStats: HashMap<String, AppStats> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)
        pageTitleTv = findViewById(R.id.page_title_tv)
        // getting the recyclerview by its id
        appRecyclerView = findViewById(R.id.apps_list)
        filters = findViewById(R.id.filters)
        currentlySelectedStatType = StatsTypeMap().TOTAL_TIME_USED
        currentlySelectedTimeRange = DateTimeHelper.getMinutesFromBeginningOfDay()
        currentlySelectedTimeRangeString = "today"
        arrowBackIv = findViewById(R.id.arrow_back)

        // this creates a vertical layout Manager
        appRecyclerView?.layoutManager = LinearLayoutManager(this)

        preloadDataFromIntent()
        attachTimeRangeSelector()
        attachStatsTypeSelector()
        arrowBackIvListener()
        loadGeneralAppStats()
        attachPrivacyPolicy()
    }

    private fun attachPrivacyPolicy() {
        val privacyPolicy = findViewById<TextView>(R.id.privacy_policy)
        privacyPolicy.movementMethod = LinkMovementMethod.getInstance()

        // Text view number 2 to add hyperlink
        val terms = findViewById<TextView>(R.id.terms_of_service)
        terms.movementMethod = LinkMovementMethod.getInstance()
    }


    private fun arrowBackIvListener() {
        arrowBackIv?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun preloadDataFromIntent() {
        val intent: Intent = intent
        val data = intent.getStringExtra("selectedStatType")
        if (data != null)
            currentlySelectedStatType = data
    }

    private fun setPageTitle(statType: String?, timeRange: String?)
    {
        when(statType) {
            StatsTypeMap().LAST_TIME_OPENED -> {
                if (timeRange == "today")
                    pageTitleTv?.text = ("Last time opened for today")
                else
                    pageTitleTv?.text = ("Last time opened within the last ").plus(timeRange)
            }

            StatsTypeMap().TOTAL_TIME_USED -> {
                if (timeRange == "today")
                    pageTitleTv?.text = ("Total time spent for today")
                else
                    pageTitleTv?.text = ("Total time spent on apps used in the last ").plus(timeRange)
            }

            StatsTypeMap().USAGE -> {
                if (timeRange == "today")
                    pageTitleTv?.text = ("showing app usage count for today")
                else
                    pageTitleTv?.text = ("showing app usage count in the last ").plus(timeRange)
            }
        }
    }

    private fun attachTimeRangeSelector() {
        val ranges = resources.getStringArray(R.array.time_range)
        // access the spinner
        val spinner = findViewById<Spinner>(R.id.timerange_dropdown)
        if (spinner != null) {
            val adapter = ArrayAdapter(this,
                R.layout.spinner_item, ranges)
            spinner.adapter = adapter
            spinner.setSelection(0, false)

            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {
                    val selectedRange = ranges[position]
                    currentlySelectedTimeRangeString = selectedRange
                    val rangeInHour = TimeRangeMap().getHour(selectedRange)
                    if (rangeInHour != null) {
                        currentlySelectedTimeRange = rangeInHour
                        loadGeneralAppStats()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }
    }

    private fun attachStatsTypeSelector() {
        val statsTypes = resources.getStringArray(R.array.stats_type)
        // access the spinner
        val spinner = findViewById<Spinner>(R.id.stats_dropdown)
        if (spinner != null) {
            val adapter = ArrayAdapter(this,
                R.layout.spinner_item, statsTypes)
            spinner.adapter = adapter
            // set initial selection based on default
            when(currentlySelectedStatType)
            {
                StatsTypeMap().USAGE -> {
                    spinner.setSelection(1, false)
                }

                StatsTypeMap().TOTAL_TIME_USED -> {
                    spinner.setSelection(0, false)
                }
            }

            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {
                    val selectedType = statsTypes[position]
                    if (selectedType != null) {
                        currentlySelectedStatType = selectedType
                        loadGeneralAppStats()
                    } else {
                        Toast.makeText(
                            this@UsageActivity,
                            "Invalid selection " + statsTypes[position],
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    /**
     * load the usage stats for last 24h
     */
    private fun loadGeneralAppStats() {
        appPackageStats = HashMap()
        appPackageStats = UsageUtils(baseContext).Usage().loadGeneralAppEvents(currentlySelectedTimeRange!!)

        // Group the usageStats by application and sort them by last time used
        when(currentlySelectedStatType) {
            StatsTypeMap().LAST_TIME_OPENED -> {
                showAppsLastUsage(appPackageStats)
            }

            StatsTypeMap().TOTAL_TIME_USED -> {
                showAppsTotalTimeInForeGround(appPackageStats)
            }

            StatsTypeMap().USAGE -> {
                showAppsUsageFrequency(appPackageStats)
            }
        }

        setPageTitle(currentlySelectedStatType, currentlySelectedTimeRangeString)
    }

    private fun showAppsLastUsage(sortedMap: Map<String, AppStats>) {
        clearOutCurrentList()
        val usageStatsList: List<AppStats> = ArrayList(sortedMap.values)

        // sort the applications by time spent in foreground
        Collections.sort(usageStatsList) { z1: AppStats, z2: AppStats -> z1.lastTimeUsed.compareTo(z2.lastTimeUsed) }

        // get total time of apps usage to calculate the usagePercentage for each app
        val totalTime = usageStatsList.stream().map { obj: AppStats -> obj.lastTimeUsed }
            .mapToLong { obj: Long -> obj }.sum()

        //fill the appsList
        for (usageStats in usageStatsList) {
            try {
                val lastUsedAppTime: Long = 0
                // Filter out lastTimeOpened that are greater than currentlySelectedTimeRange
                val selectedDateTimeString = DateTimeHelper.getDateTimeStringHoursFromNow(currentlySelectedTimeRange)
                val selectedDateTimeInMillisecs = DateTimeHelper.getDateTimeStringInMilliSecs(selectedDateTimeString)
                if (usageStats.lastTimeUsed > lastUsedAppTime && usageStats.lastTimeUsed > selectedDateTimeInMillisecs) {
                    val packageName = usageStats.packageName
                    var icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_launcher_background)
                    val packageNames = packageName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    var appName = packageNames[packageNames.size - 1].trim { it <= ' ' }
                    if (GeneralUtils().Utils().isAppInformationAvailable(applicationContext, packageName)) {
                        val ai: ApplicationInfo = GeneralUtils().Utils().getAppInformation(applicationContext, packageName)
                        icon = applicationContext.packageManager.getApplicationIcon(ai)
                        appName = applicationContext.packageManager.getApplicationLabel(ai)
                            .toString()
                    }
                    if (appName.trim() != "") {
                        val lastTimeUsed = DateTimeHelper.getDateTimeForUIDisplay(usageStats.lastTimeUsed)
                        val usagePercentage = (usageStats.lastTimeUsed * 100 / totalTime).toInt()
                        val usageStatDTO = AppDetails(packageName.hashCode().toLong(), icon, appName, packageName, usagePercentage, lastTimeUsed, "", StatsTypeMap().LAST_TIME_OPENED, 0 )
                        // Add the new note at the top of the list
                        compiledAppList.add(usageStatDTO)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        bindCompiledListToUi()
    }

    private fun showAppsUsageFrequency(sortedMap: Map<String, AppStats>) {
        clearOutCurrentList()
        val usageList: List<AppStats> = ArrayList(sortedMap.values)
        val tempCompiledAppList: ArrayList<AppDetails> = ArrayList()
        if (usageList.isNotEmpty()) {
            // sort the applications by time spent in foreground
            Collections.sort(usageList) { z1: AppStats, z2: AppStats -> z1.usageCount.compareTo(z2.usageCount) }

            // get total time of apps usage to calculate the usagePercentage for each app
            val totalFreq = usageList.stream().map { obj: AppStats -> obj.usageCount }
                .mapToInt { obj: Int -> obj }.sum()

            for (appStat in usageList) {
                try {
                    var icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_launcher_background)
                    var appName = ""
                    val packageName = appStat.packageName
                    if (GeneralUtils().Utils().isAppInformationAvailable(applicationContext, packageName)) {
                        val ai: ApplicationInfo = GeneralUtils().Utils().getAppInformation(applicationContext, packageName)
                        icon = applicationContext.packageManager.getApplicationIcon(ai)
                        appName = applicationContext.packageManager.getApplicationLabel(ai)
                            .toString()
                    }

                    if (appName.trim() != "") {
                        // half the freq data
                        val freq = appStat.usageCount
                        var freqPercentage = 0
                        if (totalFreq != 0) {
                            freqPercentage = ((freq.toDouble() * 100) / totalFreq.toDouble()).roundToInt()
                        }

                        val usageStatDTO = AppDetails(packageName.hashCode().toLong(), icon, appName, packageName, freqPercentage, "", "", StatsTypeMap().USAGE, freq)
                        // Add the new note at the top of the list
                        tempCompiledAppList.add(usageStatDTO)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }

            compiledAppList = tempCompiledAppList
            bindCompiledListToUi()
        }
    }


    private fun clearOutCurrentList()
    {
        compiledAppList.clear()
        bindCompiledListToUi()
    }

    private fun showAppsTotalTimeInForeGround(sortedMap: Map<String, AppStats>) {
        clearOutCurrentList()
        val usageStatsList: List<AppStats> = ArrayList(sortedMap.values)

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
                    val packageName = usageStats.packageName
                    var icon = ContextCompat.getDrawable(baseContext, R.drawable.ic_launcher_background)
                    val packageNames = packageName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    var appName = packageNames[packageNames.size - 1].trim { it <= ' ' }
                    if (GeneralUtils().Utils().isAppInformationAvailable(applicationContext, usageStats.packageName)) {
                        val ai: ApplicationInfo = GeneralUtils().Utils().getAppInformation(applicationContext, packageName)
                        icon = applicationContext.packageManager.getApplicationIcon(ai)
                        appName = applicationContext.packageManager.getApplicationLabel(ai)
                            .toString()
                    }
                    if (appName.trim() != "") {
                        val timeInForeground = DateTimeHelper.getRunningDurationBreakdown(usageStats.totalTimeSpent, currentlySelectedTimeRange!!)
                        val usagePercentage = (usageStats.totalTimeSpent * 100 / totalTime).toInt()
                        val usageStatDTO = AppDetails(packageName.hashCode().toLong(), icon, appName, packageName, usagePercentage, "", timeInForeground, StatsTypeMap().TOTAL_TIME_USED, 0)
                        compiledAppList.add(usageStatDTO)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }
        bindCompiledListToUi()
    }

   private fun bindCompiledListToUi ()
   {
       // reverse the list to get most usage first
       compiledAppList.reverse()
        // refresh the page title
       when(currentlySelectedStatType) {
           StatsTypeMap().LAST_TIME_OPENED -> {
               appRecyclerView?.adapter = LastTimeOpenAdapter(compiledAppList)
           }
           StatsTypeMap().TOTAL_TIME_USED -> {
               appRecyclerView?.adapter = TotalTimeUsedAdapter(compiledAppList)
           }
           StatsTypeMap().USAGE -> {
               appRecyclerView?.adapter = UsageCountAdapter(compiledAppList)
           }
       }
   }
}