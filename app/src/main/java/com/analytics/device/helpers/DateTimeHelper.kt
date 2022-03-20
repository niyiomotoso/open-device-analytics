package com.analytics.device.helpers

import android.text.format.DateUtils
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit

class DateTimeHelper {
    companion object {
        private const val dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss"
        /**
         * helper method to get string in format hh:mm:ss from milliseconds
         *
         * @param millis (application time in foreground)
         * @return string in format hh:mm:ss from milliseconds
         */
        fun getRunningDurationBreakdown(time: Long, hourCap: Int): String {
            var millis = time
            val hours: Long = TimeUnit.MILLISECONDS.toHours(millis)
            if (hours >= hourCap)
                return hourCap.toString().plus("h ")
            millis -= TimeUnit.HOURS.toMillis(hours)
            val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(millis)
            millis -= TimeUnit.MINUTES.toMillis(minutes)
            val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(millis)
            return hours.toString().plus("h ").plus(minutes).plus("m ").plus(seconds).plus("s")
        }

        fun getTimeInHoursAndMinuteBreakdown(time: Long, shouldMinify: Boolean): String {
            var millis = time
            val hours: Long = TimeUnit.MILLISECONDS.toHours(millis)
            millis -= TimeUnit.HOURS.toMillis(hours)
            val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(millis)
            millis -= TimeUnit.MINUTES.toMillis(minutes)
            if (shouldMinify)
                return hours.toString().plus("h ").plus(minutes).plus("m")
            return hours.toString().plus(" hr ").plus(minutes).plus(" min")
        }

        /**
         * helper method to get string  from milliseconds
         *
         * @param millis
         * @return string  from milliseconds
         */
        fun getDateTimeBreakdown(millis: Long): String {
            require(millis >= 0) { "Duration must be greater than zero!" }
            var formatter = SimpleDateFormat(dateTimePattern);
            return formatter.format(Date(millis))
        }

        /**
         * helper method to get human readable date from milliseconds
         *
         * @param millis
         * @return string  from milliseconds
         */
        fun getDateTimeForUIDisplay(millis: Long): String {
            require(millis >= 0) { "Duration must be greater than zero!" }
            var formatter = SimpleDateFormat("MMM d, hh:mm a");
            return formatter.format(Date(millis))
        }

        /**
         * helper method to check if time is within the hour
         */
        fun isWithInLastHour(datetimeRefStart: String, datetimeRefEnd: String, hours: Int): Boolean {
            val diffInMillisec = getTimeDiffInMilliSecs(datetimeRefStart, datetimeRefEnd)

            return (TimeUnit.MILLISECONDS.toHours(diffInMillisec) <= hours)
        }

        /**
         * helper method to check if time is within the hour
         */
        fun isWithInLastMinutes(datetimeRefStart: String, datetimeRefEnd: String, minutes: Int): Boolean {
            val diffInMillisec = getTimeDiffInMilliSecs(datetimeRefStart, datetimeRefEnd)

            return (TimeUnit.MILLISECONDS.toMinutes(diffInMillisec) <= minutes)
        }

        /**
         * helper method to check if time is within the secs
         */
        fun isWithInLastSecs(datetimeRefStart: String, datetimeRefEnd: String, seconds: Int): Boolean {
            val diffInMillisec = getTimeDiffInMilliSecs(datetimeRefStart,datetimeRefEnd)
            return (TimeUnit.MILLISECONDS.toSeconds(diffInMillisec) <= seconds)
        }

        /**
         * helper method to get time diff from Now
         */
        fun getTimeDiffInMilliSecs(datetimeStart: String, datetimeEnd: String): Long {
            val formatter = DateTimeFormatter.ofPattern(dateTimePattern)
            val dateRefStart = LocalDateTime.parse(datetimeStart, formatter)
            val dateRefEnd = LocalDateTime.parse(datetimeEnd, formatter)

            // val timeNow = LocalDateTime.now()
            return Duration.between(dateRefStart, dateRefEnd).toMillis()
        }


        /**
         * helper method to get time diff from Now
         */
        fun getDateTimeStringInMilliSecs(datetime: String): Long {
            val sdf = SimpleDateFormat(dateTimePattern)
            try {
                val mDate = sdf.parse(datetime)
                if (mDate != null) {
                    return mDate.time
                }
            } catch (e: ParseException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            return 0
        }

        fun getSystemCurrentTimeNow(): String {
            val df: DateFormat = SimpleDateFormat(dateTimePattern) // Quoted "Z" to indicate UTC, no timezone offset
            return  df.format(Date())
        }

        fun getDateTimeStringHoursFromNow(currentlySelectedTimeRange: Int?): String
        {
            var timeNow = LocalDateTime.now()
            var timeRange = currentlySelectedTimeRange
            if (timeRange == null) {
                // default to 24 hours
                timeRange = 24
            }
            timeNow = timeNow.minusHours(timeRange.toLong())
           return timeNow.toString()
        }

        fun getDateTimeStringDaysFromNow(daysFromNow: Int?): String
        {
            var timeNow = LocalDateTime.now()
            var timeRange = daysFromNow
            if (timeRange == null) {
                // default to 1 DAY
                timeRange = 1
            }
            timeNow = timeNow.minusDays(timeRange.toLong())
            return timeNow.toString()
        }

        fun getHourFromBeginningOfDay(): Int
        {
            val timeNow = LocalDateTime.now()
            return timeNow.hour
        }

        fun isDateTimeMillisWithinToday(dateTime: Long): Boolean
        {
            return DateUtils.isToday(dateTime);
        }
    }
}