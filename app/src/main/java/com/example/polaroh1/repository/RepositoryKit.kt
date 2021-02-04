package com.example.polaroh1.repository

import android.content.Context
import androidx.room.Room
import com.example.polaroh1.repository.entity.*

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

    fun insertHRList(vararg samples: HREntity) {
        databaseManager.getHRDao().insertAll(*samples)
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

    fun queryAllRecords() = databaseManager.getRecordDao().queryAll()
    fun queryAllACC() = databaseManager.getACCDao().queryAll()
    fun queryAllACCByRecordId()=databaseManager.getRecordDao().queryByRecordIdAsync()
    fun queryAllACCAsync() = databaseManager.getACCDao().queryAllAsync()

    fun insertRecord(recordEntity: RecordEntity) =
        databaseManager.getRecordDao().insert(recordEntity)


    fun clearAllTables() {
        println("ericyu - RepositoryKit.clearAllData")
        databaseManager.clearAllTables()
    }

}