package com.example.polaroh1.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.polaroh1.repository.utils.DateConverter
import com.example.polaroh1.repository.utils.IntListConverter
import java.util.*

@Entity(tableName = "HR")
@TypeConverters(
    DateConverter::class,
    IntListConverter::class
)
data class HREntity(
    @PrimaryKey(autoGenerate = true)
    val id:Int=0,
    @ColumnInfo(name = "TIMESTAMP")
    val timestamp: Date =Date(0),
    @ColumnInfo(name = "INDEX")
    val index: Int=0,
    @ColumnInfo(name = "HR")
    val hr: Int=0,
    @ColumnInfo(name = "RRS")
    val rrs: List<Int> = listOf(),
    @ColumnInfo(name = "RRS_MS")
    val rrsMs: List<Int> = listOf(),
    @ColumnInfo(name = "CONTACT_STATUS")
    val contactStatus: Boolean=false,
    @ColumnInfo(name = "CONTACT_STATUS_SUPPORTED")
    val contactStatusSupported: Boolean=false,
    @ColumnInfo(name = "RR_AVAILABLE")
    val rrAvailable: Boolean=false,
)