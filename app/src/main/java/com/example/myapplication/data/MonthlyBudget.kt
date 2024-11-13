package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// MonthlyBudget.kt
@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val year: Int,
    val month: Int,
    val categoryName: String,
    val budgetedAmount: Double
)
