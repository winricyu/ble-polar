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
    @ColumnInfo(name = "RECORD_ID")
    var recordId: Long = 0,
    @ColumnInfo(name = "TIMESTAMP")
    val timestamp: Date = Date(0),
    @ColumnInfo(name = "INDEX")
    val index: Int = 0,
    @ColumnInfo(name = "PPI")
    val ppi: Int = 0,
    @ColumnInfo(name = "ERROR_ESTIMATE")
    val errorEstimate: Int = 0,
    @ColumnInfo(name = "HR")
    val hr: Int = 0,
    @ColumnInfo(name = "BLOCKER_BIT")
    val blockerBit: Boolean = false,
    @ColumnInfo(name = "SKIN_CONTACT_STATUS")
    val skinContactStatus: Boolean = false,
    @ColumnInfo(name = "SKIN_CONTACT_SUPPORTED")
    val skinContactSupported: Boolean = false,
)