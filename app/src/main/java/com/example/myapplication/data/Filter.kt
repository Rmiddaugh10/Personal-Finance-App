package com.example.myapplication.data

sealed class Filter {
    data class ExpenseFilters(
        val years: List<Int> = emptyList(),
        val months: List<Int> = emptyList(),
        val categories: List<String> = emptyList(),
        val source: String? = null
    ) {
        fun hasActiveFilters() = years.isNotEmpty() ||
                months.isNotEmpty() ||
                categories.isNotEmpty() ||
                source != null
    }

    data class BudgetComparisonFilters(
        val years: List<Int> = emptyList(),
        val months: List<Int> = emptyList(),
        val categories: List<String> = emptyList()
    )
}
