package com.example.polaroh1.repository

import android.content.Context
import androidx.room.Room
import com.example.polaroh1.repository.entity.ACCEntity
import com.example.polaroh1.repository.entity.HREntity
import com.example.polaroh1.repository.entity.PPGEntity
import com.example.polaroh1.repository.entity.PPIEntity

object RepositoryKit {

    private lateinit var databaseManager: DatabaseManager

    fun setup(context: Context) {
        println("ericyu - RepositoryKit.setup, context = [${context}]")
        databaseManager =
            Room.databaseBuilder(context, DatabaseManager::class.java, "PolarDB").build()
    }

    fun insertACC(accEntity: ACCEntity) {
        databaseManager.getACCDao().insert(accEntity)
    }

    fun insertHR(hrEntity: HREntity) {
        databaseManager.getHRDao().insert(hrEntity)
    }

    fun insertACCList(vararg samples: ACCEntity) {
        databaseManager.getACCDao().insertAll(*samples)
    }


    fun insertPPGList(vararg samples: PPGEntity) {
        databaseManager.getPPGDao().insertAll(*samples)
    }


    fun insertPPIList(vararg samples: PPIEntity) {
        databaseManager.getPPIDao().insertAll(*samples)
    }

    fun queryAllACC() = databaseManager.getACCDao().queryAll()

    fun queryAllACCAsync() = databaseManager.getACCDao().queryAllAsync()


    fun clearAllData() {
        println("ericyu - RepositoryKit.clearAllData")
        databaseManager.clearAllTables()
    }

}