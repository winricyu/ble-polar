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
    @ColumnInfo(name = "RECORD_ID")
    var recordId: Long = 0,
    @ColumnInfo(name = "TIMESTAMP")
    val timestamp: Date = Date(0),
    @ColumnInfo(name = "INDEX")
    val index: Int = 0,
    @ColumnInfo(name = "X")
    val x: Int = 0,
    @ColumnInfo(name = "Y")
    val y: Int = 0,
    @ColumnInfo(name = "Z")
    val z: Int = 0
)