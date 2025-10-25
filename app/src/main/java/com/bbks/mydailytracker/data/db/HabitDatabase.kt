package com.bbks.mydailytracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bbks.mydailytracker.data.db.Converters
import com.bbks.mydailytracker.data.model.DailyHabitResult
import com.bbks.mydailytracker.data.model.Habit
import com.bbks.mydailytracker.data.model.HabitCheck

@Database(entities = [Habit::class, HabitCheck::class, DailyHabitResult::class], version = 15)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCheckDao(): HabitCheckDao
    abstract fun dailyHabitResultDao(): DailyHabitResultDao

    companion object {
        @Volatile private var INSTANCE: HabitDatabase? = null

        // 🔧 14 → 15 마이그레이션: 중복 정리 → 유니크 인덱스 생성
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) 같은 (habitId, date)에서 최신 id 1건만 남기기
                db.execSQL("""
                    DELETE FROM daily_habit_results
                    WHERE id NOT IN (
                        SELECT MAX(id)
                        FROM daily_habit_results
                        GROUP BY habitId, date
                    )
                """.trimIndent())


                // ✅ 2) 기존 잘못된 인덱스 이름 제거
                db.execSQL("DROP INDEX IF EXISTS idx_results_habit_date")

                // 3) (habitId, date) 유니크 인덱스 생성
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_daily_habit_results_habitId_date
                    ON daily_habit_results(habitId, date)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habits.db"
                ).fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13)
                    .addMigrations(MIGRATION_14_15)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}