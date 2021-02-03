package com.example.polaroh1.repository.dao

import androidx.room.*
import com.example.polaroh1.repository.entity.PPGEntity

@Dao
interface PPGDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ppgEntity: PPGEntity)

    @Insert
    fun insertAll(vararg samples: PPGEntity)

    @Delete
    fun delete(data: PPGEntity)

    @Query("SELECT * FROM PPG")
    fun queryAll():List<PPGEntity>
}