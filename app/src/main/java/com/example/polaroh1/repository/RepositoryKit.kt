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

    fun insertHR(hrEntity: HREntity) {
        databaseManager.getHRDao().insert(hrEntity)
    }

    fun insertACC(accEntity: ACCEntity) {
        databaseManager.getACCDao().insert(accEntity)
    }

    fun insertPPG(ppgEntity: PPGEntity) {
        databaseManager.getPPGDao().insert(ppgEntity)
    }

    fun insertPPI(ppiEntity: PPIEntity) {
        databaseManager.getPPIDao().insert(ppiEntity)
    }

    fun insertSleep(sleepEntity: SleepEntity) {
        databaseManager.getSleepDao().insert(sleepEntity)
    }

    fun insertHRList(vararg samples: HREntity) {
        databaseManager.getHRDao().insertAll(*samples)
    }

    fun insertACCList(vararg samples: ACCEntity) {
        databaseManager.getACCDao().insertAll(*samples)
    }

    fun insertPPGList(vararg samples: PPGEntity) {
        println(
            "RepositoryKit.insertPPGList , samples = ${
                when (samples.size) {
                    126, 144 -> "${samples.size}"
                    else -> "[${samples.size}]"

                }
            }"
        )
        databaseManager.getPPGDao().insertAll(*samples)
    }

    fun insertPPIList(vararg samples: PPIEntity) {
        databaseManager.getPPIDao().insertAll(*samples)
    }

    fun queryAllRecordsAsync() = databaseManager.getRecordDao().queryAllAsync()
    fun queryAllRecords() = databaseManager.getRecordDao().queryAll()
    fun queryAllACC() = databaseManager.getACCDao().queryAll()
    fun queryRecordByCountAsync(limit: Int, offset: Int) =
        databaseManager.getRecordDao().queryRecordByCountAsync(limit, offset)
    suspend fun queryRecordByCount(limit: Int, offset: Int) =
        databaseManager.getRecordDao().queryRecordByCount(limit, offset)
    //fun queryRecordAndDetailAsync() = databaseManager.getRecordDao().queryRecordAndDetailAsync()

    /* fun queryRecordDetailByCountAsync(limit: Int, offset: Int) =
         databaseManager.getRecordDao().queryRecordDetailByCountAsync(limit, offset)

     fun queryRecordAndDetail() =
         databaseManager.getRecordDao().queryRecordAndDetail()*/

    fun queryAllACCAsync() = databaseManager.getACCDao().queryAllAsync()

    fun insertRecord(recordEntity: RecordEntity) =
        databaseManager.getRecordDao().insert(recordEntity)


    fun clearAllTables() {
        println("ericyu - RepositoryKit.clearAllData")
        databaseManager.clearAllTables()
    }

    fun clearAllTableEntries() {
        println("ericyu - RepositoryKit.clearAllTableEntries")
        println("getRecordDao().deleteAll: ${databaseManager.getRecordDao().deleteAll()}")
        println("getHRDao().deleteAll: ${databaseManager.getHRDao().deleteAll()}")
        println("getPPIDao().deleteAll: ${databaseManager.getPPIDao().deleteAll()}")
        println("getPPGDao().deleteAll: ${databaseManager.getPPGDao().deleteAll()}")
        println("getACCDao().deleteAll: ${databaseManager.getACCDao().deleteAll()}")
        println("getACCDao().deleteAll: ${databaseManager.getSleepDao().deleteAll()}")
    }

    fun queryRecordCountAsync() = databaseManager.getRecordDao().queryCountAsync()
    fun queryRecordCount() = databaseManager.getRecordDao().queryCount()


}