package com.example.polaroh1.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.DateConverter
import com.example.polaroh1.repository.utils.IntListConverter
import java.util.*

@Entity(tableName = "PPG")
@TypeConverters(
    DateConverter::class,
    IntListConverter::class
)
data class PPGEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var recordId: Long = 0,
    val timestamp: Date = Date(0),
    val index: Int = 0,
    val ppg0: Int = 0,
    val ppg1: Int = 0,
    val ppg2: Int = 0,
    val ambient: Int = 0,
    val ppgDataSamples: List<Int> = listOf(),
    val ambient2: Int = 0,
    val status: Long = 0,
)
