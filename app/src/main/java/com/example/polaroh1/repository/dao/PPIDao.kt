package com.example.polaroh1.repository.dao

import androidx.room.*
import com.example.polaroh1.repository.entity.PPIEntity

@Dao
interface PPIDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ppiEntity: PPIEntity)

    @Insert
    fun insertAll(vararg samples: PPIEntity)

    @Delete
    fun delete(ppiEntity: PPIEntity)

    @Query("DELETE FROM PPI")
    fun deleteAll():Int

    @Query("SELECT * FROM PPI")
    fun queryAll(): List<PPIEntity>
}