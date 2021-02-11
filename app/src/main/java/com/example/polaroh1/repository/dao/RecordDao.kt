package com.example.polaroh1.repository.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.polaroh1.repository.entity.ACCEntity
import com.example.polaroh1.repository.entity.RecordAndDetail
import com.example.polaroh1.repository.entity.RecordEntity
import java.util.*

@Dao
interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recordEntity: RecordEntity): Long

    @Insert
    fun insertAll(vararg samples: RecordEntity)

    @Delete
    fun delete(data: RecordEntity)

    @Query("SELECT * FROM RECORD")
    fun queryAll(): List<RecordEntity>

    @Query("SELECT * FROM RECORD")
    fun queryAllAsync(): LiveData<List<RecordEntity>>

    @Transaction
    @Query("SELECT * FROM RECORD")
    fun queryRecordAndDetailAsync():LiveData<List<RecordAndDetail>>

    @Transaction
    @Query("SELECT * FROM RECORD")
    fun queryRecordAndDetail():List<RecordAndDetail>

}