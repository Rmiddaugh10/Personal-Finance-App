package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.BudgetCategory
import java.time.LocalDate

// Common button used across all filters
@Composable
fun FilterToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Black
        )
    }
}

// Source Filter Row
@Composable
fun SourceFilterRow(
    selectedSource: String?,
    availableSources: List<String>,
    onSourceSelected: (String?) -> Unit
) {
    Column {
        Text(
            "Source",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedSource == null,
                    onClick = { onSourceSelected(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            items(availableSources) { source ->
                FilterChip(
                    selected = source == selectedSource,
                    onClick = { onSourceSelected(source) },
                    label = { Text(source) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

// Year Filter Row
@Composable
fun YearFilterRow(
    selectedYears: List<Int>,
    onYearSelected: (Int) -> Unit
) {
    Column {
        Text(
            "Years",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val currentYear = LocalDate.now().year
            val years = (currentYear - 5..currentYear).toList()
            items(years) { year ->
                FilterToggleButton(
                    text = year.toString(),
                    selected = selectedYears.contains(year),
                    onClick = { onYearSelected(year) }
                )
            }
        }
    }
}

// Month Filter Row
@Composable
fun MonthFilterRow(
    selectedMonths: List<Int>,
    onMonthSelected: (Int) -> Unit
) {
    Column {
        Text(
            "Months",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((1..12).toList()) { month ->
                FilterToggleButton(
                    text = getMonthName(month),
                    selected = selectedMonths.contains(month),
                    onClick = { onMonthSelected(month) }
                )
            }
        }
    }
}

// Category Filter Row
@Composable
fun CategoryFilterRow(
    budgetCategories: List<BudgetCategory>,
    selectedCategories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    Column {
        Text(
            "Categories",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(budgetCategories) { category ->
                FilterToggleButton(
                    text = category.categoryName,
                    selected = selectedCategories.contains(category.categoryName),
                    onClick = { onCategorySelected(category.categoryName) }
                )
            }
        }
    }
}

private fun getMonthName(month: Int): String = when (month) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    12 -> "December"
    else -> "Invalid Month"
}