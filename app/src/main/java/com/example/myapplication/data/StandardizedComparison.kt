package com.example.myapplication.data

data class StandardizedComparison(
    val categoryName: String,
    val totalSpent: Double,
    val budgetedAmount: Double,
    val difference: Double,
    val percentageUsed: Double,
    val isIncome: Boolean = false
)

