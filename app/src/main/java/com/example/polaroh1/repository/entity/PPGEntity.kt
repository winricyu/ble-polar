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
    @ColumnInfo(name = "TIMESTAMP")
    val timestamp: Date,
    @ColumnInfo(name = "INDEX")
    val index: Int = 0,
    @ColumnInfo(name = "PPG0")
    val ppg0: Int = 0,
    @ColumnInfo(name = "PPG1")
    val ppg1: Int = 0,
    @ColumnInfo(name = "PPG2")
    val ppg2: Int = 0,
    @ColumnInfo(name = "AMBIENT")
    val ambient: Int = 0,
    @ColumnInfo(name = "PPG_DATA_SAMPLES")
    val ppgDataSamples: List<Int>,
    @ColumnInfo(name = "AMBIENT2")
    val ambient2: Int = 0,
    @ColumnInfo(name = "STATUS")
    val status: Long = 0,
)
