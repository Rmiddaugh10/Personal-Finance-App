package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val description: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val source: String? = null
) {
    val isIncome: Boolean
        get() = category.contains("Income", ignoreCase = true)
}
