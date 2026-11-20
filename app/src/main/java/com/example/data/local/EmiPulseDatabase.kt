package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedEmiSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EmiPulseDatabase : RoomDatabase() {

    abstract fun savedEmiSessionDao(): SavedEmiSessionDao

    companion object {
        @Volatile
        private var instance: EmiPulseDatabase? = null

        fun getInstance(context: Context): EmiPulseDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EmiPulseDatabase::class.java,
                    "emi_pulse.db"
                ).build().also { instance = it }
            }
        }
    }
}
