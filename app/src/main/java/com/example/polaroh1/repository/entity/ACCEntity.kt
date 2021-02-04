package com.example.polaroh1.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.DateConverter
import java.util.*

@Entity(tableName = "ACC")
@TypeConverters(
    DateConverter::class
)
data class ACCEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var recordId: Long = 0,
    val timestamp: Date = Date(0),
    val index: Int = 0,
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0
)