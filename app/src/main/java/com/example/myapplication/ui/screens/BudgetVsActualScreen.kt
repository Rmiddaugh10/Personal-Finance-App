package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.StandardizedComparison
import com.example.myapplication.extensions.topThreeCloseToOverBudget
import com.example.myapplication.ui.components.BudgetFilterSection
import com.example.myapplication.ui.components.DonutChartCompose
import com.example.myapplication.viewmodels.MainViewModel
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.abs


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetVsActualScreen(
    viewModel: MainViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val filteredComparisons by viewModel.filteredComparisons.collectAsState()
    val budgetCategories by viewModel.getBudgetCategories().collectAsState(initial = emptyList())
    var showFilters by remember { mutableStateOf(false) }
    val budgetFilters by viewModel.budgetFilters.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val availableSources by viewModel.availableSources.collectAsState()
    val standardizedComparisons = viewModel.getStandardizedComparisons()

    // Update the filter count calculation
    val activeFilterCount = budgetFilters.categories.size +
            budgetFilters.months.size +
            budgetFilters.years.size +
            (if (selectedSource != null) 1 else 0)



    // Apply filters when they change
    LaunchedEffect(budgetFilters, selectedSource) {
        viewModel.applyFilters()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 56.dp),
            contentPadding = PaddingValues(top = 16.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Budget Overview",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FiltersButton(
                            showFilters = showFilters,
                            onShowFiltersChange = { showFilters = it },
                            activeFilterCount = activeFilterCount
                        )
                        AnimatedVisibility(visible = showFilters) {
                            Column {
                                BudgetFilterSection(
                                    currentFilters = budgetFilters,
                                    budgetCategories = budgetCategories,
                                    selectedSource = selectedSource,
                                    availableSources = availableSources,
                                    onSourceSelected = { viewModel.setSelectedSource(it) },
                                    onYearSelected = { year ->
                                        viewModel.updateBudgetYearFilters(
                                            if (budgetFilters.years.contains(year))
                                                budgetFilters.years - year
                                            else
                                                budgetFilters.years + year
                                        )
                                    },
                                    onMonthSelected = { month ->
                                        viewModel.updateBudgetMonthFilters(
                                            if (budgetFilters.months.contains(month))
                                                budgetFilters.months - month
                                            else
                                                budgetFilters.months + month
                                        )
                                    },
                                    onCategorySelected = { category ->
                                        viewModel.updateBudgetCategoryFilters(
                                            if (budgetFilters.categories.contains(category))
                                                budgetFilters.categories - category
                                            else
                                                budgetFilters.categories + category
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredComparisons.isNotEmpty()) {
                item {
                    SummarySection(viewModel = viewModel)
                }
                item {
                    EnhancedDonutChartSection(
                        totalBudget = viewModel.calculateTotalBudget(),
                        totalActual = viewModel.calculateTotalSpent()
                    )
                }
                item {
                    CategoryHeader(
                        title = "Categories Close to Budget",
                        subtitle = "Top performing budget categories"
                    )
                }

                // Calculate top three comparisons from standardizedComparisons
                val topThreeComparisons = standardizedComparisons.topThreeCloseToOverBudget()

                // Display top three comparisons
                items(topThreeComparisons) { comparison ->
                    EnhancedBudgetComparisonItem(comparison)
                }
                item {
                    CategoryHeader(
                        title = "All Budget Comparisons",
                        subtitle = "Detailed view of all categories"
                    )
                }

                // Display all budget comparisons
                items(
                    items = standardizedComparisons,
                    key = { comparison -> "${comparison.categoryName}_${comparison.budgetedAmount}_${comparison.totalSpent}_${UUID.randomUUID()}" }
                ) { comparison ->
                    EnhancedBudgetComparisonItem(comparison)
                }
            } else {
                item {
                    EnhancedEmptyState()
                }
            }
        }
    }
}




@Composable
private fun FiltersButton(
    showFilters: Boolean,
    onShowFiltersChange: (Boolean) -> Unit,
    activeFilterCount: Int,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        onClick = { onShowFiltersChange(!showFilters) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = if (showFilters) "Hide Filters" else "Show Filters",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (showFilters) "Hide Filters" else "Show Filters",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activeFilterCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = activeFilterCount.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Icon(
                imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun SummarySection(
    viewModel: MainViewModel
) {
    val totalIncome = viewModel.calculateTotalIncome()
    // Use the ViewModel's calculation methods instead of passed parameters
    val calculatedBudget = viewModel.calculateTotalBudget()
    val calculatedSpent = viewModel.calculateTotalSpent()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // First row with Budget and Spent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Budget",
                amount = calculatedBudget.toFloat(), // Use calculated value
                icon = Icons.Outlined.AccountBalance,
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Total Spent",
                amount = calculatedSpent.toFloat(), // Use calculated value
                icon = Icons.Outlined.Payment,
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        // Second row with Income (if exists)
        if (totalIncome > 0) {
            SummaryCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Total Income",
                amount = totalIncome.toFloat(),
                icon = Icons.Outlined.TrendingUp,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
    }
}



@Composable
private fun SummaryCard(
    title: String,
    amount: Float,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface, // Default to your surface color
    amountColor: Color = MaterialTheme.colorScheme.onSurface, // Default to your onSurface color
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = NumberFormat.getCurrencyInstance().format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}

@Composable
fun EnhancedDonutChartSection(
    totalBudget: Double,
    totalActual: Double
) {
    val absoluteActual = Math.abs(totalActual)
    val percentage = if (totalBudget > 0) (absoluteActual / totalBudget) * 100 else 0.0
    val centerText = String.format("%.1f%%", percentage)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(350.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Budget Usage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            DonutChartCompose(
                totalBudget = totalBudget,
                totalActual = totalActual,
                centerText = centerText
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EnhancedLegendItem(color = Color(0xFFFFB74D), text = "Budgeted")
                Spacer(modifier = Modifier.width(24.dp))
                EnhancedLegendItem(color = Color(0xFF66BB6A), text = "Actual")
            }
        }
    }
}


@Composable
private fun EnhancedLegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CategoryHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun EnhancedBudgetComparisonItem(comparison: StandardizedComparison) {
    val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val animatedProgress = remember { Animatable(0f) }

    // Determine card color based on category type and status
    val cardColor = when {
        comparison.isIncome -> MaterialTheme.colorScheme.tertiaryContainer
        comparison.budgetedAmount == 0.0 -> MaterialTheme.colorScheme.surfaceVariant // Unbudgeted
        comparison.percentageUsed > 100 -> MaterialTheme.colorScheme.errorContainer
        comparison.percentageUsed > 90 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    // Determine progress bar color
    val progressColor = when {
        comparison.isIncome -> MaterialTheme.colorScheme.tertiary
        comparison.budgetedAmount == 0.0 -> MaterialTheme.colorScheme.secondary // Unbudgeted
        comparison.percentageUsed > 100 -> MaterialTheme.colorScheme.error
        comparison.percentageUsed > 90 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }


    LaunchedEffect(comparison) {
        animatedProgress.animateTo(
            targetValue = (comparison.percentageUsed / 100f).coerceIn(0.0, 1.0).toFloat(),
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comparison.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Show different chip based on category type
                Surface(
                    color = when {
                        comparison.isIncome -> MaterialTheme.colorScheme.tertiary
                        comparison.budgetedAmount == 0.0 -> MaterialTheme.colorScheme.secondary
                        comparison.percentageUsed > 100 -> MaterialTheme.colorScheme.error
                        comparison.percentageUsed > 90 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = when {
                            comparison.isIncome -> "Income"
                            comparison.budgetedAmount == 0.0 -> "Unbudgeted"
                            else -> "${comparison.percentageUsed.toInt()}% Used"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Only show progress bar for expense categories
            if (!comparison.isIncome) {
                LinearProgressIndicator(
                    progress = animatedProgress.value,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (comparison.isIncome) "Earned" else "Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = numberFormat.format(abs(comparison.totalSpent)),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (comparison.isIncome)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (comparison.isIncome) "Expected" else "Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = numberFormat.format(abs(comparison.budgetedAmount)),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}


@Composable
fun EnhancedEmptyState() {
    Text(text = "No data available", style = MaterialTheme.typography.bodyMedium)
}

