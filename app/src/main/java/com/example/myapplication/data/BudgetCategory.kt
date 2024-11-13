package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_category")
data class BudgetCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryName: String,
    val budgetedAmount: Double,
    val budgetLimit: Double = 0.0,
    val color: Int? = null
)

