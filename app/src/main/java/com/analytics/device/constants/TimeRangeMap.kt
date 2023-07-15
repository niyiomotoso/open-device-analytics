package com.analytics.device.constants

import com.analytics.device.helpers.DateTimeHelper

class TimeRangeMap {
    var timeRange : HashMap<String, Int> = HashMap()
    init {
        timeRange.put("today", DateTimeHelper.getMinutesFromBeginningOfDay())
        timeRange.put("1 hour", 1 * 60)
        timeRange.put("3 hours", 3 * 60)
        timeRange.put("6 hours", 6 * 60)
        timeRange.put("12 hours", 12 * 60)
        timeRange.put("1 day", 24 * 60)
        timeRange.put("3 days", 72 * 60)
        timeRange.put("5 days", 120 * 60)
        timeRange.put("1 week", 168 * 60)
        timeRange.put("2 weeks", 336 * 60)
        timeRange.put("3 weeks", 504 * 60)
        timeRange.put("1 month", 720 * 60)
        timeRange.put("3 months", 2160* 60)
    }

    fun getHour(key: String): Int?
    {
        return timeRange[key]
    }


}