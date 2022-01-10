package com.analytics.device.models

import android.graphics.drawable.Drawable

class AppDetails(
    var id: Long,
    var appIcon: Drawable?,
    var appName: String,
    var packageName: String,
    var statPercentage: Int,
    var lastTimeUsed: String,
    var totalTimeInForeground: String,
    var statType: String,
    var frequency: Int
    )
