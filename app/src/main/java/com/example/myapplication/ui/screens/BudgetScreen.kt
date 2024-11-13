package com.example.myapplication.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.BudgetCategory
import com.example.myapplication.ui.theme.navigation.ScreenWithBottomBar
import com.example.myapplication.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BudgetScreen(navController: NavHostController, viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()
    val budgetCategories by viewModel.getBudgetCategories().collectAsState(initial = emptyList())
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryAmount by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var currentYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    ScreenWithBottomBar(navController = navController) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Define and manage your budget categories",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Input Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text("Category Name") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = showError && errorMessage.contains("Category name", ignoreCase = true),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newCategoryAmount,
                                onValueChange = { newCategoryAmount = it },
                                label = { Text("Budgeted Amount") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = showError && errorMessage.contains("Budgeted amount", ignoreCase = true),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Text(
                                        "$",
                                        modifier = Modifier.padding(start = 12.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            )

                            if (showError) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    showError = false
                                    if (newCategoryName.isBlank() || newCategoryAmount.isBlank()) {
                                        showError = true
                                        errorMessage = "Please fill in all fields."
                                        return@Button
                                    }
                                    val amount = newCategoryAmount.toDoubleOrNull()
                                    if (amount == null) {
                                        showError = true
                                        errorMessage = "Invalid budgeted amount."
                                        return@Button
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            if (isEditing) {
                                                currentCategory?.let { category ->
                                                    viewModel.updateBudgetCategory(category.categoryName, amount)
                                                    isEditing = false
                                                    currentCategory = null
                                                }
                                            } else {
                                                if (budgetCategories.any { it.categoryName == newCategoryName }) {
                                                    showError = true
                                                    errorMessage = "Category name already exists."
                                                    return@launch
                                                }
                                                val newCategory = BudgetCategory(
                                                    categoryName = newCategoryName,
                                                    budgetedAmount = amount
                                                )
                                                viewModel.insertCategory(newCategory)
                                            }
                                            newCategoryName = ""
                                            newCategoryAmount = ""
                                        } catch (e: Exception) {
                                            showError = true
                                            errorMessage = "Error: ${e.message}"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isEditing) "Update Category" else "Add Category")
                            }
                        }
                    }
                }

                // Categories Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Budget Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${budgetCategories.size} categories",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Categories List
                items(budgetCategories) { category ->
                    EnhancedBudgetCategoryCard(
                        category = category,
                        onEdit = {
                            isEditing = true
                            currentCategory = category
                            newCategoryName = category.categoryName
                            newCategoryAmount = category.budgetedAmount.toString()
                        },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteCategory(category)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedBudgetCategoryCard(
    category: BudgetCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$${NumberFormat.getNumberInstance(Locale.US).format(category.budgetedAmount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}