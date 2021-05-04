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

    @Query("DELETE FROM RECORD")
    fun deleteAll(): Int

    @Query("SELECT * FROM RECORD")
    fun queryAll(): List<RecordEntity>

    @Query("SELECT COUNT(id) FROM RECORD")
    fun queryCountAsync(): LiveData<Int>

    @Query("SELECT COUNT(id) FROM RECORD")
    fun queryCount(): Int

    @Query("SELECT * FROM RECORD")
    fun queryAllAsync(): LiveData<List<RecordEntity>>

    @Query("SELECT * FROM RECORD ORDER BY id ASC LIMIT :limit OFFSET :offset")
    fun queryRecordByCountAsync(limit: Int, offset: Int): LiveData<List<RecordEntity>>

    @Query("SELECT * FROM RECORD ORDER BY id ASC LIMIT :limit OFFSET :offset")
    fun queryRecordByCount(limit: Int, offset: Int): List<RecordEntity>

    /*@Transaction
    @Query("SELECT * FROM RECORD")
    fun queryRecordAndDetailAsync(): LiveData<List<RecordAndDetail>>*/


    /*@Transaction
    @Query("SELECT * FROM RECORD ORDER BY id ASC LIMIT :limit OFFSET :offset")
    fun queryRecordDetailByCountAsync(limit: Int, offset: Int): LiveData<List<RecordAndDetail>>

    @Transaction
    @Query("SELECT * FROM RECORD")
    fun queryRecordAndDetail(): List<RecordAndDetail>*/

}