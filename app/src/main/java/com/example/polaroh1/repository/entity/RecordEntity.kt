package com.example.polaroh1.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.DateConverter
import com.example.polaroh1.repository.utils.IntListConverter
import java.util.*

@Entity(tableName = "RECORD")
@TypeConverters(
    DateConverter::class, IntListConverter::class
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Date = Date(0),
    var hrList: List<Int> = listOf(),
    var ppg1List: List<Int> = listOf(),
    var ppg2List: List<Int> = listOf(),
    var ppg3List: List<Int> = listOf(),
    var ambient1List: List<Int> = listOf(),
    var ambient2List: List<Int> = listOf(),
    var accXList: List<Int> = listOf(),
    var accYList: List<Int> = listOf(),
    var accZList: List<Int> = listOf(),
)