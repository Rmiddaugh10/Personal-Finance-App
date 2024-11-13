package com.example.myapplication.data

data class ChartData(
    val totalBudget: Double,
    val totalActual: Double,
    val percentageUsed: Double,
    val categoryBreakdown: List<CategoryBreakdown> = emptyList()
)

