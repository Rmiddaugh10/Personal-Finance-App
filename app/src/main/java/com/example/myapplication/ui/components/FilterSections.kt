package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.BudgetCategory
import com.example.myapplication.data.Filter

// FilterSections.kt
@Composable
fun ExpenseFilterSection(
    currentFilters: Filter.ExpenseFilters,
    budgetCategories: List<BudgetCategory>,
    selectedSource: String?,
    availableSources: List<String>,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSourceSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleMedium)

        if (availableSources.isNotEmpty()) {
            SourceFilterRow(
                selectedSource = selectedSource,
                availableSources = availableSources,
                onSourceSelected = onSourceSelected
            )
        }

        YearFilterRow(
            selectedYears = currentFilters.years,
            onYearSelected = onYearSelected
        )

        MonthFilterRow(
            selectedMonths = currentFilters.months,
            onMonthSelected = onMonthSelected
        )

        CategoryFilterRow(
            budgetCategories = budgetCategories,
            selectedCategories = currentFilters.categories,
            onCategorySelected = onCategorySelected
        )
    }
}

@Composable
fun BudgetFilterSection(
    currentFilters: Filter.BudgetComparisonFilters,
    budgetCategories: List<BudgetCategory>,
    selectedSource: String?,
    availableSources: List<String>,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onCategorySelected: (String) -> Unit,
    onSourceSelected: (String?) -> Unit
) {

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleMedium)

        if (availableSources.isNotEmpty()) {
            SourceFilterRow(
                selectedSource = selectedSource,
                availableSources = availableSources,
                onSourceSelected = onSourceSelected
            )
        }

        YearFilterRow(
            selectedYears = currentFilters.years,
            onYearSelected = onYearSelected
        )

        MonthFilterRow(
            selectedMonths = currentFilters.months,
            onMonthSelected = onMonthSelected
        )

        CategoryFilterRow(
            budgetCategories = budgetCategories,
            selectedCategories = currentFilters.categories,
            onCategorySelected = onCategorySelected
        )
    }
}