package com.example.myapplication.extensions

import com.example.myapplication.data.StandardizedComparison

fun List<StandardizedComparison>.topThreeCloseToOverBudget(): List<StandardizedComparison> {
    return this.filter { it.difference < 0 }
        .sortedBy { it.difference }
        .take(3)
}


