package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    budgetCategories: List<String>,  // Add this parameter
    onAddTransaction: (Double, String, String, LocalDate) -> Unit,  // Add this parameter
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, description: String, category: String, date: LocalDate) -> Unit
) {
    // Define states for the input fields
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull()
                    if (parsedAmount != null) {
                        onConfirm(parsedAmount, description, category, date)
                    }
                }
            ) {
                Text("Add Transaction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Add New Transaction") },
        text = {
            Column {
                // Amount field
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category field
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date selection (basic version for example purposes)
                OutlinedTextField(
                    value = date.toString(),
                    onValueChange = { /* No-op here; provide a date picker in actual UI */ },
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AddTransactionDialogPreview() {
    AddTransactionDialog(
        budgetCategories = listOf("Food", "Transport", "Utilities"), // Example categories
        onAddTransaction = { _, _, _, _ -> }, // Dummy function for preview
        onDismiss = {},
        onConfirm = { _, _, _, _ -> }
    )
}

