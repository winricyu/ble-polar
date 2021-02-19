package com.example.polaroh1.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.IntListConverter

@Entity(tableName = "SLEEP")
@TypeConverters(
    IntListConverter::class
)
data class SleepEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val stress: Float = 0f,
    val hrArray: List<Int> = listOf(),
    val ppiArray: List<Int> = listOf(),
    val ppiLen: Int = 0,
    val rmssdArray: List<Int> = listOf(),
)
