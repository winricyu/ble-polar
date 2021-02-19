package com.example.polaroh1.repository.entity

import androidx.room.*
import com.example.polaroh1.repository.utils.DateConverter
import com.example.polaroh1.repository.utils.IntListConverter
import java.util.*

@Entity(tableName = "HR")
@TypeConverters(
    IntListConverter::class
)
data class HREntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var recordId: Long = 0,
    val index: Int = 0,
    val hr: Int = 0,
    val rrs: List<Int> = listOf(),
    val rrsMs: List<Int> = listOf(),
    val contactStatus: Boolean = false,
    val contactStatusSupported: Boolean = false,
    val rrAvailable: Boolean = false,
)