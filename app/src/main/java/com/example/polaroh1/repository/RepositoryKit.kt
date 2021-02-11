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

    suspend fun insertACC(accEntity: ACCEntity) {
        databaseManager.getACCDao().insert(accEntity)
    }

    suspend fun insertHR(hrEntity: HREntity) {
        databaseManager.getHRDao().insert(hrEntity)
    }

    suspend fun insertHRList(vararg samples: HREntity) {
        databaseManager.getHRDao().insertAll(*samples)
    }

    suspend fun insertACCList(vararg samples: ACCEntity) {
        databaseManager.getACCDao().insertAll(*samples)
    }

    suspend fun insertPPGList(vararg samples: PPGEntity) {
        databaseManager.getPPGDao().insertAll(*samples)
    }

    suspend fun insertPPIList(vararg samples: PPIEntity) {
        databaseManager.getPPIDao().insertAll(*samples)
    }

    suspend fun queryAllRecords() = databaseManager.getRecordDao().queryAll()
    suspend fun queryAllACC() = databaseManager.getACCDao().queryAll()
    suspend fun queryRecordAndDetailAsync() =
        databaseManager.getRecordDao().queryRecordAndDetailAsync()
    suspend fun queryRecordAndDetail() =
        databaseManager.getRecordDao().queryRecordAndDetail()

    suspend fun queryAllACCAsync() = databaseManager.getACCDao().queryAllAsync()

    suspend fun insertRecord(recordEntity: RecordEntity) =
        databaseManager.getRecordDao().insert(recordEntity)


    suspend fun clearAllTables() {
        println("ericyu - RepositoryKit.clearAllData")
        databaseManager.clearAllTables()
    }

}