package com.example.myapplication

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.ExpenseRepository
import com.example.myapplication.ui.screens.BottomNavigationBar
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.navigation.AppNavigation
import com.example.myapplication.utils.CsvParserUtility
import com.example.myapplication.viewmodels.ExpenseViewModelFactory
import com.example.myapplication.viewmodels.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : ComponentActivity() {
    private lateinit var repository: ExpenseRepository

    private val viewModel: MainViewModel by lazy {
        ExpenseViewModelFactory(repository).create(MainViewModel::class.java)
    }

    private fun isCsvFile(uri: Uri): Boolean {
        val mimeType = contentResolver.getType(uri)
        Log.d("MainActivity", "Detected MIME Type: $mimeType")

        // Check only the MIME type for CSV validation
        return mimeType in listOf(
            "text/csv",
            "application/vnd.ms-excel",
            "text/comma-separated-values",
            "text/plain"
        )
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Log.d("MainActivity", "File URI selected: $it")
            if (isCsvFile(it)) handleCsvFile(it)
            else {
                Log.d("MainActivity", "File is not a CSV")
                Toast.makeText(this, "Please select a CSV file", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            viewModel.setPermissionGranted(isGranted)
            if (!isGranted) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = (application as MyApplication).repository

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val isLoading by viewModel.isLoading.collectAsState()

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Scaffold(
                        bottomBar = { BottomNavigationBar(navController = navController) }
                    ) { paddingValues ->
                        AppNavigation(
                            navController = navController,
                            viewModel = viewModel,
                            paddingValues = paddingValues,
                            repository = repository,
                            onImportClick = { filePickerLauncher.launch("*/*") }
                        )
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun handleCsvFile(uri: Uri) {
        viewModel.viewModelScope.launch {
            viewModel.setLoading(true)
            try {
                val expenses = withContext(Dispatchers.IO) {
                    CsvParserUtility.parseCsvFile(uri, contentResolver)
                }

                Log.d("MainActivity", "Parsed ${expenses.size} total transactions")

                var inserted = 0
                var skipped = 0
                var autoCategorized = 0

                withContext(Dispatchers.IO) {
                    expenses.chunked(50).forEach { batch ->
                        batch.forEach { expense ->
                            try {
                                val exists = repository.transactionExists(
                                    year = expense.year,
                                    month = expense.month,
                                    day = expense.day,
                                    amount = expense.amount,
                                    description = expense.description
                                )

                                if (!exists) {
                                    // Try to find a suggested category based on historical data
                                    val suggestedCategory = repository.suggestCategory(expense.description)

                                    if (suggestedCategory != "Uncategorized") {
                                        autoCategorized++
                                        Log.d("MainActivity", "Auto-categorized '${expense.description}' as '$suggestedCategory'")
                                    }

                                    // Insert expense with the suggested category
                                    repository.updateExpenseWithCategory(expense, suggestedCategory)
                                    inserted++

                                    Log.d("MainActivity", """
                                    Inserted transaction:
                                    Date: ${expense.month}/${expense.day}/${expense.year}
                                    Amount: ${expense.amount}
                                    Description: ${expense.description}
                                    Category: $suggestedCategory
                                """.trimIndent())
                                } else {
                                    skipped++
                                    Log.d("MainActivity", "Skipped duplicate: ${expense.description}")
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error processing transaction", e)
                            }
                        }
                    }
                }

                val message = buildString {
                    append("Import complete\n")
                    append("Total parsed: ${expenses.size}\n")
                    append("Inserted: $inserted\n")
                    append("Auto-categorized: $autoCategorized\n")
                    append("Skipped duplicates: $skipped")
                }

                Log.d("MainActivity", message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error importing CSV", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Error importing CSV: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                viewModel.setLoading(false)
            }
        }
    }
    private fun previewCsvContent(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                Log.d("MainActivity", "CSV Content Preview:")
                reader.lineSequence().take(10).forEachIndexed { index, line ->
                    Log.d("MainActivity", "Line $index: $line")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error previewing CSV content", e)
        }
    }
}



