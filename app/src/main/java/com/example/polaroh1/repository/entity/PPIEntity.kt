package com.example.polaroh1.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.DateConverter
import java.util.*

@Entity(tableName = "PPI")
@TypeConverters(
    DateConverter::class
)
data class PPIEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var recordId: Long = 0,
    val timestamp: Date = Date(0),
    val index: Int = 0,
    val ppi: Int = 0,
    val errorEstimate: Int = 0,
    val hr: Int = 0,
    val blockerBit: Boolean = false,
    val skinContactStatus: Boolean = false,
    val skinContactSupported: Boolean = false,
)