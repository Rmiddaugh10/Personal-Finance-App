@file:OptIn(ExperimentalMaterialApi::class)

package com.example.myapplication.ui.screens

//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.BudgetCategory
import com.example.myapplication.data.ExpenseData
import com.example.myapplication.ui.components.AddTransactionDialog
import com.example.myapplication.ui.components.ExpenseFilterSection
import com.example.myapplication.ui.theme.navigation.ScreenWithBottomBar
import com.example.myapplication.viewmodels.MainViewModel


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExpenseItem(
    expense: ExpenseData,
    budgetCategories: List<BudgetCategory>,
    onCategoryChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToEnd || it == DismissValue.DismissedToStart) {
                showDialog = true
                false
            } else {
                false
            }
        }
    )


    SwipeToDismiss(
        state = dismissState,
        background = {
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection != null) MaterialTheme.colorScheme.error else Color.Transparent
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .background(color, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (dismissState.dismissDirection != null) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        },
        dismissContent = {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date: ${expense.month}/${expense.day}/${expense.year}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // Use source instead of extracted bankName
                        expense.source?.let { source ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = source,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", Math.abs(expense.amount))}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (expense.amount < 0) Color.Red else Color(0xFF2E7D32) // Green for income
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Category: ${expense.category}", style = MaterialTheme.typography.bodyMedium)
                    Text("Description: ${expense.description}", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Change Category")
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        budgetCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.categoryName) },
                                onClick = {
                                    onCategoryChange(category.categoryName)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this expense? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(navController: NavHostController, viewModel: MainViewModel, onImportClick: () -> Unit) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val budgetCategories by viewModel.getBudgetCategories().collectAsState(initial = emptyList())
    var showFilters by remember { mutableStateOf(false) }
    val expenseFilters by viewModel.expenseFilters.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Calculate active filter count
    val activeFilterCount = expenseFilters.categories.size +
            expenseFilters.months.size +
            expenseFilters.years.size

    ScreenWithBottomBar(navController = navController) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Title Row: Center-align "Transactions" on a separate line
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally) // Center-align title
                    )

                    Spacer(modifier = Modifier.height(8.dp)) // Add some space between title and buttons

                    // Button Row: Align buttons to the end of the row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onImportClick,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Import CSV")
                        }
                        Button(onClick = { showDialog = true }) {
                            Text("Clear CSV")
                        }
                    }
                }
            }


            // Enhanced Filter Button
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                onClick = { showFilters = !showFilters }
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

            // Enhanced Filter Section
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ExpenseFilterSection( // Changed from FilterSection to ExpenseFilterSection
                    currentFilters = expenseFilters,
                    budgetCategories = budgetCategories,
                    selectedSource = viewModel.selectedSource.collectAsState().value,
                    availableSources = viewModel.availableSources.collectAsState().value,
                    onYearSelected = { year ->
                        viewModel.updateYearFilters(
                            if (expenseFilters.years.contains(year))
                                expenseFilters.years - year
                            else
                                expenseFilters.years + year
                        )
                    },
                    onMonthSelected = { month ->
                        viewModel.updateMonthFilters(
                            if (expenseFilters.months.contains(month))
                                expenseFilters.months - month
                            else
                                expenseFilters.months + month
                        )
                    },
                    onCategorySelected = { category ->
                        viewModel.updateCategoryFilters(
                            if (expenseFilters.categories.contains(category))
                                expenseFilters.categories - category
                            else
                                expenseFilters.categories + category
                        )
                    },
                    onSourceSelected = { source ->
                        viewModel.setSelectedSource(source)
                    }
                )
            }


            // Expense List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseItem(
                        expense = expense,
                        budgetCategories = budgetCategories,
                        onCategoryChange = { newCategory ->
                            viewModel.updateExpenseCategory(expense.id, newCategory)
                        },
                        onDelete = { viewModel.deleteExpense(expense) }
                    )
                }
            }

            // FAB for adding transactions
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Cash Transaction")
                }
            }
        }

        // Dialogs
        if (showAddDialog) {
            AddTransactionDialog(
                budgetCategories = budgetCategories.map { it.categoryName },
                onAddTransaction = { amount, description, category, date ->
                    viewModel.addCashTransaction(amount, description, category, date)
                    showAddDialog = false
                },
                onConfirm = { amount, description, category, date ->
                    viewModel.addCashTransaction(amount, description, category, date)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to clear all CSV data? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllExpenses()
                            showDialog = false
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}





