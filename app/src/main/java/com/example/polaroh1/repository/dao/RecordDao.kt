package com.example.polaroh1.repository.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.polaroh1.repository.entity.ACCEntity
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

    @Query("SELECT * FROM ACC, RECORD WHERE ACC.RECORD_ID = RECORD.id")
    fun queryByRecordIdAsync():LiveData<List<ACCEntity>>

    /*data class RecordDetail(
        @ColumnInfo(name = "ID")
        val id: Int = 0,
        @ColumnInfo(name = "TIMESTAMP")
        val timestamp: Date = Date(0),
        @ColumnInfo(name = "HRS")
        val hr: List<Int> = listOf(),
        @ColumnInfo(name = "PPG0S")
        val ppg0: List<Int> = listOf(),
        @ColumnInfo(name = "PPIS")
        val ppi: List<Int> = listOf(),
        @ColumnInfo(name = "XS")
        val x: List<Int> = listOf(),
        @ColumnInfo(name = "YS")
        val y: List<Int> = listOf(),
        @ColumnInfo(name = "ZS")
        val z: List<Int> = listOf()

    )*/
}