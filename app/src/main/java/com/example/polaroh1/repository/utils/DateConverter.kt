package com.example.polaroh1.repository.utils

import androidx.room.TypeConverter
import java.util.*

class DateConverter {
    @TypeConverter
    fun timestampToDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return date.time
    }
}