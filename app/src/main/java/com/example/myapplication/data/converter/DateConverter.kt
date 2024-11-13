package com.example.myapplication.data.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class DateConverter {
    @TypeConverter
    fun toDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun fromDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}
