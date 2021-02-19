package com.example.polaroh1.repository.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.polaroh1.repository.entity.SleepEntity

@Dao
interface SleepDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sleepEntity: SleepEntity): Long

    @Delete
    fun delete(data: SleepEntity)

    @Query("DELETE FROM SLEEP")
    fun deleteAll(): Int

    @Query("SELECT * FROM SLEEP")
    fun queryAll(): List<SleepEntity>

    @Query("SELECT * FROM SLEEP")
    fun queryAllAsync(): LiveData<List<SleepEntity>>

}