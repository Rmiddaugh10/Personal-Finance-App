package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_comparison")
data class BudgetComparison(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val year: Int,
    val month: Int,
    val categoryName: String,
    val budgetedAmount: Double,
    val actualAmount: Double,
    var difference: Double = budgetedAmount - actualAmount,
    var percentageUsed: Double = if (budgetedAmount != 0.0)
        (actualAmount / budgetedAmount) * 100
    else 0.0
)