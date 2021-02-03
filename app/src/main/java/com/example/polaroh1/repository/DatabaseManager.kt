package com.example.polaroh1.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.polaroh1.repository.dao.ACCDao
import com.example.polaroh1.repository.dao.HRDao
import com.example.polaroh1.repository.dao.PPGDao
import com.example.polaroh1.repository.dao.PPIDao
import com.example.polaroh1.repository.entity.ACCEntity
import com.example.polaroh1.repository.entity.HREntity
import com.example.polaroh1.repository.entity.PPGEntity
import com.example.polaroh1.repository.entity.PPIEntity

@Database(
    entities = [
        PPGEntity::class, PPIEntity::class, ACCEntity::class, HREntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DatabaseManager : RoomDatabase() {

    abstract fun getHRDao(): HRDao
    abstract fun getPPGDao(): PPGDao
    abstract fun getPPIDao(): PPIDao
    abstract fun getACCDao(): ACCDao

}