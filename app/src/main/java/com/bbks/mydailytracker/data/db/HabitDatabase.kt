package com.bbks.mydailytracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bbks.mydailytracker.data.db.Converters
import com.bbks.mydailytracker.data.model.DailyHabitResult
import com.bbks.mydailytracker.data.model.Habit
import com.bbks.mydailytracker.data.model.HabitCheck

@Database(entities = [Habit::class, HabitCheck::class, DailyHabitResult::class], version = 14)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCheckDao(): HabitCheckDao
    abstract fun dailyHabitResultDao(): DailyHabitResultDao

    companion object {
        @Volatile private var INSTANCE: HabitDatabase? = null

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habits.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}