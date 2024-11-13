package com.example.myapplication.data

data class PinnedCategoryInfo(
    val categoryName: String,
    val budgetedAmount: Double,
    val actualAmount: Double,
    val difference: Double,
    val percentageUsed: Double
)
