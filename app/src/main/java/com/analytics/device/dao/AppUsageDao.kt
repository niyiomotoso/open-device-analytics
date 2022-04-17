package com.analytics.device.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.analytics.device.models.AppUsage
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAppUsage(appUsage: AppUsage)

    @Query("SELECT * FROM app_usage WHERE CAST(strftime('%s', createdAt)  AS  integer) >= CAST(strftime('%s', :startTime)  AS  integer) ORDER BY lastOpenTime DESC")
    fun getAppUsages(startTime: String): Flow<List<AppUsage>>

    @Query("SELECT * FROM app_usage where packageName = :packageName ORDER BY id DESC LIMIT 1")
    suspend fun getMostRecentAppUsage(packageName: String): AppUsage?

    @Update
    suspend fun updateAppUsage(appUsage: AppUsage)

    @Delete
    suspend fun deleteAppUsage(appUsage: AppUsage)

    @Query("DELETE FROM app_usage WHERE CAST(strftime('%s', createdAt)  AS  integer) <= CAST(strftime('%s', :createdAt)  AS  integer)")
    suspend fun deleteOldAppUsages(createdAt: String)
}