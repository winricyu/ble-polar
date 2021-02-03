package com.example.polaroh1.repository.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.polaroh1.repository.entity.ACCEntity

@Dao
interface ACCDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(accEntity: ACCEntity)

    @Insert
    fun insertAll(vararg samples: ACCEntity)

    @Delete
    fun delete(data: ACCEntity)

    @Query("SELECT * FROM ACC")
    fun queryAll():List<ACCEntity>

    @Query("SELECT * FROM ACC")
    fun queryAllAsync():LiveData<List<ACCEntity>>
}