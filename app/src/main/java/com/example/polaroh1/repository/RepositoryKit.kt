package com.example.polaroh1.repository

import android.content.Context
import androidx.room.Room
import com.example.polaroh1.repository.entity.ACCEntity

class RepositoryKit {

    companion object {
        private lateinit var databaseManager: DatabaseManager

        fun setup(context: Context) {
            println("ericyu - RepositoryKit.setup, context = [${context}]")
            databaseManager =
                Room.databaseBuilder(context, DatabaseManager::class.java, "PolarDB").build()
        }

        fun insertACC(accEntity: ACCEntity) {
            println("ericyu - RepositoryKit.insertACC, accEntity = [${accEntity}]")
            databaseManager.getACCDao().insert(accEntity)
        }

        fun insertAll(vararg samples: ACCEntity) {
            println("ericyu - RepositoryKit.insertAll, samples = [${samples}]")
            databaseManager.getACCDao().insertAll(*samples)
        }

        fun queryAllACC() =databaseManager.getACCDao().queryAll()

        fun queryAllACCAsync() = databaseManager.getACCDao().queryAllAsync()


        fun clearAllData() {
            println("ericyu - RepositoryKit.clearAllData")
            databaseManager.clearAllTables()
        }

    }

}