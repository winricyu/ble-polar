package com.example.polaroh1.repository.dao

import androidx.room.*
import com.example.polaroh1.repository.entity.HREntity

@Dao
interface HRDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hrEntity: HREntity)

    @Insert
    fun insertAll(vararg samples: HREntity)

    @Delete
    fun delete(data: HREntity)

    @Query("DELETE FROM HR")
    fun deleteAll():Int

    @Query("SELECT * FROM HR")
    fun queryAll():List<HREntity>
}