package com.example.polaroh1.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.polaroh1.repository.dao.*
import com.example.polaroh1.repository.entity.*

@Database(
    entities = [
        PPGEntity::class, PPIEntity::class, ACCEntity::class, HREntity::class, RecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DatabaseManager : RoomDatabase() {

    abstract fun getHRDao(): HRDao
    abstract fun getPPGDao(): PPGDao
    abstract fun getPPIDao(): PPIDao
    abstract fun getACCDao(): ACCDao
    abstract fun getRecordDao(): RecordDao

}