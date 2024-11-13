package com.example.myapplication.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.PinnedCategoryInfo
import com.example.myapplication.data.StandardizedComparison
import com.example.myapplication.viewmodels.MainViewModel
import java.text.NumberFormat
import kotlin.math.abs


@Composable
fun OverviewSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val pinnedCategories = viewModel.pinnedCategories.collectAsState()
    // Use getCurrentMonthComparisons instead of getStandardizedComparisons
    val currentMonthComparisons = viewModel.getCurrentMonthComparisons()
    val showCategoryPicker = remember { mutableStateOf(false) }

    // Add this remember block to maintain state across recompositions
    val pinnedCategoryInfo = remember(pinnedCategories.value, currentMonthComparisons) {
        currentMonthComparisons
            .filter { it.categoryName in pinnedCategories.value }
            .map { comparison ->
                PinnedCategoryInfo(
                    categoryName = comparison.categoryName,
                    budgetedAmount = comparison.budgetedAmount,
                    actualAmount = comparison.totalSpent,
                    difference = comparison.difference,
                    percentageUsed = comparison.percentageUsed
                )
            }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Budget Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(
                onClick = { showCategoryPicker.value = true }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add categories to track"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pinnedCategories.value.isEmpty()) {
            EmptyPinnedState()
        } else {
            // Show only pinned categories
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pinnedCategoryInfo.forEach { info ->
                    QuickBudgetCard(
                        comparison = StandardizedComparison(
                            categoryName = info.categoryName,
                            totalSpent = info.actualAmount,
                            budgetedAmount = info.budgetedAmount,
                            difference = info.difference,
                            percentageUsed = info.percentageUsed
                        ),
                        onUnpin = { viewModel.togglePinnedCategory(info.categoryName) }
                    )
                }
            }
        }
    }

    if (showCategoryPicker.value) {
        CategoryPickerDialog(
            currentPinnedCategories = pinnedCategories.value,
            // Use currentMonthComparisons instead of standardizedComparisons
            allCategories = currentMonthComparisons.map { it.categoryName },
            onDismiss = { showCategoryPicker.value = false },
            onToggleCategory = { viewModel.togglePinnedCategory(it) }
        )
    }
}

@Composable
private fun QuickBudgetCard(
    comparison: StandardizedComparison,
    onUnpin: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comparison.categoryName,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onUnpin) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Unpin category"
                    )
                }
            }

            LinearProgressIndicator(
                progress = (comparison.percentageUsed.toFloat() / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(abs(comparison.totalSpent)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.labelMedium
                    )
                    // Handle zero budget case
                    val remaining = if (comparison.budgetedAmount == 0.0) {
                        0.0
                    } else {
                        comparison.budgetedAmount - abs(comparison.totalSpent)
                    }
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(remaining),
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            comparison.budgetedAmount == 0.0 -> MaterialTheme.colorScheme.error
                            remaining < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    currentPinnedCategories: Set<String>,
    allCategories: List<String>,
    onDismiss: () -> Unit,
    onToggleCategory: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Categories to Track") },
        text = {
            LazyColumn {
                items(
                    count = allCategories.size,
                    key = { allCategories[it] }
                ) { index ->
                    val category = allCategories[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCategory(category) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category)
                        if (category in currentPinnedCategories) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun EmptyPinnedState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select categories to track by tapping the + button",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}