package com.analytics.device.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class AppUsage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "appName")
    val appName: String,
    @ColumnInfo(name = "packageName")
    val packageName: String?,
    @ColumnInfo(name = "lastOpenTime")
    var lastOpenTime: String,
    @ColumnInfo(name = "createdAt")
    var createdAt: String,
    @ColumnInfo(name = "frequency", defaultValue = "0")
    var frequency: Int
)