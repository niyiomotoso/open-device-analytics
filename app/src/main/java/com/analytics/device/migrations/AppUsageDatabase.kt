package com.analytics.device.migrations

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.analytics.device.converters.AppUsageConverters
import com.analytics.device.dao.AppUsageDao
import com.analytics.device.models.AppUsage

@Database(
    entities = [AppUsage::class],
    version = 1,
    exportSchema = true
)

@TypeConverters(AppUsageConverters::class)
abstract class AppUsageDatabase: RoomDatabase() {


    abstract fun appUsageDao(): AppUsageDao

    companion object {

        @Volatile
        private var INSTANCE: AppUsageDatabase? = null

        fun getDatabase(context: Context): AppUsageDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            if (INSTANCE == null) {
                synchronized(this) {
                    // Pass the database to the INSTANCE
                    INSTANCE = buildDatabase(context)
                }
            }
            // Return database.
            return INSTANCE!!
        }

        private fun buildDatabase(context: Context): AppUsageDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppUsageDatabase::class.java,
                "app_usage_database"
            )
                .build()
        }
    }
}
