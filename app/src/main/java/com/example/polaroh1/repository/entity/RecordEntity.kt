package com.example.polaroh1.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.DateConverter
import java.util.*

@Entity(tableName = "RECORD")
@TypeConverters(
    DateConverter::class
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "TIMESTAMP")
    val timestamp: Date = Date(0)
)