package com.example.polaroh1.repository.utils

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import java.util.*

class IntListConverter {

    @TypeConverter
    fun intListToString(list: List<Int>): String {
        return if (list.isEmpty()) "" else list.joinToString(",")
    }

    @TypeConverter
    fun stringToIntList(data: String): List<Int> {
        return if (data.isNullOrBlank()) listOf() else data.split(",").map { it.toInt() }

    }
}

