package com.analytics.device.constants

import com.analytics.device.helpers.DateTimeHelper

class TimeRangeMap {
    var timeRange : HashMap<String, Int> = HashMap()
    init {
        timeRange.put("today", DateTimeHelper.getHourFromBeginningOfDay())
        timeRange.put("1 hour", 1)
        timeRange.put("3 hours", 3)
        timeRange.put("6 hours", 6)
        timeRange.put("12 hours", 12)
        timeRange.put("1 day", 24)
        timeRange.put("3 days", 72)
        timeRange.put("5 days", 120)
        timeRange.put("1 week", 168)
        timeRange.put("2 weeks", 336)
        timeRange.put("3 weeks", 504)
        timeRange.put("1 month", 720)
        timeRange.put("3 months", 2160)
    }

    fun getHour(key: String): Int?
    {
        return timeRange[key]
    }


}